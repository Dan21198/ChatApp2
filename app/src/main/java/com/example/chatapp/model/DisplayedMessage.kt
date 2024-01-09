package com.example.chatapp.model

data class DisplayedMessage(
    val sender: User,
    val type: String,
    val content: String
)