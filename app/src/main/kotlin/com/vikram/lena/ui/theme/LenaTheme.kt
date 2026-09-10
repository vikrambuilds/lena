package com.vikram.lena.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors
val LenaPrimary = Color(0xFF7C4DFF)
val LenaSecondary = Color(0xFF00E5FF)
val LenaTertiary = Color(0xFFFF6B6B)
val LenaBackground = Color(0xFF0F0F23)
val LenaSurface = Color(0xFF1A1A2E)
val LenaCardBg = Color(0xFF2A2A3E)
val LenaOnSurface = Color.White
val LenaSuccess = Color(0xFF4CAF50)
val LenaError = Color(0xFFF44336)
val LenaWarning = Color(0xFFFF9800)

private val DarkColorScheme = darkColorScheme(
    primary = LenaPrimary,
    onPrimary = Color.White,
    secondary = LenaSecondary,
    onSecondary = Color.Black,
    tertiary = LenaTertiary,
    background = LenaBackground,
    onBackground = LenaOnSurface,
    surface = LenaSurface,
    onSurface = LenaOnSurface,
    error = LenaError,
    onError = Color.White,
    surfaceVariant = LenaCardBg,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f)
)

@Composable
fun LenaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}