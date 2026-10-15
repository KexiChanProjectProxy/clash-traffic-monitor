package com.zenkexi.clashmonitor

import com.zenkexi.clashmonitor.domain.NewConnectionRateTracker
import com.zenkexi.clashmonitor.domain.appendCapped
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateTrackerTest {
    @Test
    fun firstSnapshotHasNoRate() {
        assertNull(NewConnectionRateTracker().update(setOf("a"), 1000))
    }

    @Test
    fun countsOnlyNewIds() {
        val t = NewConnectionRateTracker()
        t.update(setOf("a", "b", "c"), 1000)
        // b closed, d and e opened, 1 second later
        assertEquals(2.0, t.update(setOf("a", "c", "d", "e"), 2000)!!, 1e-9)
    }

    @Test
    fun dividesByElapsedTime() {
        val t = NewConnectionRateTracker()
        t.update(emptySet(), 0)
        assertEquals(2.0, t.update(setOf("1", "2", "3", "4"), 2000)!!, 1e-9)
    }

    @Test
    fun clampsTinyIntervals() {
        val t = NewConnectionRateTracker()
        t.update(emptySet(), 0)
        assertEquals(2.0, t.update(setOf("x"), 10)!!, 1e-9) // 1 / 0.5s
    }

    @Test
    fun resetForgetsPrevious() {
        val t = NewConnectionRateTracker()
        t.update(setOf("a"), 0)
        t.reset()
        assertNull(t.update(setOf("a", "b"), 1000))
    }

    @Test
    fun appendCappedKeepsLastN() {
        var l = emptyList<Float>()
        for (i in 1..5) l = l.appendCapped(i.toFloat(), 3)
        assertEquals(listOf(3f, 4f, 5f), l)
    }
}
