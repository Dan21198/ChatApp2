package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.example.chatapp.api.ApiService
import com.example.chatapp.api.AuthRequest
import com.example.chatapp.api.AuthResponse
import com.example.chatapp.manager.TokenManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LogIn : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogIn: Button
    private lateinit var btnSignUp: Button
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnLogIn = findViewById(R.id.btnLogIn)
        btnSignUp = findViewById(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            navigateToSignUp()
        }
        btnLogIn.setOnClickListener {
            performLogin()
        }

        TokenManager.initSharedPreferences(applicationContext)

        val accessToken = TokenManager.getAccessToken()
        val refreshToken = TokenManager.getRefreshToken()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080")
            .client(createOkHttpClient(accessToken, refreshToken))
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun navigateToSignUp() {
        val intent = Intent(this, SignUp::class.java)
        startActivity(intent)
    }

    private fun saveTokens(accessToken: String, refreshToken: String) {
        TokenManager.saveTokens(accessToken, refreshToken)
    }

    private fun performLogin() {
        val email = edtEmail.text.toString()
        val password = edtPassword.text.toString()

        val authRequest = AuthRequest(
            email = email,
            password = password
        )

        apiService.loginUser(authRequest).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    val authResponse = response.body()

                    authResponse?.let {
                        val accessToken = it.accessToken
                        val refreshToken = it.refreshToken

                        if (!accessToken.isNullOrBlank() && refreshToken != null) {
                            saveTokens(accessToken, refreshToken)
                            navigateToChatActivity()
                        } else {
                            Log.e("LoginActivity", "Access Token or Refresh Token is null or blank")
                        }

                        Log.d("LoginActivity", "Response body content: ${response.body()}")
                    }
                } else {
                    Log.e("LoginActivity", "Login failed: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("LoginActivity", "Login failed: ${t.message}")
            }
        })
    }

    private fun navigateToChatActivity() {
        val intent = Intent(this@LogIn, ChatActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun createOkHttpClient(accessToken: String?, refreshToken: String?): OkHttpClient {
        val httpClient = OkHttpClient.Builder()
        httpClient.addInterceptor(Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .header("Refresh-Token", refreshToken ?: "")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        })
        return httpClient.build()
    }
}
