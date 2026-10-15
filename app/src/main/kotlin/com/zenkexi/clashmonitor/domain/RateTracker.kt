package com.zenkexi.clashmonitor.domain

/**
 * Derives a "new connections per second" rate from successive /connections snapshots
 * by diffing connection IDs. Connections that open and close between two snapshots
 * are not seen, so this is a lower bound.
 */
class NewConnectionRateTracker {
    private var prevIds: Set<String>? = null
    private var prevTimeMs: Long = 0

    /** Returns the rate, or null for the first snapshot after a reset. */
    fun update(ids: Set<String>, nowMs: Long): Double? {
        val prev = prevIds
        val prevTime = prevTimeMs
        prevIds = ids
        prevTimeMs = nowMs
        if (prev == null) return null
        val newCount = ids.count { it !in prev }
        // Clash pushes about once a second; clamp so a burst of two quick frames can't spike.
        val dtSec = ((nowMs - prevTime) / 1000.0).coerceAtLeast(0.5)
        return newCount / dtSec
    }

    fun reset() {
        prevIds = null
        prevTimeMs = 0
    }
}

/** Appends [value] and keeps only the last [capacity] items. */
fun List<Float>.appendCapped(value: Float, capacity: Int = HISTORY_SIZE): List<Float> {
    val start = (size + 1 - capacity).coerceAtLeast(0)
    val out = ArrayList<Float>(minOf(size + 1, capacity))
    for (i in start until size) out += this[i]
    out += value
    return out
}

const val HISTORY_SIZE = 60
