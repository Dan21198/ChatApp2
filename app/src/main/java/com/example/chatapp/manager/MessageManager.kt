package com.example.chatapp.manager

import com.example.chatapp.api.ApiService

class MessageManager(private val apiService: ApiService) {

    private val messageSender = MessageSender(apiService)

    fun sendMessage(
        chatRoomId: Long,
        senderId: Long,
        content: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        messageSender.sendMessage(chatRoomId, senderId, content, onSuccess, onFailure)
    }
}