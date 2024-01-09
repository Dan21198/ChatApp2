package com.example.chatapp.manager

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val ACCESS_TOKEN_KEY = "accessToken"
    private const val REFRESH_TOKEN_KEY = "refreshToken"
    private lateinit var sharedPreferences: SharedPreferences

    fun initSharedPreferences(context: Context) {
        sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        val editor = sharedPreferences.edit()
        editor.putString(ACCESS_TOKEN_KEY, accessToken)
        editor.putString(REFRESH_TOKEN_KEY, refreshToken)
        editor.apply()
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(ACCESS_TOKEN_KEY, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(REFRESH_TOKEN_KEY, null)
    }
}
