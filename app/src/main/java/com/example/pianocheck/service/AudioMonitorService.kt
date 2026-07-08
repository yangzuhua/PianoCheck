package com.example.pianocheck.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.pianocheck.data.AppSettings
import com.example.pianocheck.data.PracticeSession
import com.example.pianocheck.data.SessionDatabase
import com.example.pianocheck.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * 前台服务：实时采集麦克风音量，判断是否在弹琴。
 * - 连续 silenceSeconds 秒听不到钢琴声（音量低于阈值）→ TTS 播报提醒文案
 * - 倒计时结束 或 手动结束：若全程无提醒 → TTS 播报鼓励文案，并记录为「弹得好」
 *   若有提醒 → 记录为「有提醒·没认真弹」
 */
class AudioMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tts: TtsHelper? = null
    private var repo: SessionRepository? = null
    private lateinit var notifManager: NotificationManager

    // 本次会话参数
    private var practiceMs: Long = 0
    private var silenceMs: Long = 0
    private var threshold: Float = 0.02f
    private var remindText = ""
    private var encourageText = ""

    // 运行态
    private var startTime = 0L
    private var lastSoundTime = 0L
    private var silenceStart: Long? = null
    private var lastReminderAt = 0L
    private var lastTtsAt = 0L
    private var remindedCount = 0
    private var playedMs = 0L

    override fun onCreate() {
        super.onCreate()
        notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        tts = TtsHelper(this)
        repo = SessionRepository(SessionDatabase.get(this).sessionDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val settings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_SETTINGS, AppSettings::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_SETTINGS) as? AppSettings
                }
                startSession(settings ?: AppSettings())
            }
            ACTION_STOP -> stopSession()
        }
        return START_NOT_STICKY
    }

    private fun startSession(settings: AppSettings) {
        practiceMs = settings.practiceMinutes * 60_000L
        silenceMs = settings.silenceSeconds * 1000L
        threshold = settings.sensitivity
        remindText = settings.remindText
        encourageText = settings.encourageText

        startTime = System.currentTimeMillis()
        lastSoundTime = startTime
        lastReminderAt = 0L
        lastTtsAt = 0L
        remindedCount = 0
        playedMs = 0L
        silenceStart = null

        val notif = buildNotification("监督中…")
        ServiceCompat.startForeground(
            this, NOTIF_ID, notif,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        )

        PracticeSessionManager.reset()
        PracticeSessionManager.update {
            it.copy(isRunning = true, remainingMs = practiceMs, statusText = "监听中")
        }

        // 开始时播一句开场提醒
        tts?.speak("练琴时间到啦，开始弹琴吧！")
        lastTtsAt = System.currentTimeMillis()

        serviceScope.launch { monitorLoop() }
        serviceScope.launch { timerLoop() }
    }

    /** 麦克风音量检测主循环 */
    private suspend fun monitorLoop() {
        val sampleRate = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            finishWithError("无法初始化麦克风")
            return
        }
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            finishWithError("麦克风被其他应用占用")
            return
        }
        record.startRecording()
        val buffer = ShortArray(minBuf)
        var prevNow = System.currentTimeMillis()
        try {
            while (PracticeSessionManager.state.value.isRunning) {
                val now = System.currentTimeMillis()
                val dt = now - prevNow
                prevNow = now

                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) {
                    delay(50)
                    continue
                }

                // 计算 RMS 音量（归一化到 0..1）
                var sumSq = 0.0
                for (i in 0 until read) {
                    val v = buffer[i].toDouble()
                    sumSq += v * v
                }
                val rms = sqrt(sumSq / read)
                val level = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

                // 语音播报期间（约 1.2s）忽略声音，避免 TTS 外放被麦克风拾取造成误判
                if (now - lastTtsAt < 1200L) {
                    PracticeSessionManager.update { it.copy(level = level) }
                    delay(120)
                    continue
                }

                if (level >= threshold) {
                    // 检测到钢琴声
                    playedMs += dt
                    lastSoundTime = now
                    silenceStart = null
                } else {
                    // 静音累积
                    if (silenceStart == null) silenceStart = now
                    val silentFor = now - (silenceStart ?: now)
                    val sinceLastRemind = if (lastReminderAt == 0L) Long.MAX_VALUE else now - lastReminderAt
                    if (silentFor >= silenceMs && sinceLastRemind >= silenceMs) {
                        remindedCount++
                        lastReminderAt = now
                        lastTtsAt = now
                        tts?.speak(remindText)
                        PracticeSessionManager.update { st ->
                            st.copy(
                                remindedCount = remindedCount,
                                reminding = true,
                                statusText = "提醒：没认真弹"
                            )
                        }
                        // 2.5 秒后恢复「监听中」状态文字
                        serviceScope.launch {
                            delay(2500)
                            PracticeSessionManager.update { st ->
                                st.copy(reminding = false, statusText = "监听中")
                            }
                        }
                    }
                }

                PracticeSessionManager.update { it.copy(level = level) }
                delay(120)
            }
        } finally {
            record.stop()
            record.release()
        }
    }

    /** 倒计时循环 */
    private suspend fun timerLoop() {
        while (PracticeSessionManager.state.value.isRunning) {
            val remaining = practiceMs - (System.currentTimeMillis() - startTime)
            if (remaining <= 0) {
                PracticeSessionManager.update { it.copy(remainingMs = 0) }
                stopSession()
                return
            }
            PracticeSessionManager.update { it.copy(remainingMs = remaining) }
            val secs = remaining / 1000
            notifManager.notify(
                NOTIF_ID,
                buildNotification("剩余 %02d:%02d · 已提醒 %d 次".format(secs / 60, secs % 60, remindedCount))
            )
            delay(1000)
        }
    }

    /** 结束会话并写记录 */
    private fun stopSession() {
        if (!PracticeSessionManager.state.value.isRunning) return

        val endTime = System.currentTimeMillis()
        val encouragement = remindedCount == 0
        PracticeSessionManager.update { it.copy(isRunning = false) }

        if (encouragement) {
            tts?.speak(encourageText)
            lastTtsAt = endTime
        }

        val session = PracticeSession(
            startTime = startTime,
            endTime = endTime,
            plannedMinutes = (practiceMs / 60000).toInt(),
            playedSeconds = (playedMs / 1000).toInt(),
            remindedCount = remindedCount,
            status = if (encouragement) "GOOD" else "REMINDED",
            encouragementShown = encouragement
        )

        serviceScope.launch {
            repo?.insert(session)
            delay(1800) // 等鼓励语音播完
            tts?.shutdown()
            ServiceCompat.stopForeground(this@AudioMonitorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        PracticeSessionManager.update {
            it.copy(statusText = if (encouragement) "完成 · 弹得好" else "完成 · 有提醒")
        }
    }

    /** 异常结束（如麦克风不可用） */
    private fun finishWithError(msg: String) {
        if (!PracticeSessionManager.state.value.isRunning) return
        PracticeSessionManager.update { it.copy(isRunning = false, statusText = msg) }
        serviceScope.launch {
            tts?.shutdown()
            ServiceCompat.stopForeground(this@AudioMonitorService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "piano_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.notification_channel_desc) }
            notifManager.createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("钢琴打卡 · 监督中")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.pianocheck.START"
        const val ACTION_STOP = "com.example.pianocheck.STOP"
        const val EXTRA_SETTINGS = "settings"
        const val NOTIF_ID = 1001

        fun startIntent(ctx: Context, settings: AppSettings) = Intent(ctx, AudioMonitorService::class.java).apply {
            action = ACTION_START
            putExtra(EXTRA_SETTINGS, settings)
        }

        fun stopIntent(ctx: Context) = Intent(ctx, AudioMonitorService::class.java).apply {
            action = ACTION_STOP
        }
    }
}
