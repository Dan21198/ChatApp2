package com.example.chatapp.listener

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebsocketListener(): WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        webSocket.send("andorid device connected")
    }

}