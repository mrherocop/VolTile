package com.volumeq.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Fixed dark color scheme – no dynamic theming that can override user's dark UI
private val AppDarkColorScheme = darkColorScheme(
    primary = Color(0xFF7B8FFF),
    onPrimary = Color.White,
    secondary = Color(0xFF4CAF50),
    onSecondary = Color.White,
    background = Color(0xFF0D0D1A),
    onBackground = Color.White,
    surface = Color(0xFF141428),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E3A),
    onSurfaceVariant = Color(0xFFCCCCEE),
    outline = Color(0xFF444466),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun VolumeQTheme(
    content: @Composable () -> Unit
) {
    // Force dark mode always – this is a volume control utility, not a theme-switching app.
    val colorScheme = AppDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) break
                context = context.baseContext
            }
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = Color(0xFF0D0D1A).toArgb()
                window.navigationBarColor = Color(0xFF0D0D1A).toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
