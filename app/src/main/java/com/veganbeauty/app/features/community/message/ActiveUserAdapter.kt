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

class ActiveUserAdapter(private var items: List<ConversationEntity>) :
    RecyclerView.Adapter<ActiveUserAdapter.ActiveUserViewHolder>() {

    class ActiveUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val ivAddStory: ImageView = view.findViewById(R.id.ivAddStory)
        val vActiveDot: View = view.findViewById(R.id.vActiveDot)
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveUserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_active_user, parent, false)
        return ActiveUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActiveUserViewHolder, position: Int) {
        // First item is "Tin của bạn" (Your story)
        if (position == 0) {
            holder.tvName.text = "Tin của bạn"
            holder.tvName.setTextColor(holder.itemView.context.resources.getColor(R.color.gray_dark, null))
            holder.ivAddStory.visibility = View.VISIBLE
            holder.vActiveDot.visibility = View.GONE
            
            val currentUserAvatar = com.veganbeauty.app.data.local.ProfileSession.getAvatar(holder.itemView.context)
            if (!currentUserAvatar.isNullOrEmpty()) {
                holder.ivAvatar.load(currentUserAvatar) {
                    crossfade(true)
                    placeholder(R.color.gray_light)
                    error(R.drawable.mascot_message)
                    transformations(CircleCropTransformation())
                }
            } else {
                holder.ivAvatar.load(R.drawable.mascot_message) {
                    transformations(CircleCropTransformation())
                }
            }
            return
        }

        // Adjust index for actual data
        val item = items[position - 1]
        
        val currentUserId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(holder.itemView.context)
        val partnerId = (item.members ?: emptyList()).firstOrNull { it != currentUserId } ?: ""
        val partnerInfo = (item.memberInfo ?: emptyMap())[partnerId]
        
        var partnerName = partnerInfo?.name ?: "Unknown"
        var partnerAvatar = partnerInfo?.avatar ?: ""

        // Fix for legacy 'You' data stored in db
        if (partnerName == "You" || partnerName == "Unknown" || partnerAvatar.isEmpty()) {
            val user = com.veganbeauty.app.data.local.LocalJsonReader(holder.itemView.context).getUsers().find { it.user_id == partnerId }
            if (user != null) {
                if (partnerName == "You" || partnerName == "Unknown") partnerName = user.full_name ?: partnerName
                if (partnerAvatar.isEmpty()) partnerAvatar = user.avatar ?: ""
            } else if (partnerId == "test_001") {
                if (partnerName == "You" || partnerName == "Unknown") partnerName = "Test User"
            }
        }
        
        val isActive = (item.activeBy ?: emptyList()).contains(partnerId)
        
        // Convert name to username-like format (e.g., "Bảo Nguyên" -> "bao_nguyen")
        val shortName = partnerName.lowercase()
            .replace(" ", "_")
            .replace("á", "a").replace("à", "a").replace("ả", "a").replace("ã", "a").replace("ạ", "a")
            .replace("ă", "a").replace("ắ", "a").replace("ằ", "a").replace("ẳ", "a").replace("ẵ", "a").replace("ặ", "a")
            .replace("â", "a").replace("ấ", "a").replace("ầ", "a").replace("ẩ", "a").replace("ẫ", "a").replace("ậ", "a")
            .replace("đ", "d")
            .replace("é", "e").replace("è", "e").replace("ẻ", "e").replace("ẽ", "e").replace("ẹ", "e")
            .replace("ê", "e").replace("ế", "e").replace("ề", "e").replace("ể", "e").replace("ễ", "e").replace("ệ", "e")
            .replace("í", "i").replace("ì", "i").replace("ỉ", "i").replace("ĩ", "i").replace("ị", "i")
            .replace("ó", "o").replace("ò", "o").replace("ỏ", "o").replace("õ", "o").replace("ọ", "o")
            .replace("ô", "o").replace("ố", "o").replace("ồ", "o").replace("ổ", "o").replace("ỗ", "o").replace("ộ", "o")
            .replace("ơ", "o").replace("ớ", "o").replace("ờ", "o").replace("ở", "o").replace("ỡ", "o").replace("ợ", "o")
            .replace("ú", "u").replace("ù", "u").replace("ủ", "u").replace("ũ", "u").replace("ụ", "u")
            .replace("ư", "u").replace("ứ", "u").replace("ừ", "u").replace("ử", "u").replace("ữ", "u").replace("ự", "u")
            .replace("ý", "y").replace("ỳ", "y").replace("ỷ", "y").replace("ỹ", "y").replace("ỵ", "y")
            
        holder.tvName.text = shortName
        holder.tvName.setTextColor(holder.itemView.context.resources.getColor(R.color.gray_dark, null))
        holder.ivAddStory.visibility = View.GONE
        holder.vActiveDot.visibility = if (isActive) View.VISIBLE else View.GONE
        
        if (partnerAvatar.isNotEmpty()) {
            holder.ivAvatar.load(partnerAvatar) {
                crossfade(true)
                placeholder(R.color.gray_light)
                if (partnerId == "rootie_vn") {
                    error(R.drawable.ic_logo_rootie)
                } else {
                    error(R.drawable.mascot_message)
                }
                transformations(CircleCropTransformation())
            }
        } else {
            if (partnerId == "rootie_vn") {
                holder.ivAvatar.setImageResource(R.drawable.ic_logo_rootie)
            } else {
                holder.ivAvatar.setImageResource(R.drawable.mascot_message)
            }
        }
    }

    override fun getItemCount() = items.size + 1 // +1 for "Tin của bạn"

    fun updateData(newItems: List<ConversationEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
