package com.example.chatapp.manager

import android.util.Log
import com.example.chatapp.model.ChatDTO
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.QueueChatMessage
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketManager(private val okHttpClient: OkHttpClient) {

    private val queueListeners: MutableMap<String, (ChatMessage) -> Unit> = mutableMapOf()

    fun connectToWebSocket(token: String, data: List<ChatDTO>) {
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/ws-message/websocket")
            .addHeader("Authorization", "Bearer $token")
            .build()

        okHttpClient.newWebSocket(request, WebSocketListenerImpl(data))
        Log.d("WebSocketManager", "Connecting to WebSocket...")
    }
    private inner class WebSocketListenerImpl(private val data: List<ChatDTO>) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            super.onOpen(webSocket, response)
            Log.d("WebSocketManager", "WebSocket connection opened")
            Log.d("WebSocketManager", "$response")
            subscribeToMessages(webSocket, data)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.d("WebSocketManager", "Received message: $text")

            val lines = text.split("\n")
            val command = lines[0]
            val headers = mutableMapOf<String, String>()
            var body = ""

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isEmpty()) {
                    body = lines.drop(i + 1).joinToString("\n")
                    break
                } else {
                    val colonIndex = line.indexOf(':')
                    if (colonIndex != -1) {
                        val key = line.substring(0, colonIndex)
                        val value = line.substring(colonIndex + 1)
                        headers += key to value
                    }
                }
            }

            when (command) {
                "MESSAGE" -> {
                    val contentType = headers["content-type"]
                    if (contentType == "application/json;charset=UTF-8") {
                        try {
                            val chatMessage = Gson().fromJson(body, ChatMessage::class.java)
                            headers["destination"]?.let { handleQueueMessage(it, chatMessage) }
                        } catch (e: JsonSyntaxException) {
                            Log.e("WebSocketManager", "Failed to parse chat message: $body")
                        }
                    }
                }

            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
            Log.d("WebSocketManager", "Received binary message: ${bytes.hex()}")

            val message = bytes.utf8()
            Log.d("WebSocketManager", "Binary message as string: $message")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            super.onFailure(webSocket, t, response)
            Log.e("WebSocketManager", "WebSocket failure: ${t.message}")

            response?.let {
                Log.e("WebSocketManager", "Response code: ${it.code}")
                Log.e("WebSocketManager", "Response message: ${it.message}")
            }
        }

        private fun subscribeToMessages(webSocket: WebSocket, data: List<ChatDTO>) {
            Log.d("WebSocketManager", "Initiating subscriptions...")

            data.forEach { chatDto ->
                val chatRoom = chatDto.chat
                chatDto.queues.forEach { queue ->
                    val subscriptionMessage = "SUBSCRIBE\nid:${chatRoom.id}\ndestination:" +
                            "/chat/queue/$queue\nack:client\n\n\u0000"


                    webSocket.send(subscriptionMessage)
                    Log.d("WebSocketManager", "Sent subscription: $subscriptionMessage")

                    addListenerForQueue(queue) { chatMessage ->
                        handleQueueMessage(queue, chatMessage)
                    }

                    val hasListener = hasListenerForQueue(queue)
                    if (hasListener) {
                        Log.d("WebSocketManager", "Listener added for queue: $queue")
                    } else {
                        Log.d("WebSocketManager", "No listener found for queue: $queue")
                    }
                }
            }
        }

        private fun addListenerForQueue(queue: String, listener: (ChatMessage) -> Unit) {
            queueListeners[queue] = listener
        }


        fun hasListenerForQueue(queue: String): Boolean {
            return queueListeners.containsKey(queue)
        }

        private fun handleQueueMessage(queue: String, chatMessage: ChatMessage) {
            Log.d("WebSocketManager", "Received message from queue $queue: $chatMessage")
        }

        private fun handleSubscribedMessage(queueChatMessage: QueueChatMessage) {
            queueListeners[queueChatMessage.queue]?.invoke(queueChatMessage.chatMessage)
        }
    }
}