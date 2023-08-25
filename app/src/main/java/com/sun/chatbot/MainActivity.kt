package com.sun.chatbot

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sun.chatbot.adapter.MessageRVAdapter
import com.sun.chatbot.data.ChatMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var chatsRV: RecyclerView
    private lateinit var sendMsgIB: ImageButton
    private lateinit var userMsgEdt: EditText
    private val USER_KEY = "user"
    private val BOT_KEY = "bot"
    private lateinit var messageModalArrayList: ArrayList<ChatMessage>
    private lateinit var messageRVAdapter: MessageRVAdapter
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatsRV = findViewById(R.id.idRVChats)
        sendMsgIB = findViewById(R.id.idIBSend)
        userMsgEdt = findViewById(R.id.idEdtMessage)

        messageModalArrayList = ArrayList()
        messageRVAdapter = MessageRVAdapter(messageModalArrayList, this)

        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        chatsRV.layoutManager = linearLayoutManager
        chatsRV.adapter = messageRVAdapter

        sendMsgIB.setOnClickListener {
            val userMessage = userMsgEdt.text.toString()
            if (userMessage.isEmpty()) {
                Toast.makeText(this@MainActivity, "Please enter your message..", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendMessage(userMessage)
            userMsgEdt.text.clear()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("NotifyDataSetChanged")
    private fun sendMessage(userMsg: String) {
        messageModalArrayList.add(ChatMessage(userMsg, USER_KEY))
        messageRVAdapter.notifyDataSetChanged()

        val url = "https://ai-chatbot.p.rapidapi.com/chat/free?message=$userMsg&uid=user1"

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-RapidAPI-Key", "4eb9e442d4mshe75760479e178afp17bf9bjsne6494e0da592")
                    .addHeader("X-RapidAPI-Host", "ai-chatbot.p.rapidapi.com")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val botResponse = jsonResponse?.optJSONObject("chatbot")?.getString("response")

                    runOnUiThread {
                        botResponse?.let { ChatMessage(it, BOT_KEY) }
                            ?.let { messageModalArrayList.add(it) }
                        messageRVAdapter.notifyDataSetChanged()
                    }
                } else {
                    runOnUiThread {
                        messageModalArrayList.add(ChatMessage("No response", BOT_KEY))
                        messageRVAdapter.notifyDataSetChanged()
                        Toast.makeText(this@MainActivity, "No response from the bot..", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    messageModalArrayList.add(ChatMessage("Sorry no response found", BOT_KEY))
                    messageRVAdapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, "No response from the bot..", Toast.LENGTH_SHORT).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                runOnUiThread {
                    messageModalArrayList.add(ChatMessage("JSON Error", BOT_KEY))
                    messageRVAdapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, "JSON Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
