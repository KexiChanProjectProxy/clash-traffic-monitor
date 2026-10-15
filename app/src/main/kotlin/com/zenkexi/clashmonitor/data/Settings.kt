package com.zenkexi.clashmonitor.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ControllerConfig(
    val host: String = "",
    val port: Int = 9090,
    val secret: String = "",
    val useHttps: Boolean = false,
) {
    val isConfigured: Boolean get() = host.isNotBlank()

    /** Base HTTP URL, e.g. http://192.168.1.1:9090 */
    val baseUrl: String
        get() {
            val h = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
            // Bracket bare IPv6 literals.
            val hostPart = if (h.contains(':') && !h.startsWith("[")) "[$h]" else h
            return "${if (useHttps) "https" else "http"}://$hostPart:$port"
        }

    val displayName: String get() = "${host.trim()}:$port"
}

data class AppSettings(
    val controller: ControllerConfig = ControllerConfig(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keepScreenOn: Boolean = true,
)

class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        controller = ControllerConfig(
            host = prefs.getString(K_HOST, "") ?: "",
            port = prefs.getInt(K_PORT, 9090),
            secret = prefs.getString(K_SECRET, "") ?: "",
            useHttps = prefs.getBoolean(K_HTTPS, false),
        ),
        themeMode = runCatching { ThemeMode.valueOf(prefs.getString(K_THEME, null) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM),
        keepScreenOn = prefs.getBoolean(K_KEEP_ON, true),
    )

    fun update(newSettings: AppSettings) {
        prefs.edit()
            .putString(K_HOST, newSettings.controller.host.trim())
            .putInt(K_PORT, newSettings.controller.port)
            .putString(K_SECRET, newSettings.controller.secret)
            .putBoolean(K_HTTPS, newSettings.controller.useHttps)
            .putString(K_THEME, newSettings.themeMode.name)
            .putBoolean(K_KEEP_ON, newSettings.keepScreenOn)
            .apply()
        _state.value = newSettings
    }

    private companion object {
        const val K_HOST = "host"
        const val K_PORT = "port"
        const val K_SECRET = "secret"
        const val K_HTTPS = "https"
        const val K_THEME = "theme"
        const val K_KEEP_ON = "keep_screen_on"
    }
}
