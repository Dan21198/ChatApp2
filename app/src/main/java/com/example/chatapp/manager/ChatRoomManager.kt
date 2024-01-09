package com.example.chatapp.manager

import com.example.chatapp.api.ApiService
import com.example.chatapp.model.ChatDTO
import com.example.chatapp.model.ChatRoom
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatRoomManager(private val apiService: ApiService) {

    fun fetchChatRooms(
        onSuccess: (List<ChatDTO>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        apiService.getExchanges().enqueue(object : Callback<List<ChatDTO>> {
            override fun onResponse(call: Call<List<ChatDTO>>, response: Response<List<ChatDTO>>) {
                if (response.isSuccessful) {
                    val chatRooms = response.body()
                    chatRooms?.let { onSuccess(it) }
                } else {
                    onFailure("Failed to fetch chat rooms: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<ChatDTO>>, t: Throwable) {
                onFailure("Network failure: ${t.message}")
            }
        })
    }

    fun createChatRoom(
        roomName: String,
        onSuccess: (ChatRoom) -> Unit,
        onFailure: (String) -> Unit
    ) {
        apiService.createChat(roomName).enqueue(object : Callback<ChatRoom> {
            override fun onResponse(call: Call<ChatRoom>, response: Response<ChatRoom>) {
                if (response.isSuccessful) {
                    val createdChatRoom = response.body()
                    createdChatRoom?.let { onSuccess(it) }
                } else {
                    onFailure("Failed to create chat room: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ChatRoom>, t: Throwable) {
                onFailure("Network failure: ${t.message}")
            }
        })
    }
}