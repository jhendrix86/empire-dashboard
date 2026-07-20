package com.empire.dashboard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmpireGold    = Color(0xFFFFC107)
val EmpireGreen   = Color(0xFF00E676)
val EmpireRed     = Color(0xFFFF5252)
val EmpireSurface = Color(0xFF1A1A2E)
val EmpireCard    = Color(0xFF16213E)
val EmpireAccent  = Color(0xFF0F3460)

private val EmpireColorScheme = darkColorScheme(
    primary         = EmpireGold,
    secondary       = EmpireGreen,
    background      = EmpireSurface,
    surface         = EmpireCard,
    onPrimary       = Color.Black,
    onSecondary     = Color.Black,
    onBackground    = Color.White,
    onSurface       = Color.White,
    error           = EmpireRed
)

@Composable
fun EmpireTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmpireColorScheme,
        content = content
    )
}
