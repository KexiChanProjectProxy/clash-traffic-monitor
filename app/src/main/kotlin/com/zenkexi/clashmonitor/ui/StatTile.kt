package com.zenkexi.clashmonitor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenkexi.clashmonitor.domain.HISTORY_SIZE
import com.zenkexi.clashmonitor.ui.theme.LiveNumberStyle

/**
 * One metric: label with identity dot, a large live value with its unit,
 * a secondary line, and a 60 s sparkline along the bottom.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String,
    detail: String,
    history: List<Float>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            // Size the hero number from whichever dimension is tighter.
            val valueSize = with(density) {
                minOf(maxHeight.toPx() * 0.28f, maxWidth.toPx() * 0.15f).toSp()
            }
            val labelSize = (valueSize.value * 0.28f).coerceIn(12f, 20f).sp

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(accent, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        label,
                        fontSize = labelSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Text(
                        value,
                        style = LiveNumberStyle,
                        fontSize = valueSize,
                        lineHeight = valueSize,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            unit,
                            fontSize = labelSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = with(density) { (valueSize.toPx() * 0.12f).toDp() }),
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    detail,
                    fontSize = (labelSize.value * 0.8f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Sparkline(
                    values = history,
                    color = accent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

/** Area sparkline: 2 px line plus a soft gradient fill, scaled to the window max. */
@Composable
fun Sparkline(values: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val maxV = values.max().coerceAtLeast(1f)
        val stepX = size.width / (HISTORY_SIZE - 1)
        // Right-align so new samples enter from the right edge.
        val startX = size.width - stepX * (values.size - 1)
        val strokePx = 2.dp.toPx()
        val usableH = size.height - strokePx
        fun y(v: Float) = strokePx / 2 + usableH * (1f - v / maxV)

        val line = Path()
        values.forEachIndexed { i, v ->
            val x = startX + i * stepX
            if (i == 0) line.moveTo(x, y(v)) else line.lineTo(x, y(v))
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(startX, size.height)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.02f)),
                startY = 0f,
                endY = size.height,
            ),
        )
        drawPath(line, color, style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = values.last()
        drawCircle(color, radius = 3.5.dp.toPx(), center = Offset(size.width, y(last)))
    }
}
