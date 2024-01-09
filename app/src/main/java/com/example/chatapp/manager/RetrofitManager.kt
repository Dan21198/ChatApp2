package com.example.chatapp.manager

import com.example.chatapp.api.ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitManager {
    private const val BASE_URL = "http://10.0.2.2:8080"

    fun createRetrofitClient(accessToken: String?): ApiService {
        val httpClient = OkHttpClient.Builder()
        accessToken?.let {
            httpClient.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .method(original.method, original.body)
                val request = requestBuilder.build()
                chain.proceed(request)
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}