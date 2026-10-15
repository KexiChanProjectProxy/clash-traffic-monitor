package com.zenkexi.clashmonitor

import com.zenkexi.clashmonitor.data.ClashParser
import com.zenkexi.clashmonitor.util.formatBytes
import com.zenkexi.clashmonitor.util.formatCount
import com.zenkexi.clashmonitor.util.formatRate
import com.zenkexi.clashmonitor.util.formatSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun speeds() {
        assertEquals("0 B/s", formatSpeed(0).toString())
        assertEquals("999 B/s", formatSpeed(999).toString())
        assertEquals("0.98 KB/s", formatSpeed(1000).toString())
        assertEquals("1.50 KB/s", formatSpeed(1536).toString())
        assertEquals("12.0 MB/s", formatSpeed(12L * 1024 * 1024).toString())
        assertEquals("512 MB/s", formatSpeed(512L * 1024 * 1024).toString())
    }

    @Test
    fun bytesAndCounts() {
        assertEquals("1.00 GB", formatBytes(1024L * 1024 * 1024))
        assertEquals("1,234,567", formatCount(1234567))
        assertEquals("0", formatRate(0.0))
        assertEquals("3.5", formatRate(3.5))
        assertEquals("1,234", formatRate(1234.4))
    }

    @Test
    fun parsesClashPayloads() {
        val t = ClashParser.parseTraffic("""{"up":123,"down":456}""")
        assertEquals(123L, t.up)
        assertEquals(456L, t.down)

        val c = ClashParser.parseConnections(
            """{"downloadTotal":10,"uploadTotal":20,"connections":[{"id":"a"},{"id":"b"}]}"""
        )
        assertEquals(setOf("a", "b"), c.ids)
        assertEquals(10L, c.downloadTotal)
        assertEquals(20L, c.uploadTotal)

        // mihomo sends null instead of [] when there are no connections
        assertEquals(0, ClashParser.parseConnections("""{"connections":null}""").ids.size)

        assertEquals("v1.18.0 (Meta)", ClashParser.parseVersion("""{"meta":true,"version":"v1.18.0"}"""))
    }
}
