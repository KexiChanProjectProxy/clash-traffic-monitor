package com.zenkexi.clashmonitor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

val AppTypography = Typography()

/** Live values change every second, so tabular figures keep them from jittering. */
val LiveNumberStyle = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontFeatureSettings = "tnum",
)
