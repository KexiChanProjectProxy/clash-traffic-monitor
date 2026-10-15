package com.zenkexi.clashmonitor

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.NightMode
import com.android.resources.ScreenOrientation
import com.zenkexi.clashmonitor.ui.DashboardScreen
import com.zenkexi.clashmonitor.ui.theme.ClashMonitorTheme
import org.junit.Rule
import org.junit.Test
import kotlin.math.sin

/** Renders the dashboard on the JVM so the layout can be checked without a device. */
class DashboardScreenshotTest {
    private val landscape = DeviceConfig.PIXEL_5.copy(
        orientation = ScreenOrientation.LANDSCAPE,
        screenWidth = DeviceConfig.PIXEL_5.screenHeight,
        screenHeight = DeviceConfig.PIXEL_5.screenWidth,
    )

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = landscape, theme = "android:Theme.Material.NoActionBar")

    private fun wave(n: Int, base: Float, amp: Float, period: Float) =
        List(n) { i -> (base + amp * sin(i / period) + amp * 0.3f * sin(i * 1.7f)).coerceAtLeast(0f) }

    private val sample = DashboardState(
        controller = "192.168.1.1:9090",
        status = LinkStatus.Connected,
        connections = 187,
        newPerSec = 6.4,
        up = 312_000,
        down = 14_800_000,
        uploadTotal = 1_340_000_000,
        downloadTotal = 28_600_000_000,
        connHistory = wave(60, 170f, 25f, 8f),
        rateHistory = wave(60, 6f, 4f, 5f),
        upHistory = wave(60, 250_000f, 150_000f, 6f),
        downHistory = wave(60, 9_000_000f, 6_000_000f, 9f),
    )

    @Test
    fun light() = paparazzi.snapshot { ClashMonitorTheme(dark = false) { DashboardScreen(sample) {} } }

    @Test
    fun dark() {
        paparazzi.unsafeUpdateConfig(landscape.copy(nightMode = NightMode.NIGHT))
        paparazzi.snapshot { ClashMonitorTheme(dark = true) { DashboardScreen(sample) {} } }
    }

    @Test
    fun errorState() = paparazzi.snapshot {
        ClashMonitorTheme(dark = false) {
            DashboardScreen(DashboardState(controller = "10.0.0.1:9090", status = LinkStatus.Error("Unauthorized (401), check secret"))) {}
        }
    }
}
