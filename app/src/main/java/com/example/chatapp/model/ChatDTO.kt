package com.example.chatapp.model

data class ChatDTO(
    val chat: ChatRoom,
    val queues: List<String>
)