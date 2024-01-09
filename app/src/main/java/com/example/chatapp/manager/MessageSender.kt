package com.example.chatapp.manager

import com.example.chatapp.api.ApiService
import com.example.chatapp.model.ChatMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessageSender(private val apiService: ApiService) {

    fun sendMessage(
        chatRoomId: Long,
        senderId: Long,
        content: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val message = ChatMessage(type = "text", chatId = chatRoomId, senderId = senderId, content = content)

        apiService.sendMessage(message).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    onSuccess.invoke()
                } else {
                    onFailure.invoke()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                onFailure.invoke()
            }
        })
    }
}
