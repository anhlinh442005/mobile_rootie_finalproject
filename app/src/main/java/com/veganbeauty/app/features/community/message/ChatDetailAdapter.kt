package com.veganbeauty.app.features.community.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.ChatMessageEntity

class ChatDetailAdapter(
    private var items: List<ChatMessageEntity>,
    private val onMessageLongClick: (ChatMessageEntity) -> Unit
) : RecyclerView.Adapter<ChatDetailAdapter.ChatViewHolder>() {

    private var partnerAvatar: String = ""

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val ivPartnerAvatar: ImageView = view.findViewById(R.id.ivPartnerAvatar)
        val vLeftSpacer: View = view.findViewById(R.id.vLeftSpacer)
        val llBubble: LinearLayout = view.findViewById(R.id.llBubble)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val vRightSpacer: View = view.findViewById(R.id.vRightSpacer)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    fun setPartnerAvatar(url: String) {
        partnerAvatar = url
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_chat_bubble, parent, false)
        return ChatViewHolder(view)
    }

    private var selectedItemPosition: Int = -1

    private fun formatTimestamp(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        val now = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val formatTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN"))
        val formatDate = java.text.SimpleDateFormat("dd 'th' MM", java.util.Locale("vi", "VN"))

        val timeStr = formatTime.format(calendar.time)

        return if (calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
        ) {
            timeStr
        } else {
            val dateStr = formatDate.format(calendar.time)
            "$timeStr ng $dateStr"
        }
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = items[position]

        val showTimestamp = if (position == 0) {
            true
        } else {
            val prevItem = items[position - 1]
            val diff = item.createdAt - prevItem.createdAt
            diff > 30 * 60 * 1000 // 30 minutes in milliseconds
        }

        if (showTimestamp || position == selectedItemPosition) {
            holder.tvTimestamp.visibility = View.VISIBLE
            holder.tvTimestamp.text = formatTimestamp(item.createdAt)
        } else {
            holder.tvTimestamp.visibility = View.GONE
        }

        holder.llBubble.setOnClickListener {
            val prevSelected = selectedItemPosition
            selectedItemPosition = if (selectedItemPosition == position) -1 else position
            if (prevSelected != -1) notifyItemChanged(prevSelected)
            if (selectedItemPosition != -1) notifyItemChanged(selectedItemPosition)
        }

        holder.tvMessage.text = item.text

        val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(holder.itemView.context)
        val isMine = item.senderId == currentUserId

        if (isMine) {
            // My message
            holder.ivPartnerAvatar.visibility = View.GONE
            holder.vLeftSpacer.visibility = View.VISIBLE
            holder.vRightSpacer.visibility = View.GONE
            holder.llBubble.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#758864")) // Dark green
            holder.tvMessage.setTextColor(android.graphics.Color.WHITE)

            // Show status (e.g., "Đã xem" if partner read it)
            val partnerId = if (item.receiverId == currentUserId) item.senderId else item.receiverId
            val partnerStatus = item.status[partnerId]
            if (partnerStatus == "read" && position == selectedItemPosition) {
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvStatus.text = "Đã xem"
            } else {
                holder.tvStatus.visibility = View.GONE
            }

            holder.llBubble.setOnLongClickListener {
                onMessageLongClick(item)
                true
            }
        } else {
            // Partner message
            holder.ivPartnerAvatar.visibility = View.VISIBLE
            holder.vLeftSpacer.visibility = View.GONE
            holder.vRightSpacer.visibility = View.VISIBLE
            holder.llBubble.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DEE2D3")) // Light green
            holder.tvMessage.setTextColor(android.graphics.Color.BLACK)
            holder.tvStatus.visibility = View.GONE
            
            holder.llBubble.setOnLongClickListener(null)

            val nextItem = if (position < items.size - 1) items[position + 1] else null
            val nextIsMine = nextItem?.senderId == currentUserId
            
            // Check if next item is also within 30 minutes and from same partner to hide avatar
            val nextIsClose = nextItem != null && (nextItem.createdAt - item.createdAt <= 30 * 60 * 1000)
            
            if (nextItem != null && !nextIsMine) {
                holder.ivPartnerAvatar.visibility = View.INVISIBLE // Reserve space but hide if not last in group
            } else {
                holder.ivPartnerAvatar.visibility = View.VISIBLE
                if (partnerAvatar.isNotEmpty()) {
                    holder.ivPartnerAvatar.load(partnerAvatar) {
                        crossfade(true)
                        transformations(CircleCropTransformation())
                    }
                }
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<ChatMessageEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
