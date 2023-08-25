package com.sun.chatbot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sun.chatbot.R
import com.sun.chatbot.data.ChatMessage

class MessageRVAdapter(
    private val messageModalArrayList: ArrayList<ChatMessage>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        when (viewType) {
            0 -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.user_msg, parent, false)
                return UserViewHolder(view)
            }
            1 -> {
                view = LayoutInflater.from(parent.context).inflate(R.layout.bot_msg, parent, false)
                return BotViewHolder(view)
            }
        }
        return object : RecyclerView.ViewHolder(View(context)) {}
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val modal = messageModalArrayList[position]
        when (modal.sender) {
            "user" -> (holder as UserViewHolder).userTV.text = modal.message
            "bot" -> (holder as BotViewHolder).botTV.text = modal.message
        }
    }

    override fun getItemCount(): Int {
        return messageModalArrayList.size
    }

    override fun getItemViewType(position: Int): Int {
        when (messageModalArrayList[position].sender) {
            "user" -> return 0
            "bot" -> return 1
        }
        return -1
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userTV: TextView = itemView.findViewById(R.id.idTVUser)
    }

    inner class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var botTV: TextView = itemView.findViewById(R.id.idTVBot)
    }
}



