package com.example.chatapp.api

import com.example.chatapp.model.ChatDTO
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.ChatRoom
import com.example.chatapp.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("api/auth/register")
    fun registerUser(@Body registrationRequest: RegistrationRequest): Call<AuthResponse>

    @POST("api/auth/authenticate")
    fun loginUser(@Body authRequest: AuthRequest): Call<AuthResponse>

    @POST("api/chat/send")
    fun sendMessage(@Body message: ChatMessage): Call<Void>

    @POST("api/chat/create")
    fun createChat(@Query("chatName") chatName: String): Call<ChatRoom>

    @GET("api/chat/queues")
    fun getQueues(): Call<List<String>>

    @GET("api/chat/exchanges")
    fun getExchanges(): Call<List<ChatDTO>>

    @GET("api/chat/exchanges")
    suspend fun getExchangesSuspend(): List<ChatDTO>

    @POST("api/chat/add")
    fun addUser(@Query("chatId") chatId: Long, @Query("email") email: String): Call<User>


}