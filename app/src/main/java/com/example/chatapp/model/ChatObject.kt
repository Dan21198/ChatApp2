package com.example.chatapp.model

data class ChatObject(
    val chat: ChatRoom,
    val queues: List<String>,
    val messages: List<DisplayedMessage>,
    val seen: Boolean
)