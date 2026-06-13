package com.orbit.mobile.core.network

import com.orbit.mobile.core.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

// Socket event
data class SocketEvent(
    val type: String,
    val payload: JsonObject
)

// Socket state
enum class SocketState { CONNECTING, CONNECTED, DISCONNECTED }

// Project socket
class ChatSocket internal constructor(
    private val client: OkHttpClient,
    private val provider: BaseUrlProvider,
    private val session: SessionManager,
    private val projectId: String
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var socket: WebSocket? = null
    private var wantOpen = false
    private var attempt = 0
    private var reconnectJob: Job? = null

    private val _events = MutableSharedFlow<SocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events

    private val _state = MutableStateFlow(SocketState.DISCONNECTED)
    val state: StateFlow<SocketState> = _state

    // Open socket
    fun open() {
        if (wantOpen) return
        wantOpen = true
        connect()
    }

    // Close socket
    fun close() {
        wantOpen = false
        reconnectJob?.cancel()
        socket?.close(1000, null)
        socket = null
        _state.value = SocketState.DISCONNECTED
        scope.cancel()
    }

    // Connect once
    private fun connect() {
        val token = session.token ?: return
        _state.value = SocketState.CONNECTING
        val url = "${provider.wsBaseUrl}/ws/$projectId?token=$token"
        val request = Request.Builder().url(url).build()
        socket = client.newWebSocket(request, listener)
    }

    // Retry later
    private fun scheduleReconnect() {
        if (!wantOpen) return
        val backoff = when (attempt) {
            0 -> 1_000L
            1 -> 2_000L
            2 -> 5_000L
            else -> 10_000L
        }
        attempt++
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(backoff)
            if (wantOpen) connect()
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            attempt = 0
            _state.value = SocketState.CONNECTED
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching {
                val obj = json.parseToJsonElement(text).jsonObject
                SocketEvent(
                    type = obj["type"]?.jsonPrimitive?.content ?: "unknown",
                    payload = obj
                )
            }.getOrNull() ?: return
            _events.tryEmit(event)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _state.value = SocketState.DISCONNECTED
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _state.value = SocketState.DISCONNECTED
            scheduleReconnect()
        }
    }
}

// Socket factory
@Singleton
class ChatSocketFactory @Inject constructor(
    private val client: OkHttpClient,
    private val provider: BaseUrlProvider,
    private val session: SessionManager
) {
    // New socket
    fun create(projectId: String): ChatSocket =
        ChatSocket(client, provider, session, projectId)
}
