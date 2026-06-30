package com.volumeq.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.volumeq.app.R

val SpaceGroteskFamily = FontFamily(
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
    Font(R.font.space_grotesk_bold, FontWeight.Black),
    Font(R.font.space_grotesk_bold, FontWeight.ExtraBold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
