package com.example.pianocheck.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 简单的 TTS 封装，用于播放自定义提醒 / 鼓励语音。
 */
class TtsHelper(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // 优先使用中文；若设备未安装中文引擎会自动回退
                val result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.CHINA
                }
                ready = true
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        // QUEUE_FLUSH：打断上一条，立即播报（适合提醒场景）
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "piano_remind")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
