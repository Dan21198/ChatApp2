package com.example.chatapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.chatapp.api.ApiService
import com.example.chatapp.api.AuthResponse
import com.example.chatapp.api.RegistrationRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SignUp : AppCompatActivity() {
    private lateinit var edtFirstName: EditText
    private lateinit var edtLastName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        edtFirstName = findViewById(R.id.edt_firstName)
        edtLastName = findViewById(R.id.edt_lastName)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        btnSignUp = findViewById(R.id.btnSignUp)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)

        btnSignUp.setOnClickListener {
            performRegistration()
        }
    }

    private fun performRegistration() {
        Log.d("SignUpActivity", "Starting registration request...")
        val firstName = edtFirstName.text.toString()
        val lastName = edtLastName.text.toString()
        val email = edtEmail.text.toString()
        val password = edtPassword.text.toString()

        val registrationRequest = RegistrationRequest(
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = password
        )

        apiService.registerUser(registrationRequest).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Log.d("SignUpActivity", "Registration successful")

                    val intent = Intent(this@SignUp, LogIn::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Log.d("SignUpActivity", "Registration failed: ${response.message()}")

                    Toast.makeText(this@SignUp, "Sign Up failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Log.e("SignUpActivity", "Registration failed: ${t.message}")

                Toast.makeText(this@SignUp, "Sign Up failed: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }



    }
        )}
}
