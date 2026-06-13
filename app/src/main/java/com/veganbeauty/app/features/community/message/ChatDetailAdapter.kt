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

class ChatDetailAdapter(private var items: List<ChatMessageEntity>) :
    RecyclerView.Adapter<ChatDetailAdapter.ChatViewHolder>() {

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

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val item = items[position]

        // Simple mock timestamp string logic since createdAt is Long
        val timeStr = "Vừa xong"
        holder.tvTimestamp.visibility = View.VISIBLE
        holder.tvTimestamp.text = timeStr

        holder.tvMessage.text = item.text

        val currentUserId = "test_001"
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
            if (partnerStatus == "read") {
                holder.tvStatus.visibility = View.VISIBLE
                holder.tvStatus.text = "Đã xem"
            } else {
                holder.tvStatus.visibility = View.GONE
            }
        } else {
            // Partner message
            holder.ivPartnerAvatar.visibility = View.VISIBLE
            holder.vLeftSpacer.visibility = View.GONE
            holder.vRightSpacer.visibility = View.VISIBLE
            holder.llBubble.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DEE2D3")) // Light green
            holder.tvMessage.setTextColor(android.graphics.Color.BLACK)
            holder.tvStatus.visibility = View.GONE

            val nextItem = if (position < items.size - 1) items[position + 1] else null
            val nextIsMine = nextItem?.senderId == currentUserId
            if (nextItem != null && !nextIsMine) {
                holder.ivPartnerAvatar.visibility = View.INVISIBLE // Reserve space but hide if not last
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
