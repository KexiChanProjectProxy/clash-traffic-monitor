package com.zenkexi.clashmonitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zenkexi.clashmonitor.data.ClashApiClient
import com.zenkexi.clashmonitor.data.ControllerConfig
import com.zenkexi.clashmonitor.data.Settings
import com.zenkexi.clashmonitor.data.StreamEvent
import com.zenkexi.clashmonitor.domain.NewConnectionRateTracker
import com.zenkexi.clashmonitor.domain.appendCapped
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LinkStatus {
    data object NotConfigured : LinkStatus
    data object Connecting : LinkStatus
    data object Connected : LinkStatus
    data class Error(val message: String) : LinkStatus
}

data class DashboardState(
    val controller: String = "",
    val status: LinkStatus = LinkStatus.NotConfigured,
    val connections: Int? = null,
    val newPerSec: Double? = null,
    val up: Long? = null,
    val down: Long? = null,
    val uploadTotal: Long? = null,
    val downloadTotal: Long? = null,
    val connHistory: List<Float> = emptyList(),
    val rateHistory: List<Float> = emptyList(),
    val upHistory: List<Float> = emptyList(),
    val downHistory: List<Float> = emptyList(),
)

class MonitorViewModel(app: Application) : AndroidViewModel(app) {

    val settings = Settings(app)

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var trafficStatus: StreamEvent<*> = StreamEvent.Connecting
    private var connStatus: StreamEvent<*> = StreamEvent.Connecting
    private val rateTracker = NewConnectionRateTracker()

    /**
     * Streams data while the caller is active (call from repeatOnLifecycle(STARTED)).
     * Restarts automatically when the controller settings change.
     */
    suspend fun run() {
        settings.state.map { it.controller }.distinctUntilChanged().collectLatest { cfg ->
            _state.value = DashboardState(
                controller = cfg.displayName,
                status = if (cfg.isConfigured) LinkStatus.Connecting else LinkStatus.NotConfigured,
            )
            if (!cfg.isConfigured) return@collectLatest
            stream(cfg)
        }
    }

    private suspend fun stream(cfg: ControllerConfig) = coroutineScope {
        val client = ClashApiClient(cfg)
        trafficStatus = StreamEvent.Connecting
        connStatus = StreamEvent.Connecting
        rateTracker.reset()

        launch {
            client.traffic().collect { ev ->
                trafficStatus = ev
                _state.update { s ->
                    val base = s.copy(status = combinedStatus())
                    if (ev is StreamEvent.Data) {
                        base.copy(
                            up = ev.value.up,
                            down = ev.value.down,
                            upHistory = s.upHistory.appendCapped(ev.value.up.toFloat()),
                            downHistory = s.downHistory.appendCapped(ev.value.down.toFloat()),
                        )
                    } else base
                }
            }
        }
        launch {
            client.connections().collect { ev ->
                connStatus = ev
                if (ev !is StreamEvent.Data) rateTracker.reset()
                _state.update { s ->
                    val base = s.copy(status = combinedStatus())
                    if (ev is StreamEvent.Data) {
                        val snap = ev.value
                        val rate = rateTracker.update(snap.ids, System.currentTimeMillis())
                        base.copy(
                            connections = snap.ids.size,
                            newPerSec = rate ?: s.newPerSec,
                            uploadTotal = snap.uploadTotal,
                            downloadTotal = snap.downloadTotal,
                            connHistory = s.connHistory.appendCapped(snap.ids.size.toFloat()),
                            rateHistory = if (rate != null) s.rateHistory.appendCapped(rate.toFloat())
                            else s.rateHistory,
                        )
                    } else base
                }
            }
        }
    }

    private fun combinedStatus(): LinkStatus {
        val t = trafficStatus
        val c = connStatus
        return when {
            t is StreamEvent.Failed -> LinkStatus.Error(t.message)
            c is StreamEvent.Failed -> LinkStatus.Error(c.message)
            t is StreamEvent.Data<*> && c is StreamEvent.Data<*> -> LinkStatus.Connected
            else -> LinkStatus.Connecting
        }
    }
}
