package com.example.chatapp.api

data class RegistrationRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String

)