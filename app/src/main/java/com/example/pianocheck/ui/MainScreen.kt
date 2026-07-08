package com.example.pianocheck.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.pianocheck.data.SessionRepository
import com.example.pianocheck.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(settingsRepo: SettingsRepository, sessionRepo: SessionRepository) {
    var tab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("⚙️") },
                    label = { Text("设置") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("\uD83C\uDFB9") },
                    label = { Text("监督") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Text("\uD83D\uDCC5") },
                    label = { Text("记录") }
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> SettingsScreen(settingsRepo) { tab = 1 }
                1 -> MonitorScreen(settingsRepo)
                2 -> RecordsScreen(sessionRepo)
            }
        }
    }
}
