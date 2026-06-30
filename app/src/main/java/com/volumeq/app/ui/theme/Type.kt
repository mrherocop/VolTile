package com.volumeq.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.volumeq.app.R

// Space Grotesk — offline font for main app
val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
    Font(R.font.space_grotesk_bold, FontWeight.Black),
    Font(R.font.space_grotesk_bold, FontWeight.ExtraBold),
)

// JetBrains Mono — offline font for notification panel mono labels
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)
