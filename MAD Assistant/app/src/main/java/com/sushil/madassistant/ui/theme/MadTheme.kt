package com.sushil.madassistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MadBg = Color(0xFF050A14)
val MadSurface = Color(0xFF0A1628)
val MadCard = Color(0xFF0D1B2E)
val MadCyan = Color(0xFF00E5FF)
val MadGold = Color(0xFFFFD54F)
val MadRed = Color(0xFFFF5252)
val MadGreen = Color(0xFF69F0AE)
val MadTextPrimary = Color(0xFFE1F5FE)
val MadTextSecondary = Color(0xFF90A4AE)

private val MadColorScheme = darkColorScheme(
    primary = MadCyan,
    onPrimary = MadBg,
    secondary = MadGold,
    onSecondary = MadBg,
    background = MadBg,
    onBackground = MadTextPrimary,
    surface = MadSurface,
    onSurface = MadTextPrimary,
    surfaceVariant = MadCard,
    onSurfaceVariant = MadTextSecondary,
    error = MadRed
)

@Composable
fun MadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MadColorScheme,
        content = content
    )
}
