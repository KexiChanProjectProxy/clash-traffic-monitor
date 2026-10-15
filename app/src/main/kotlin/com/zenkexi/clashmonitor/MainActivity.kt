package com.zenkexi.clashmonitor

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zenkexi.clashmonitor.ui.DashboardScreen
import com.zenkexi.clashmonitor.ui.SettingsScreen
import com.zenkexi.clashmonitor.ui.theme.ClashMonitorTheme
import com.zenkexi.clashmonitor.ui.theme.isDarkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: MonitorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        hideSystemBars()

        // Stream only while visible; sockets close when the app goes to background.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.run() }
        }

        setContent {
            val settings by vm.settings.state.collectAsStateWithLifecycle()
            val dashboard by vm.state.collectAsStateWithLifecycle()
            var showSettings by rememberSaveable { mutableStateOf(!settings.controller.isConfigured) }

            LaunchedEffect(settings.keepScreenOn) { setKeepScreenOn(settings.keepScreenOn) }

            ClashMonitorTheme(dark = isDarkTheme(settings.themeMode)) {
                if (showSettings) {
                    val canGoBack = settings.controller.isConfigured
                    BackHandler(enabled = canGoBack) { showSettings = false }
                    SettingsScreen(
                        initial = settings,
                        canGoBack = canGoBack,
                        onSave = { vm.settings.update(it); showSettings = false },
                        onBack = { showSettings = false },
                    )
                } else {
                    DashboardScreen(dashboard, onOpenSettings = { showSettings = true })
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun setKeepScreenOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /** Immersive mode: bars hidden, swipe from an edge shows them briefly. */
    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
