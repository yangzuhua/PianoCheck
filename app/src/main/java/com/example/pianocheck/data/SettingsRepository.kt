package com.example.pianocheck.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "piano_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PRACTICE_MIN = intPreferencesKey("practice_min")
        val SILENCE_SEC = intPreferencesKey("silence_sec")
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val REMIND_TEXT = stringPreferencesKey("remind_text")
        val ENCOURAGE_TEXT = stringPreferencesKey("encourage_text")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            practiceMinutes = prefs[Keys.PRACTICE_MIN] ?: 30,
            silenceSeconds = prefs[Keys.SILENCE_SEC] ?: 30,
            sensitivity = prefs[Keys.SENSITIVITY] ?: 0.02f,
            remindText = prefs[Keys.REMIND_TEXT] ?: "宝贝，继续弹琴哦，不要停~",
            encourageText = prefs[Keys.ENCOURAGE_TEXT] ?: "今天弹得真棒，继续保持！"
        )
    }

    suspend fun save(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRACTICE_MIN] = settings.practiceMinutes
            prefs[Keys.SILENCE_SEC] = settings.silenceSeconds
            prefs[Keys.SENSITIVITY] = settings.sensitivity
            prefs[Keys.REMIND_TEXT] = settings.remindText
            prefs[Keys.ENCOURAGE_TEXT] = settings.encourageText
        }
    }
}
