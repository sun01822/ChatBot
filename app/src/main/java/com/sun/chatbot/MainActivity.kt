package com.sun.chatbot

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sun.chatbot.adapter.MessageRVAdapter
import com.sun.chatbot.data.ChatMessage
import com.sun.chatbot.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding : ActivityMainBinding
    private lateinit var chatsRV: RecyclerView
    private lateinit var sendMsgIB: ImageButton
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var userMsgEdt: EditText
    private val USER_KEY = "user"
    private val BOT_KEY = "bot"
    private lateinit var messageModalArrayList: ArrayList<ChatMessage>
    private lateinit var messageRVAdapter: MessageRVAdapter
    private val REQUEST_CODE_SPEECH_INPUT = 1
    private val client = OkHttpClient()
    //Text to Speech
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        chatsRV = findViewById(R.id.idRVChats)
        sendMsgIB = findViewById(R.id.idIBSend)
        userMsgEdt = findViewById(R.id.idEdtMessage)

        messageModalArrayList = ArrayList()
        messageRVAdapter = MessageRVAdapter(messageModalArrayList, this)

        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        chatsRV.layoutManager = linearLayoutManager
        chatsRV.adapter = messageRVAdapter

        // TextToSpeech(Context: this, OnInitListener: this)
        tts = TextToSpeech(this, this)


        sendMsgIB.setOnClickListener {
            val userMessage = userMsgEdt.text.toString()
            if (userMessage.isEmpty()) {
                Toast.makeText(this@MainActivity, "Please enter your message..", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendMessage(userMessage)
            userMsgEdt.text.clear()
        }

        binding.idIBVoice.setOnClickListener {
            // on below line we are calling speech recognizer intent.
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            // on below line we are passing language model
            // and model free form in our intent
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            // on below line we are passing our
            // language as a default language.
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            // on below line we are specifying a prompt
            // message as speak to text on below line.
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

            // on below line we are specifying a try catch block.
            // in this block we are calling a start activity
            // for result method and passing our result code.
            try {
                startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
            } catch (e: Exception) {
                // on below line we are displaying error message in toast
                Toast
                    .makeText(
                        this@MainActivity, " " + e.message,
                        Toast.LENGTH_SHORT
                    )
                    .show()
            }
        }
    }

    // on below line we are calling on activity result method.
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // in this method we are checking request
        // code with our result code.
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            // on below line we are checking if result code is ok
            if (resultCode == RESULT_OK && data != null) {

                // in that case we are extracting the
                // data from our array list
                val res: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                // on below line we are setting data
                // to our output text view.
                binding.idEdtMessage.setText(
                    Objects.requireNonNull(res)[0]
                )
            }
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
                    speakMessage(botResponse.toString())
                    runOnUiThread {
                        botResponse?.let { ChatMessage(it, BOT_KEY) }
                            ?.let { messageModalArrayList.add(it) }
                        messageRVAdapter.notifyDataSetChanged()
                    }
                } else {
                    runOnUiThread {
                        messageModalArrayList.add(ChatMessage("No response", BOT_KEY))
                        messageRVAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    messageModalArrayList.add(ChatMessage("Sorry no response found", BOT_KEY))
                    messageRVAdapter.notifyDataSetChanged()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                runOnUiThread {
                    messageModalArrayList.add(ChatMessage("JSON Error", BOT_KEY))
                    messageRVAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun speakMessage(speak : String){
        tts!!.speak(speak, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language not supported!")
            }
        }
    }

    public override fun onDestroy() {
        // Shutdown TTS when
        // activity is destroyed
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}
