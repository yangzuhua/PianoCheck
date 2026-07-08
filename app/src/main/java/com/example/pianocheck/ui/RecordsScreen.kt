package com.example.pianocheck.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pianocheck.data.PracticeSession
import com.example.pianocheck.data.SessionRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecordsScreen(repo: SessionRepository) {
    val sessions by repo.allSessions.collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("MM-dd HH:mm", Locale.CHINESE) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("打卡记录", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { scope.launch { repo.clear() } }) {
                Text("清空")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Text(
                "还没有记录，去「监督」开始第一次练习吧~",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions) { s ->
                    SessionCard(s, fmt) { scope.launch { repo.delete(s) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(s: PracticeSession, fmt: SimpleDateFormat, onDelete: () -> Unit) {
    val (label, color) = when (s.status) {
        "GOOD" -> "弹得好 👍" to Color(0xFF2E7D32)
        "REMINDED" -> "有提醒 · 没认真弹" to Color(0xFFC62828)
        else -> "已结束" to Color.Gray
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fmt.format(Date(s.startTime)), style = MaterialTheme.typography.titleMedium)
                Text(label, color = color, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "计划 ${s.plannedMinutes} 分钟 · 实际弹奏 ${s.playedSeconds} 秒 · 提醒 ${s.remindedCount} 次",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDelete) {
                Text("删除这条", color = Color.Gray)
            }
        }
    }
}
