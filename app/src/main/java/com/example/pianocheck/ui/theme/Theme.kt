package com.example.pianocheck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFF6A1B9A)
)

@Composable
fun PianoCheckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        // 简单深色适配
        lightColorScheme(
            primary = Color(0xFF90CAF9),
            secondary = Color(0xFF80CBC4),
            tertiary = Color(0xFFCE93D8)
        )
    } else {
        LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
