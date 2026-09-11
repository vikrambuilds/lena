package com.vikram.lena.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Modern Vibrant Colors
val NeonPurple = Color(0xFFB388FF)
val ElectricBlue = Color(0xFF00E5FF)
val HotPink = Color(0xFFFF4081)
val NeonGreen = Color(0xFF00E676)
val SunsetOrange = Color(0xFFFF6E40)

// Dark theme backgrounds
val DeepDark = Color(0xFF0A0A1F)
val MidDark = Color(0xFF1A1A2E)
val CardDark = Color(0xFF16162A)
val GlassDark = Color(0xFF2A2A4A)

// Text colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF)
val TextTertiary = Color(0x80FFFFFF)

// Status colors
val SuccessGreen = Color(0xFF00C853)
val WarningYellow = Color(0xFFFFD600)
val ErrorRed = Color(0xFFFF5252)

// Gradient brushes
object Gradients {
    val PurpleBlue = Brush.linearGradient(
        colors = listOf(NeonPurple, ElectricBlue)
    )
    val PinkOrange = Brush.linearGradient(
        colors = listOf(HotPink, SunsetOrange)
    )
    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(DeepDark, MidDark, CardDark)
    )
    val OrbGradient = Brush.radialGradient(
        colors = listOf(
            NeonPurple.copy(alpha = 0.8f),
            ElectricBlue.copy(alpha = 0.4f),
            Color.Transparent
        )
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    onPrimary = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.Black,
    tertiary = HotPink,
    background = DeepDark,
    onBackground = TextPrimary,
    surface = MidDark,
    onSurface = TextPrimary,
    error = ErrorRed,
    onError = Color.White,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary
)

@Composable
fun LenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}