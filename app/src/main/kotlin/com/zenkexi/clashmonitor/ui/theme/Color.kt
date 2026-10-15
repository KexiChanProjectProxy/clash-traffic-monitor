package com.zenkexi.clashmonitor.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Fallback schemes for Android 10/11 (no dynamic color). */
val LightScheme = lightColorScheme(
    primary = Color(0xFF2A78D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE2FB),
    onPrimaryContainer = Color(0xFF0D366B),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF0B0B0B),
    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFF52514E),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F1F5),
    outlineVariant = Color(0xFFDADCE2),
    error = Color(0xFFC62828),
)

val DarkScheme = darkColorScheme(
    primary = Color(0xFF86B6EF),
    onPrimary = Color(0xFF0D366B),
    primaryContainer = Color(0xFF184F95),
    onPrimaryContainer = Color(0xFFCDE2FB),
    background = Color(0xFF0F1218),
    onBackground = Color(0xFFF0EFEC),
    surface = Color(0xFF0F1218),
    onSurface = Color(0xFFF0EFEC),
    onSurfaceVariant = Color(0xFFC3C2B7),
    surfaceContainer = Color(0xFF1A1D24),
    surfaceContainerHigh = Color(0xFF232731),
    outlineVariant = Color(0xFF343844),
    error = Color(0xFFEF9A9A),
)

/** Fixed identity colors per metric, stepped separately for light and dark surfaces. */
@Immutable
data class MetricColors(
    val download: Color,
    val upload: Color,
    val connections: Color,
    val newConnections: Color,
    val good: Color,
    val warning: Color,
    val critical: Color,
)

val LightMetricColors = MetricColors(
    download = Color(0xFF2A78D6),
    upload = Color(0xFFEB6834),
    connections = Color(0xFF1BAF7A),
    newConnections = Color(0xFF6250D6),
    good = Color(0xFF2E7D32),
    warning = Color(0xFFB26A00),
    critical = Color(0xFFC62828),
)

val DarkMetricColors = MetricColors(
    download = Color(0xFF3987E5),
    upload = Color(0xFFD95926),
    connections = Color(0xFF199E70),
    newConnections = Color(0xFF9085E9),
    good = Color(0xFF66BB6A),
    warning = Color(0xFFFFB74D),
    critical = Color(0xFFEF9A9A),
)

val LocalMetricColors = staticCompositionLocalOf { LightMetricColors }
