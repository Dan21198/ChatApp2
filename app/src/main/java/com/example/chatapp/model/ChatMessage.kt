package com.example.chatapp.model

data class ChatMessage(
    val type: String,
    val chatId: Long,
    val senderId: Long,
    val content: String
)

