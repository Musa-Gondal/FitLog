package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Lime = Color(0xFFC6F432)
val LimeDark = Color(0xFF4F6B00)
val ProteinColor = Color(0xFF4FC3F7)
val CarbColor = Color(0xFFFFB74D)
val FatColor = Color(0xFFF06292)
val WaterColor = Color(0xFF29B6F6)

private val Dark = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF1A2400),
    primaryContainer = Color(0xFF2E3D00),
    onPrimaryContainer = Color(0xFFDDF98A),
    secondary = Color(0xFF9CD3C0),
    onSecondary = Color(0xFF02382B),
    secondaryContainer = Color(0xFF1F4E41),
    onSecondaryContainer = Color(0xFFB8F0DC),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF2A302C),
    onSurfaceVariant = Color(0xFFC2C8C1),
    surfaceContainer = Color(0xFF1B201D),
    surfaceContainerHigh = Color(0xFF232926),
    surfaceContainerHighest = Color(0xFF2D3330),
    surfaceContainerLow = Color(0xFF171C19),
    outline = Color(0xFF8C928B),
    error = Color(0xFFFFB4AB),
)

private val Light = lightColorScheme(
    primary = LimeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF98A),
    onPrimaryContainer = Color(0xFF161F00),
    secondary = Color(0xFF346A5A),
    secondaryContainer = Color(0xFFB8F0DC),
    onSecondaryContainer = Color(0xFF002019),
    background = Color(0xFFF7F9F3),
    surface = Color(0xFFF7F9F3),
    surfaceContainer = Color(0xFFECEFE8),
    surfaceContainerHigh = Color(0xFFE6E9E2),
    surfaceContainerHighest = Color(0xFFE0E3DC),
    surfaceContainerLow = Color(0xFFF1F4ED),
)

@Composable
fun FitLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
