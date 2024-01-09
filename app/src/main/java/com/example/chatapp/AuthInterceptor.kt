package com.example.chatapp

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val accessToken: String?, private val refreshToken: String?) :
    Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        accessToken?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        return chain.proceed(request.build())
    }
}