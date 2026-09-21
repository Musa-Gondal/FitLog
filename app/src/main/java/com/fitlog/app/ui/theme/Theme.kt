package com.fitlog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Lime = Color(0xFFC6F432)
val LimeDark = Color(0xFF4F6B00)
val ProteinColor = Color(0xFF4FC3F7)
val CarbColor = Color(0xFFFFB74D)
val FatColor = Color(0xFFF06292)
val WaterColor = Color(0xFF29B6F6)
val WarnColor = Color(0xFFFF8A65)
val GoodColor = Color(0xFF81C784)
val InfoColor = Color(0xFF90CAF9)
val Bronze = Color(0xFFCD7F32)
val Silver = Color(0xFFB0BEC5)
val Gold = Color(0xFFFFC107)

private val Dark = darkColorScheme(
    primary = Lime,
    onPrimary = Color(0xFF1A2400),
    primaryContainer = Color(0xFF2E3D00),
    onPrimaryContainer = Color(0xFFDDF98A),
    secondary = Color(0xFF9CD3C0),
    onSecondary = Color(0xFF02382B),
    secondaryContainer = Color(0xFF1F4E41),
    onSecondaryContainer = Color(0xFFB8F0DC),
    tertiary = Color(0xFFFFB74D),
    background = Color(0xFF0E1210),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF0E1210),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF2A302C),
    onSurfaceVariant = Color(0xFFB9C0B8),
    surfaceContainer = Color(0xFF181D1A),
    surfaceContainerHigh = Color(0xFF212723),
    surfaceContainerHighest = Color(0xFF2B312D),
    surfaceContainerLow = Color(0xFF141816),
    outline = Color(0xFF8C928B),
    outlineVariant = Color(0xFF3A413C),
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
    tertiary = Color(0xFFB26A00),
    background = Color(0xFFF6F8F2),
    surface = Color(0xFFF6F8F2),
    surfaceContainer = Color(0xFFECEFE7),
    surfaceContainerHigh = Color(0xFFE5E9E0),
    surfaceContainerHighest = Color(0xFFDEE2D9),
    surfaceContainerLow = Color(0xFFF0F3EC),
    outlineVariant = Color(0xFFC6CBC2),
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun FitLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
