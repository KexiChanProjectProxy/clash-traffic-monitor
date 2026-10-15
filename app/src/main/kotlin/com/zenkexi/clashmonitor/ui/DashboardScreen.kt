package com.zenkexi.clashmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenkexi.clashmonitor.DashboardState
import com.zenkexi.clashmonitor.LinkStatus
import com.zenkexi.clashmonitor.ui.theme.LocalMetricColors
import com.zenkexi.clashmonitor.util.formatBytes
import com.zenkexi.clashmonitor.util.formatCount
import com.zenkexi.clashmonitor.util.formatRate
import com.zenkexi.clashmonitor.util.formatSpeed

private const val DASH = "–"

@Composable
fun DashboardScreen(state: DashboardState, onOpenSettings: () -> Unit) {
    val colors = LocalMetricColors.current
    val gap = 12.dp
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TopBar(state, onOpenSettings)
        Spacer(Modifier.height(8.dp))

        val down = state.down?.let(::formatSpeed)
        val up = state.up?.let(::formatSpeed)
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            StatTile(
                label = "Download",
                value = down?.value ?: DASH,
                unit = down?.unit ?: "",
                detail = "Peak 60s  " + peakSpeed(state.downHistory),
                history = state.downHistory,
                accent = colors.download,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                label = "Upload",
                value = up?.value ?: DASH,
                unit = up?.unit ?: "",
                detail = "Peak 60s  " + peakSpeed(state.upHistory),
                history = state.upHistory,
                accent = colors.upload,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        Spacer(Modifier.height(gap))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            StatTile(
                label = "Connections",
                value = state.connections?.let(::formatCount) ?: DASH,
                unit = "",
                detail = "Peak 60s  " + (state.connHistory.maxOrNull()?.toInt()?.let(::formatCount) ?: DASH),
                history = state.connHistory,
                accent = colors.connections,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                label = "New connections",
                value = state.newPerSec?.let(::formatRate) ?: DASH,
                unit = if (state.newPerSec != null) "/s" else "",
                detail = "Avg 60s  " + (state.rateHistory.takeIf { it.isNotEmpty() }
                    ?.average()?.let { formatRate(it) + "/s" } ?: DASH),
                history = state.rateHistory,
                accent = colors.newConnections,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

private fun peakSpeed(h: List<Float>): String =
    h.maxOrNull()?.let { formatSpeed(it.toLong()).toString() } ?: DASH

@Composable
private fun TopBar(state: DashboardState, onOpenSettings: () -> Unit) {
    val colors = LocalMetricColors.current
    val (dotColor, statusText) = when (val s = state.status) {
        LinkStatus.NotConfigured -> MaterialTheme.colorScheme.outlineVariant to "Not configured"
        LinkStatus.Connecting -> colors.warning to "Connecting"
        LinkStatus.Connected -> colors.good to "Live"
        is LinkStatus.Error -> colors.critical to s.message
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                statusText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                state.controller,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (state.downloadTotal != null && state.uploadTotal != null) {
            Text(
                "Total  ↓ ${formatBytes(state.downloadTotal)}   ↑ ${formatBytes(state.uploadTotal)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

