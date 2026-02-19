package com.example.comprafaciladmin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF2D3B87)
val AppBackground = Color(0xFFF5F7FB)
val CardWhite = Color(0xFFFFFFFF)
val AccentYellow = Color(0xFFFDCB58)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    background = AppBackground,
    surface = CardWhite,
)

@Composable
fun CompraFacilAdminTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
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
