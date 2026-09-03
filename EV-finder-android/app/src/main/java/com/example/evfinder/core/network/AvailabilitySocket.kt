package com.example.evfinder.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/**
 * App-wide WebSocket connection for live availability updates.
 * Emits an [AvailabilityEvent] every time the backend broadcasts a change
 * (booking created / cancelled / payment failed). Screens collect the flow
 * and refresh their data.
 */
object AvailabilitySocket {

    data class AvailabilityEvent(val serviceId: String, val stationId: String)

    private val _events = MutableSharedFlow<AvailabilityEvent>(replay = 0, extraBufferCapacity = 8)
    val events: SharedFlow<AvailabilityEvent> = _events

    @Volatile
    var connected: Boolean = false
        private set

    private var webSocket: WebSocket? = null

    fun connect() {
        if (webSocket != null) return
        val client = OkHttpClient()
        val request = Request.Builder().url(ApiClient.WS_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                connected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    if (json.optString("type") == "availability_changed") {
                        _events.tryEmit(
                            AvailabilityEvent(
                                json.getString("serviceId"),
                                json.getString("stationId")
                            )
                        )
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                connected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connected = false
            }
        })
    }

    fun close() {
        webSocket?.close(1000, "app exit")
        webSocket = null
        connected = false
    }
}
