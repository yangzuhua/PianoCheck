package com.example.pianocheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.pianocheck.data.SessionDatabase
import com.example.pianocheck.data.SessionRepository
import com.example.pianocheck.data.SettingsRepository
import com.example.pianocheck.ui.MainScreen
import com.example.pianocheck.ui.theme.PianoCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepo = SettingsRepository(this)
        val sessionRepo = SessionRepository(SessionDatabase.get(this).sessionDao())

        setContent {
            PianoCheckTheme {
                MainScreen(settingsRepo = settingsRepo, sessionRepo = sessionRepo)
            }
        }
    }
}
