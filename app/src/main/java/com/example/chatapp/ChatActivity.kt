package com.example.chatapp

import android.app.Dialog
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatapp.Adapter.ChatRoomAdapter
import com.example.chatapp.Adapter.MessageAdapter
import com.example.chatapp.api.ApiService
import com.example.chatapp.manager.ExchangeManager
import com.example.chatapp.manager.MessageManager
import com.example.chatapp.manager.RetrofitManager
import com.example.chatapp.manager.TokenManager
import com.example.chatapp.model.ChatDTO
import com.example.chatapp.model.ChatMessage
import com.example.chatapp.model.ChatRoom
import com.example.chatapp.model.User
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.frame.FrameBody
import org.hildan.krossbow.stomp.frame.StompFrame
import org.hildan.krossbow.stomp.headers.StompSubscribeHeaders
import org.hildan.krossbow.websocket.okhttp.OkHttpWebSocketClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Duration

class ChatActivity : AppCompatActivity() {
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
    private var chatMessagesMap: MutableMap<Long, MutableList<ChatMessage>> = mutableMapOf()
    private val userList: MutableList<User> = mutableListOf()
    private lateinit var stompClient: StompClient
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var messageAdapter: MessageAdapter

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
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        messageAdapter = MessageAdapter()
        recyclerViewMessages.layoutManager = LinearLayoutManager(this)
        recyclerViewMessages.adapter = messageAdapter


        initializeRetrofit()
        initializeViews()

        val okHttpClient = OkHttpClient.Builder()
            .callTimeout(Duration.ofMinutes(1))
            .pingInterval(Duration.ofSeconds(10))
            .build()

        val wsClient = OkHttpWebSocketClient(okHttpClient)
        stompClient = StompClient(wsClient)

        TokenManager.getAccessToken()?.let { accessToken ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val data: List<ChatDTO> = apiService.getExchangesSuspend()
                    Log.d("WebSocketManager", "api response: $data")

                    val webSocket = stompClient.connect("ws://10.0.2.2:8080/ws-message/websocket")

                    data.forEach { chatDto ->
                        chatDto.queues.forEach { queue ->
                            val destination = "/chat/queue/$queue"
                            Log.d("WebSocketManager", "Subscribing to queue: $destination")

                            val subscriptionHeaders = StompSubscribeHeaders(destination = destination)
                            val messageFlow = webSocket.subscribe(subscriptionHeaders)

                            launch {
                                messageFlow.collect { message ->
                                    Log.d("WebSocketManager", "Received message from $destination: $message")
                                    val chatMessage = extractChatMessageFromMessage(message)
                                    saveMessageToChatRoom(chatDto.chat?.id ?: -1L, chatMessage)

                                    displayMessageForSelectedChatRoomOnMainThread(chatMessage.content)
                                }
                                Log.d("WebSocketManager", "Subscription successful for queue: $destination")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketManager", "Failed to subscribe to queues: ${e.message}")
                }
            }
        }

        val fabAddUser: FloatingActionButton = findViewById(R.id.fabAddUser)

        fabAddUser.setOnClickListener {
            showAddUserDialog()
        }

        messageManager = MessageManager(apiService)
        exchangeManager = ExchangeManager(apiService)

        fetchExchanges()
        initializeUsers()
        setupCreateRoomClickListener()
        setupButtonClickListener()
        setupChatRoomClickListener()
    }

    private fun initializeUsers() {
        apiService.getExchanges().enqueue(object : Callback<List<ChatDTO>> {
            override fun onResponse(call: Call<List<ChatDTO>>, response: Response<List<ChatDTO>>) {
                if (response.isSuccessful) {
                    val chatDTOs: List<ChatDTO>? = response.body()
                    if (chatDTOs != null) {
                        fetchedChatDTOs = chatDTOs
                        chatDTOs.forEach { chatDto ->
                            chatDto.chat?.userQueues?.forEach { userQueue ->
                                val user = userQueue.user
                                if (!userList.contains(user)) {
                                    userList.add(user)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("ApiService", "Failed to fetch exchanges:" +
                            " ${response.code()}, ${response.errorBody()?.string()}")
                }
            }
            override fun onFailure(call: Call<List<ChatDTO>>, t: Throwable) {
                Log.e("ApiService", "Network error: ${t.message}")
            }
        })
    }
    private fun showAddUserDialog() {
        if (selectedChatRoomId != -1L) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_add_user)

            val editTextNewUser: EditText = dialog.findViewById(R.id.editTextNewUser)
            val btnAddUser: Button = dialog.findViewById(R.id.btnAddUser)

            btnAddUser.setOnClickListener {
                val email = editTextNewUser.text.toString().trim()
                if (email.isNotEmpty()) {
                    Log.d("WebSocketManager", "mail: $email")
                    Log.d("WebSocketManager", "chatroomId: $selectedChatRoomId")
                    apiService.addUser(selectedChatRoomId, email).enqueue(object : Callback<User> {
                        override fun onResponse(call: Call<User>, response: Response<User>) {
                            if (response.isSuccessful) {
                                val user: User? = response.body()
                                Log.d("ApiService", "User added successfully: $user")
                            } else {
                                Log.e("ApiService", "Failed to add user:" +
                                        " ${response.code()}, ${response.errorBody()?.string()}")
                            }
                        }

                        override fun onFailure(call: Call<User>, t: Throwable) {
                            Log.e("ApiService", "Network error: ${t.message}")
                        }
                    })
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.show()
        } else {
            Toast.makeText(this, "Please select a chat room first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveMessageToChatRoom(chatRoomId: Long, chatMessage: ChatMessage) {
        val messages = chatMessagesMap.getOrPut(chatRoomId) { mutableListOf() }
        messages.add(chatMessage)
    }

    private fun extractChatMessageFromMessage(message: StompFrame.Message): ChatMessage {
        return try {
            val jsonString = (message.body as? FrameBody.Text)?.text ?: ""
            Gson().fromJson(jsonString, ChatMessage::class.java)
        } catch (e: Exception) {
            ChatMessage("", -1L, -1L, "")
        }
    }
    private fun displayMessageForSelectedChatRoomOnMainThread(content: String) {
        runOnUiThread {
            messageAdapter.submitList(chatMessagesMap[selectedChatRoomId])
            recyclerViewMessages.scrollToPosition(messageAdapter.itemCount - 1)
        }
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

            clearTextOutputAndDisplayMessages(selectedChatRoomId)
        }
    }

    private fun clearTextOutputAndDisplayMessages(chatRoomId: Long) {
        runOnUiThread {
            textOutput.text = ""
        }
        displayMessagesForSelectedChatRoom(chatRoomId)
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
        textOutput = findViewById(R.id.textOutput) ?: throw IllegalStateException("textOutput not found")
        textInput = findViewById(R.id.textInput) ?: throw IllegalStateException("textInput not found")
        btnSend = findViewById(R.id.btnSend) ?: throw IllegalStateException("btnSend not found")
    }

    private fun getSenderIdForSelectedChat(selectedChatRoom: ChatDTO?): Long {
        return selectedChatRoom?.chat?.owner?.id ?: -1L
    }

    private fun displayMessagesForSelectedChatRoom(chatRoomId: Long) {
        val messages = chatMessagesMap[chatRoomId]
        val filteredMessages = messages?.filter { it.chatId == chatRoomId }

        textOutput.text = ""
        messageAdapter.submitList(filteredMessages)
        recyclerViewMessages.scrollToPosition(filteredMessages?.size?.minus(1) ?: 0)
    }

    private fun setupButtonClickListener() {
        btnSend.setOnClickListener {
            val selectedChatRoom = listOfChatDTOs.find { it.chat.id == selectedChatRoomId }
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
