package com.zenkexi.clashmonitor.data

import org.json.JSONObject

/** One /traffic sample, bytes per second. */
data class TrafficSample(val up: Long, val down: Long)

/** One /connections snapshot. */
data class ConnectionsSnapshot(
    val ids: Set<String>,
    val uploadTotal: Long,
    val downloadTotal: Long,
)

object ClashParser {
    fun parseTraffic(json: String): TrafficSample {
        val o = JSONObject(json)
        return TrafficSample(up = o.optLong("up", 0), down = o.optLong("down", 0))
    }

    fun parseConnections(json: String): ConnectionsSnapshot {
        val o = JSONObject(json)
        val arr = o.optJSONArray("connections")
        val ids = HashSet<String>(arr?.length() ?: 0)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val id = c.optString("id", "")
                if (id.isNotEmpty()) ids += id
            }
        }
        return ConnectionsSnapshot(
            ids = ids,
            uploadTotal = o.optLong("uploadTotal", 0),
            downloadTotal = o.optLong("downloadTotal", 0),
        )
    }

    fun parseVersion(json: String): String {
        val o = JSONObject(json)
        val v = o.optString("version", "unknown")
        val meta = o.optBoolean("meta", false)
        val premium = o.optBoolean("premium", false)
        return buildString {
            append(v)
            if (meta) append(" (Meta)") else if (premium) append(" (Premium)")
        }
    }
}
