package io.mosip.greetings.chat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.mosip.greetings.R

class ChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val chatManager = ChatController(intent?.getIntExtra("mode", ChatController.PERIPHERAL_MODE)).manager

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        val mRecyclerView = findViewById<RecyclerView>(R.id.messagesRecylerView)
        val mAdapter = MessagesAdapter()
        val sendButton = findViewById<Button>(R.id.sendBtn)

        mRecyclerView.layoutManager = layoutManager
        mRecyclerView.adapter = mAdapter


        val description = findViewById<TextView>(R.id.chatDescription)
        description.text = "Talking to Central"

        chatManager.addMessageReceiver {
            mAdapter.addMessage(Message(it,  false))
        }

        sendButton.setOnClickListener {
            val messageInput = findViewById<EditText>(R.id.messageInput)
            val message = Message(messageInput.text.toString(), true)
            chatManager.sendMessage(message.text)
            mAdapter.addMessage(message)
        }
    }
}