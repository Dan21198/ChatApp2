package com.example.chatapp.model

data class ChatRoom(
    val id: Long,
    val exchange: String,
    val chatName: String,
    val owner: User,
    val userQueues: List<UserQueue>
)