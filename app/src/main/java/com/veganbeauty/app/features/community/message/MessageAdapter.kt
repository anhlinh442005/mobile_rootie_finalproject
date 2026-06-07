package com.veganbeauty.app.features.community.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.MessageEntity

class MessageAdapter(
    private var items: List<MessageEntity>,
    private val onItemClick: (MessageEntity) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val vActiveDot: View = view.findViewById(R.id.vActiveDot)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val vUnreadDot: View = view.findViewById(R.id.vUnreadDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = items[position]
        
        val lastMsgText = item.messages.lastOrNull()?.text ?: ""
        val lastTime = item.messages.lastOrNull()?.timestamp ?: ""
        
        holder.tvName.text = item.partnerName
        holder.tvLastMessage.text = lastMsgText
        holder.tvTime.text = lastTime

        if (item.partnerAvatar.isNotEmpty()) {
            holder.ivAvatar.load(item.partnerAvatar) {
                crossfade(true)
                placeholder(R.color.gray_light)
                transformations(CircleCropTransformation())
            }
        }

        holder.vActiveDot.visibility = if (item.isActive) View.VISIBLE else View.GONE
        
        if (item.isUnread) {
            holder.vUnreadDot.visibility = View.VISIBLE
            holder.tvName.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.BOLD)
            holder.tvLastMessage.setTextColor(holder.itemView.context.resources.getColor(R.color.black, null))
        } else {
            holder.vUnreadDot.visibility = View.GONE
            holder.tvName.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.tvLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL)
            holder.tvLastMessage.setTextColor(holder.itemView.context.resources.getColor(R.color.gray_dark, null))
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MessageEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
