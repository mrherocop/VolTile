package com.volumeq.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// SONIQ forces light mode always — no dynamic color, no dark mode
private val SoniqColorScheme = lightColorScheme(
    primary        = Black,
    onPrimary      = White,
    background     = Lime,
    onBackground   = Black,
    surface        = White,
    onSurface      = Black,
)

@Composable
fun VolumeQTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SoniqColorScheme,
        content = content
    )
}
