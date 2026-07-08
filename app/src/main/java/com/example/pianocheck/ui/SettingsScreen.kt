package com.example.pianocheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.pianocheck.data.AppSettings
import com.example.pianocheck.data.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repo: SettingsRepository, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val settings by repo.settingsFlow.collectAsState(initial = AppSettings())

    var practiceMin by remember { mutableIntStateOf(settings.practiceMinutes) }
    var silenceSec by remember { mutableIntStateOf(settings.silenceSeconds) }
    var sensitivity by remember { mutableFloatStateOf(settings.sensitivity) }
    var remindText by remember { mutableStateOf(settings.remindText) }
    var encourageText by remember { mutableStateOf(settings.encourageText) }
    var justSaved by remember { mutableStateOf(false) }

    // 设置加载完成后同步到本地编辑态
    LaunchedEffect(settings) {
        practiceMin = settings.practiceMinutes
        silenceSec = settings.silenceSeconds
        sensitivity = settings.sensitivity
        remindText = settings.remindText
        encourageText = settings.encourageText
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("练习设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = practiceMin.toString(),
            onValueChange = { practiceMin = it.toIntOrNull() ?: 30 },
            label = { Text("练习总时长（分钟）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = silenceSec.toString(),
            onValueChange = { silenceSec = it.toIntOrNull() ?: 30 },
            label = { Text("静音多少秒后提醒（秒）") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        Text("麦克风灵敏度（越小越灵敏）：%.3f".format(sensitivity))
        Slider(
            value = sensitivity,
            onValueChange = { sensitivity = it },
            valueRange = 0.005f..0.1f,
            steps = 18,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = remindText,
            onValueChange = { remindText = it },
            label = { Text("静音提醒文案（自定义）") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = encourageText,
            onValueChange = { encourageText = it },
            label = { Text("完成鼓励文案（自定义）") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                scope.launch {
                    repo.save(
                        AppSettings(
                            practiceMinutes = practiceMin.coerceAtLeast(1),
                            silenceSeconds = silenceSec.coerceAtLeast(1),
                            sensitivity = sensitivity,
                            remindText = remindText.ifBlank { "宝贝，继续弹琴哦，不要停~" },
                            encourageText = encourageText.ifBlank { "今天弹得真棒，继续保持！" }
                        )
                    )
                    justSaved = true
                    onSaved()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存设置")
        }
        if (justSaved) {
            Spacer(Modifier.height(8.dp))
            Text("已保存 ✓", color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "使用提示：环境越安静，灵敏度可设越小；若把电视声、说话声也误判成弹琴，就把灵敏度调大一些。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
