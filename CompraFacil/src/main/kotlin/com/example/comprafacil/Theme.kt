package com.example.comprafacil

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF2D3B87)
val AppBackground = Color(0xFFF5F7FB)
val CardWhite = Color(0xFFFFFFFF)
val AccentYellow = Color(0xFFFDCB58)
val TextDark = Color(0xFF1A1C1E)
val TextGrey = Color(0xFF6B7280)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = PrimaryBlue,
    secondary = AccentYellow,
    onSecondary = TextDark,
    background = AppBackground,
    surface = CardWhite,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun CompraFacilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Keeping it simple for now, sticking mostly to light theme as per image
    val colors = if (darkTheme) {
        // Simple dark mode variation
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}
