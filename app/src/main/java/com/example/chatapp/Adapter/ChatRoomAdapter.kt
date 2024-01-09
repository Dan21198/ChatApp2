package com.example.chatapp.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.chatapp.model.ChatDTO

class ChatRoomAdapter(context: Context, private val chatRooms: MutableList<ChatDTO>) :
    ArrayAdapter<ChatDTO>(context, android.R.layout.simple_list_item_1, chatRooms) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val chatRoom = chatRooms[position]
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)

        val textView = rowView.findViewById<TextView>(android.R.id.text1)
        textView.text = chatRoom.chat?.chatName ?: ""

        rowView.tag = chatRoom.chat?.id ?: -1L

        return rowView
    }
}