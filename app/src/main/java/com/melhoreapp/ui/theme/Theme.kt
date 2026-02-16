package com.melhoreapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6B9BD2),
    secondary = Color(0xFF8FA8B8),
    tertiary = Color(0xFF9E9E9E),
    surface = Color(0xFF121212),
    background = Color(0xFF000000),
    onSurface = Color(0xFFE0E0E0),
    onBackground = Color(0xFFE0E0E0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF0288D1),
    tertiary = Color(0xFF7B1FA2)
)

@Composable
fun MelhoreAppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
