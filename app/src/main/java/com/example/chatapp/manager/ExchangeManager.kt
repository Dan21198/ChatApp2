package com.example.chatapp.manager

import com.example.chatapp.api.ApiService
import com.example.chatapp.model.ChatDTO
import retrofit2.Callback
import retrofit2.Call
import retrofit2.Response

class ExchangeManager(private val apiService: ApiService) {

    fun fetchExchanges(
        onSuccess: (List<ChatDTO>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        apiService.getExchanges().enqueue(object : Callback<List<ChatDTO>> {
            override fun onResponse(call: Call<List<ChatDTO>>, response: Response<List<ChatDTO>>) {
                if (response.isSuccessful) {
                    val exchanges = response.body()
                    exchanges?.let {
                        onSuccess(it)
                    } ?: onFailure("Empty response body")
                } else {
                    onFailure("Failed to fetch exchanges: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<ChatDTO>>, t: Throwable) {
                onFailure("Network failure: ${t.message}")
            }
        })
    }
}