package com.example.pianocheck.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.pianocheck.data.AppSettings
import com.example.pianocheck.data.SettingsRepository
import com.example.pianocheck.service.AudioMonitorService
import com.example.pianocheck.service.PracticeSessionManager
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(repo: SettingsRepository) {
    val context = LocalContext.current
    val state by PracticeSessionManager.state.collectAsState()
    val settings by repo.settingsFlow.collectAsState(initial = AppSettings())

    var hasAudioPerm by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        hasAudioPerm = res[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else
            arrayOf(Manifest.permission.RECORD_AUDIO)
        hasAudioPerm = needed.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasAudioPerm) permLauncher.launch(needed)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("钢琴监督", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(28.dp))

        // 倒计时
        val mm = TimeUnit.MILLISECONDS.toMinutes(state.remainingMs)
        val ss = TimeUnit.MILLISECONDS.toSeconds(state.remainingMs) % 60
        Text(
            "%02d:%02d".format(mm, ss),
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.height(10.dp))

        Text(
            state.statusText,
            style = MaterialTheme.typography.titleMedium,
            color = if (state.reminding) Color.Red else MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text("本次已提醒：${state.remindedCount} 次", style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(24.dp))

        // 实时音量条
        LinearProgressIndicator(
            progress = state.level.coerceIn(0f, 1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.height(4.dp))
        Text("实时声音：%.0f%%".format(state.level * 100), style = MaterialTheme.typography.bodySmall)

        Spacer(Modifier.height(36.dp))

        if (!state.isRunning) {
            Button(
                onClick = {
                    if (!hasAudioPerm) {
                        permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        return@Button
                    }
                    val intent = AudioMonitorService.startIntent(context, settings)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("开始监督")
            }
        } else {
            Button(
                onClick = { context.startService(AudioMonitorService.stopIntent(context)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("结束")
            }
        }

        if (!hasAudioPerm) {
            Spacer(Modifier.height(8.dp))
            Text(
                "需要麦克风权限才能监听弹琴声音，请点击上方按钮授权。",
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
