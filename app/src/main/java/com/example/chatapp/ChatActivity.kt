package com.example.chatapp


import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.chatapp.Adapter.ChatRoomAdapter
import com.example.chatapp.api.ApiService
import com.example.chatapp.manager.ExchangeManager
import com.example.chatapp.manager.MessageManager
import com.example.chatapp.manager.RetrofitManager
import com.example.chatapp.manager.TokenManager
import com.example.chatapp.manager.WebSocketManager
import com.example.chatapp.model.ChatDTO
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.ChatRoom
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ua.naiksoftware.stomp.StompClient


class ChatActivity : AppCompatActivity(){

    private lateinit var textOutput: TextView
    private lateinit var textInput: EditText
    private lateinit var btnSend: Button
    private lateinit var editTextNewRoom: EditText
    private lateinit var btnCreateRoom: Button
    private lateinit var listViewChatRooms: ListView
    private lateinit var apiService: ApiService
    private var fetchedChatDTOs: List<ChatDTO>? = null
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val listOfChatDTOs: MutableList<ChatDTO> = mutableListOf()
    private var selectedChatRoomId: Long = -1L
    private lateinit var messageManager: MessageManager
    private lateinit var exchangeManager: ExchangeManager
    private val chatMessagesMap: MutableMap<Long, MutableList<String>> = mutableMapOf()
    private val okHttpClient = OkHttpClient()
    private lateinit var webSocket: WebSocket
    private lateinit var stompClient: StompClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val viewToDetectSwipe: View = findViewById(R.id.viewToDetectSwipe)

        drawerLayout.closeDrawer(GravityCompat.START)

        viewToDetectSwipe.setOnTouchListener(object : OnSwipeTouchListener(this@ChatActivity) {
            override fun onSwipeRight() {
                super.onSwipeRight()
                drawerLayout.openDrawer(GravityCompat.START)
            }
        })

        listViewChatRooms = findViewById(R.id.listChatRooms)
        btnSend = findViewById(R.id.btnSend)
        textInput = findViewById(R.id.textInput)
        editTextNewRoom = findViewById(R.id.editTextNewRoom)
        btnCreateRoom = findViewById(R.id.btnCreateRoom)
        chatRoomAdapter = ChatRoomAdapter(this, listOfChatDTOs)
        listViewChatRooms.adapter = chatRoomAdapter

        initializeRetrofit()
        initializeViews()

        TokenManager.getAccessToken()?.let { accessToken ->
            CoroutineScope(Dispatchers.IO).launch {
                initializeWebSocket(accessToken)
            }
        }

        messageManager = MessageManager(apiService)
        exchangeManager = ExchangeManager(apiService)

        fetchExchanges()
        setupCreateRoomClickListener()
        setupButtonClickListener()
        setupChatRoomClickListener()
    }


    private fun initializeWebSocket(token: String) {
        val webSocketManager = WebSocketManager(okHttpClient)
        try {
            val request = Request.Builder()
                .url("http://10.0.2.2:8080/api/chat/exchanges")
                .addHeader("Authorization", "Bearer $token")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val chats: List<ChatDTO> = parseResponseToChatDTOList(responseBody)
                    webSocketManager.connectToWebSocket(token, chats)
                } else {
                    val message = response.message
                    Log.e("WebSocketManager", "Failed to fetch chats: $message")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocketManager", "Error fetching queues: $e")
        }
    }

    private fun parseResponseToChatDTOList(responseBody: String): List<ChatDTO> {
        return Gson().fromJson(responseBody, object : TypeToken<List<ChatDTO>>() {}.type)
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.close(1000, "Activity destroyed")
    }

    private fun initializeRetrofit() {
        TokenManager.initSharedPreferences(this)
        val accessToken = TokenManager.getAccessToken()
        apiService = RetrofitManager.createRetrofitClient(accessToken)
    }

    private fun setupCreateRoomClickListener() {
        btnCreateRoom.setOnClickListener {
            val roomName = editTextNewRoom.text.toString().trim()
            if (roomName.isNotEmpty()) {
                createChatRoom(roomName)
            } else {

            }
        }
    }

    private fun createChatRoom(roomName: String) {
        apiService.createChat(roomName).enqueue(object : Callback<ChatRoom> {
            override fun onResponse(call: Call<ChatRoom>, response: Response<ChatRoom>) {
                if (response.isSuccessful) {
                    val createdChatRoom: ChatRoom? = response.body()
                    if (createdChatRoom != null) {
                        if (createdChatRoom.chatName != null) {
                            Log.d(
                                "ChatRoomCreation",
                                "Chat room created: ${createdChatRoom.chatName}"
                            )
                        } else {
                            Log.e("ChatRoomCreation", "Chat name is null")
                        }
                    } else {
                        Log.e("ChatRoomCreation", "Empty response body")
                    }
                } else {
                    val errorMessage = "Failed to create chat room: ${response.code()}"
                    Log.e("ChatRoomCreation", errorMessage)
                }
            }

            override fun onFailure(call: Call<ChatRoom>, t: Throwable) {
                val errorMessage = "Network failure: ${t.message}"
                Log.e("ChatRoomCreation", errorMessage)
            }
        })
    }

    private fun fetchExchanges() {
        exchangeManager.fetchExchanges(
            onSuccess = { exchanges ->
                listOfChatDTOs.clear()
                listOfChatDTOs.addAll(exchanges)
                chatRoomAdapter.notifyDataSetChanged()
            },
            onFailure = { errorMessage ->
                Log.e("ChatActivity", errorMessage)
            }
        )
    }

    private fun setupChatRoomClickListener() {
        listViewChatRooms.setOnItemClickListener { _, _, position, _ ->
            val selectedItem = listViewChatRooms.adapter.getItem(position)
            val id = when (selectedItem) {
                is String -> findChatDtoByChatName(selectedItem)?.chat?.id ?: -1L
                is ChatDTO -> selectedItem.chat?.id ?: -1L
                else -> -1L
            }

            selectedChatRoomId = id
            displayMessagesForSelectedChatRoom(selectedChatRoomId)
        }
    }

    private fun findChatDtoByChatName(chatName: String): ChatDTO? {
        Log.d("ChatActivity", "Searching for chat with name: $chatName")
        val foundChat = fetchedChatDTOs?.firstOrNull { it.chat?.chatName == chatName }
        if (foundChat != null) {
            Log.d("ChatActivity", "Found chat: ${foundChat.chat?.chatName}")
        } else {
            Log.d("ChatActivity", "Chat not found")
        }
        return foundChat
    }

    private fun initializeViews() {
        textOutput = findViewById(R.id.textOutput)
        textInput =
            findViewById(R.id.textInput) ?: throw IllegalStateException("textInput not found")
        btnSend = findViewById(R.id.btnSend)
    }

    private fun getSenderIdForSelectedChat(selectedChatRoom: ChatDTO?): Long {
        return selectedChatRoom?.chat?.owner?.id ?: -1L
    }

    private fun displayMessageForSelectedChatRoom(message: String) {
        Log.d("ChatActivity", "Received message: $message")
        textOutput.append("$message\n")
    }

    private fun displayMessagesForSelectedChatRoom(chatRoomId: Long) {
        val messages = chatMessagesMap[chatRoomId]
        val formattedMessages = messages?.joinToString("\n")

        textOutput.text = formattedMessages ?: ""
    }

    private fun setupButtonClickListener() {
        btnSend.setOnClickListener {
            val selectedChatRoom = listOfChatDTOs.find { it.chat?.id == selectedChatRoomId }
            val senderId = getSenderIdForSelectedChat(selectedChatRoom)
            val content = textInput.text.toString().trim()

            if (selectedChatRoomId != -1L && senderId != -1L && content.isNotEmpty()) {
                sendMessage(selectedChatRoomId, senderId, content)
            } else {
                Toast.makeText(this, "Please select a chat room and enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(chatRoomId: Long, senderId: Long, content: String) {
        val message = ChatMessage(type = "text", chatId = chatRoomId, senderId = senderId, content = content)

        apiService.sendMessage(message).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    displayMessageForSelectedChatRoom(content)
                    textInput.text.clear()
                } else {
                    Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
