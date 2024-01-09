package com.example.chatapp.manager

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Callback
import java.util.concurrent.TimeUnit

class WebSocketManager(private val okHttpClient: OkHttpClient) {
    private var webSocket: WebSocket? = null
    private val chatMessagesMap: MutableMap<String, MutableList<String>> = mutableMapOf()
    private var messageHandler: ((String) -> Unit)? = null

    fun connectWebSocket(
        webSocketUrl: String,
        token: String,
        queues: List<String>,
        messageHandler: (String) -> Unit
    ) {
        val request = Request.Builder()
            .url(webSocketUrl)
            .addHeader("Authorization", "Bearer $token")
            .build()

        val webSocketListener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                super.onOpen(webSocket, response)
                Log.d("WebSocketManager", "WebSocket opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketManager", "Received message: $text")
                messageHandler(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                super.onFailure(webSocket, t, response)
                Log.e("WebSocketManager", "WebSocket failure: ${t.message}")
            }
        }

        webSocket = okHttpClient.newWebSocket(request, webSocketListener)

        webSocket?.let { client ->
            queues.forEach { queue ->
                client.send("SUBSCRIBE /chat/$queue")
                Log.d("WebSocketManager", "Subscribed to /chat/$queue")
            }
        }
    }

    fun closeWebSocket() {
        webSocket?.cancel()
    }
}




