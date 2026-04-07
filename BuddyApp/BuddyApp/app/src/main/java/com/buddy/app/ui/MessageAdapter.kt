package com.buddy.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.buddy.app.R
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<BuddyMessage>()

    companion object {
        private const val VIEW_USER = 0
        private const val VIEW_BUDDY = 1
    }

    inner class MessageViewHolder(itemView: View, private val isUser: Boolean) :
        RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tvMessage)
        val timeView: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(message: BuddyMessage) {
            textView.text = message.text
            timeView.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(message.timestamp))
        }
    }

    override fun getItemViewType(position: Int) =
        if (messages[position].isUser) VIEW_USER else VIEW_BUDDY

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_USER) R.layout.item_message_user
                     else R.layout.item_message_buddy
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view, viewType == VIEW_USER)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: BuddyMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun getMessages() = messages.toList()
    fun clear() { messages.clear(); notifyDataSetChanged() }
}
