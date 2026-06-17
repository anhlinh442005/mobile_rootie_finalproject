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
import com.veganbeauty.app.data.local.entities.ConversationEntity

class MessageAdapter(
    private var items: List<ConversationEntity>,
    private val onItemClick: (ConversationEntity) -> Unit
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

    private fun formatTimestamp(timestampStr: String): String {
        if (timestampStr.isEmpty()) return ""
        try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = format.parse(timestampStr) ?: return ""

            val calendar = java.util.Calendar.getInstance()
            val now = java.util.Calendar.getInstance()
            calendar.time = date

            val formatTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN"))
            val formatDate = java.text.SimpleDateFormat("dd/MM", java.util.Locale("vi", "VN"))

            val timeStr = formatTime.format(calendar.time)

            return if (calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
            ) {
                timeStr
            } else {
                val dateStr = formatDate.format(calendar.time)
                "$timeStr $dateStr"
            }
        } catch (e: Exception) {
            return ""
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val item = items[position]
        
        val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(holder.itemView.context)
        val partnerId = item.members.firstOrNull { it != currentUserId } ?: ""
        val partnerInfo = item.memberInfo[partnerId]
        
        val partnerName = partnerInfo?.name ?: "Unknown"
        val partnerAvatar = partnerInfo?.avatar ?: ""
        
        val lastMsgText = item.lastMessage
        val lastTime = formatTimestamp(item.lastMessageAt)
        
        holder.tvName.text = partnerName
        holder.tvLastMessage.text = lastMsgText
        holder.tvTime.text = lastTime

        if (partnerAvatar.isNotEmpty()) {
            holder.ivAvatar.load(partnerAvatar) {
                crossfade(true)
                placeholder(R.color.gray_light)
                if (partnerId == "rootie_vn") {
                    error(R.drawable.ic_logo_rootie)
                } else {
                    error(R.drawable.img_avatar)
                }
                transformations(CircleCropTransformation())
            }
        } else {
            if (partnerId == "rootie_vn") {
                holder.ivAvatar.setImageResource(R.drawable.ic_logo_rootie)
            } else {
                holder.ivAvatar.setImageResource(R.drawable.img_avatar)
            }
        }

        holder.vActiveDot.visibility = if (item.activeBy.contains(partnerId)) View.VISIBLE else View.GONE
        
        val isUnread = item.unreadBy.contains(currentUserId)
        if (isUnread) {
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

    fun updateData(newItems: List<ConversationEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
