package com.zenkexi.clashmonitor.util

import java.util.Locale

/** A number split into its value text and unit, so the UI can size them differently. */
data class ValueWithUnit(val value: String, val unit: String) {
    override fun toString() = if (unit.isEmpty()) value else "$value $unit"
}

private val SIZE_UNITS = arrayOf("B", "KB", "MB", "GB", "TB")

private fun scale(bytes: Double): Pair<Double, Int> {
    var v = bytes.coerceAtLeast(0.0)
    var i = 0
    while (v >= 1000 && i < SIZE_UNITS.lastIndex) {
        v /= 1024
        i++
    }
    return v to i
}

private fun smart(v: Double): String = when {
    v == 0.0 -> "0"
    v < 10 -> String.format(Locale.ROOT, "%.2f", v)
    v < 100 -> String.format(Locale.ROOT, "%.1f", v)
    else -> String.format(Locale.ROOT, "%.0f", v)
}

/** 1536 -> ("1.50", "KB/s") */
fun formatSpeed(bytesPerSec: Long): ValueWithUnit {
    val (v, i) = scale(bytesPerSec.toDouble())
    return ValueWithUnit(if (i == 0) v.toLong().toString() else smart(v), SIZE_UNITS[i] + "/s")
}

/** 1536 -> "1.50 KB" */
fun formatBytes(bytes: Long): String {
    val (v, i) = scale(bytes.toDouble())
    return (if (i == 0) v.toLong().toString() else smart(v)) + " " + SIZE_UNITS[i]
}

/** 1234567 -> "1,234,567" */
fun formatCount(n: Int): String = String.format(Locale.ROOT, "%,d", n)

/** Rate with one decimal below 100, integer above. */
fun formatRate(r: Double): String = when {
    r <= 0.0 -> "0"
    r < 100 -> String.format(Locale.ROOT, "%.1f", r)
    else -> String.format(Locale.ROOT, "%,.0f", r)
}
