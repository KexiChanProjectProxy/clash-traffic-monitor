package com.zenkexi.clashmonitor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Events emitted by a streaming endpoint. */
sealed interface StreamEvent<out T> {
    data object Connecting : StreamEvent<Nothing>
    data class Data<T>(val value: T) : StreamEvent<T>
    data class Failed(val message: String) : StreamEvent<Nothing>
}

class HttpStatusException(val code: Int, message: String) : IOException(message)

class ClashApiClient(private val config: ControllerConfig) {

    private val http: OkHttpClient = shared.newBuilder().build()

    private fun request(path: String, ws: Boolean = false): Request {
        var url = config.baseUrl + path
        if (ws) url = url.replaceFirst("http", "ws")
        val b = Request.Builder().url(url)
        if (config.secret.isNotEmpty()) b.header("Authorization", "Bearer ${config.secret}")
        return b.build()
    }

    /** GET /version, returns a human readable version string. */
    suspend fun version(): String = withContext(Dispatchers.IO) {
        http.newCall(request("/version")).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpStatusException(resp.code, describeHttp(resp.code))
            ClashParser.parseVersion(resp.body?.string().orEmpty())
        }
    }

    /** Live /traffic stream with automatic reconnect. */
    fun traffic(): Flow<StreamEvent<TrafficSample>> =
        reconnecting { webSocketOnce("/traffic", ClashParser::parseTraffic) }

    /**
     * Live /connections stream with automatic reconnect. Uses WebSocket; if the core
     * rejects the upgrade it falls back to HTTP polling once per second.
     */
    fun connections(): Flow<StreamEvent<ConnectionsSnapshot>> = reconnecting {
        flow {
            var wsUnsupported = false
            emitAll(webSocketOnce("/connections", ClashParser::parseConnections) { code ->
                // 400/404/405 on upgrade means an old core without WS support here.
                if (code == 400 || code == 404 || code == 405) wsUnsupported = true
            })
            if (wsUnsupported) emitAll(pollConnections())
        }
    }

    private fun pollConnections(): Flow<StreamEvent<ConnectionsSnapshot>> = flow {
        while (coroutineContext.isActive) {
            val snap = withContext(Dispatchers.IO) {
                http.newCall(request("/connections")).execute().use { resp ->
                    if (!resp.isSuccessful) throw HttpStatusException(resp.code, describeHttp(resp.code))
                    ClashParser.parseConnections(resp.body?.string().orEmpty())
                }
            }
            emit(StreamEvent.Data(snap))
            delay(1000)
        }
    }

    /**
     * Opens one WebSocket and emits its messages until it closes. Completes normally on
     * close, throws on failure. [onUpgradeRejected] gets the HTTP code if the upgrade failed.
     */
    private fun <T> webSocketOnce(
        path: String,
        parse: (String) -> T,
        onUpgradeRejected: (Int) -> Unit = {},
    ): Flow<StreamEvent<T>> = callbackFlow {
        val listener = object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching { parse(text) }.onSuccess { trySendBlocking(StreamEvent.Data(it)) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                close(IOException("Closed by server ($code)"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close(IOException("Closed by server ($code)"))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val code = response?.code
                if (code != null && code != 101) {
                    onUpgradeRejected(code)
                    if (code == 400 || code == 404 || code == 405) {
                        close() // complete normally so caller can fall back
                        return
                    }
                    close(HttpStatusException(code, describeHttp(code)))
                } else {
                    close(t)
                }
            }
        }
        val ws = http.newWebSocket(request(path, ws = true), listener)
        awaitClose { ws.cancel() }
    }

    /** Wraps a single-session flow with Connecting/Failed events and exponential backoff. */
    private fun <T> reconnecting(session: () -> Flow<StreamEvent<T>>): Flow<StreamEvent<T>> = flow {
        var backoffMs = 1000L
        while (coroutineContext.isActive) {
            emit(StreamEvent.Connecting)
            var gotData = false
            val error: Throwable? = try {
                session().collect {
                    if (it is StreamEvent.Data) {
                        if (!gotData) backoffMs = 1000L
                        gotData = true
                    }
                    emit(it)
                }
                null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                e
            }
            emit(StreamEvent.Failed(describe(error)))
            delay(backoffMs)
            backoffMs = (backoffMs * 2).coerceAtMost(15_000L)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private val shared = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        fun describeHttp(code: Int): String = when (code) {
            401, 403 -> "Unauthorized ($code), check secret"
            404 -> "Not found (404), is this a Clash controller?"
            else -> "HTTP $code"
        }

        fun describe(t: Throwable?): String = when (t) {
            null -> "Disconnected"
            is HttpStatusException -> t.message ?: "HTTP ${t.code}"
            is java.net.UnknownHostException -> "Unknown host"
            is java.net.ConnectException -> "Connection refused"
            is java.net.SocketTimeoutException -> "Timed out"
            else -> t.message ?: t.javaClass.simpleName
        }
    }
}
