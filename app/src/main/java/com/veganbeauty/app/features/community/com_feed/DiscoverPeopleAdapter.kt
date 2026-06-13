package com.veganbeauty.app.features.community.com_feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.decode.SvgDecoder
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.databinding.ComItemDiscoverPersonBinding

class DiscoverPeopleAdapter(
    private var users: List<UserEntity>,
    private val actionType: ActionType,
    private val onActionClick: ((UserEntity, String) -> Unit)? = null
) : RecyclerView.Adapter<DiscoverPeopleAdapter.PersonViewHolder>() {

    enum class ActionType {
        SUGGEST, REQUEST, FOLLOW_BACK
    }

    class PersonViewHolder(val binding: ComItemDiscoverPersonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonViewHolder {
        val binding = ComItemDiscoverPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PersonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PersonViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvUsername.text = user.username
        
        if (!user.avatar.isNullOrEmpty()) {
            holder.binding.ivAvatar.load(user.avatar) {
                decoderFactory(SvgDecoder.Factory())
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(R.drawable.img_avatar)
            }
        } else {
            holder.binding.ivAvatar.setImageResource(android.R.color.darker_gray)
        }
        
        if (user.mutualCount > 0) {
            val friendName = user.firstMutualFriendName ?: "Ai đó"
            if (user.mutualCount == 1) {
                holder.binding.tvSubtitle.text = "Có $friendName đang theo dõi"
            } else {
                holder.binding.tvSubtitle.text = "Có $friendName và ${user.mutualCount - 1} người khác đang theo dõi"
            }
            holder.binding.tvSubtitle.visibility = View.VISIBLE
            
            // Show avatars if available
            val avatars = user.mutualFriendAvatars
            if (avatars.isNotEmpty()) {
                holder.binding.flMutualAvatars.visibility = View.VISIBLE
                
                // Show up to 3 avatars
                if (avatars.size >= 1) {
                    holder.binding.cvMutual1.visibility = View.VISIBLE
                    holder.binding.ivMutual1.load(avatars[0]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual1.visibility = View.GONE
                
                if (avatars.size >= 2) {
                    holder.binding.cvMutual2.visibility = View.VISIBLE
                    holder.binding.ivMutual2.load(avatars[1]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual2.visibility = View.GONE
                
                if (avatars.size >= 3) {
                    holder.binding.cvMutual3.visibility = View.VISIBLE
                    holder.binding.ivMutual3.load(avatars[2]) {
                        decoderFactory(SvgDecoder.Factory())
                        transformations(coil.transform.CircleCropTransformation())
                        crossfade(true)
                        error(R.drawable.img_avatar)
                    }
                } else holder.binding.cvMutual3.visibility = View.GONE
                
            } else {
                holder.binding.flMutualAvatars.visibility = View.GONE
            }
        } else {
            holder.binding.tvSubtitle.visibility = View.GONE
            holder.binding.flMutualAvatars.visibility = View.GONE
        }

        // Configure buttons based on ActionType
        when (actionType) {
            ActionType.SUGGEST -> {
                holder.binding.btnPrimary.text = "Theo dõi"
                holder.binding.btnSecondary.visibility = View.GONE
                holder.binding.ivClose.visibility = View.VISIBLE
            }
            ActionType.REQUEST -> {
                holder.binding.btnPrimary.text = "Xác nhận"
                holder.binding.btnSecondary.visibility = View.VISIBLE
                holder.binding.ivClose.visibility = View.GONE
            }
            ActionType.FOLLOW_BACK -> {
                holder.binding.btnPrimary.text = "Theo dõi lại"
                holder.binding.btnSecondary.visibility = View.GONE
                holder.binding.ivClose.visibility = View.VISIBLE
            }
        }

        holder.binding.btnPrimary.setOnClickListener {
            // Mock Action
            if (holder.binding.btnPrimary.text.toString() == "Theo dõi" || holder.binding.btnPrimary.text.toString() == "Theo dõi lại") {
                onActionClick?.invoke(user, "FOLLOW")
                holder.binding.btnPrimary.text = "Đang theo dõi"
                holder.binding.btnPrimary.setBackgroundResource(R.drawable.com_bg_filter_normal)
                holder.binding.btnPrimary.setTextColor(holder.itemView.context.getColor(R.color.primary))
            } else if (holder.binding.btnPrimary.text.toString() == "Xác nhận") {
                onActionClick?.invoke(user, "ACCEPT_REQUEST")
                holder.binding.btnPrimary.text = "Đã xác nhận"
                holder.binding.btnSecondary.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserEntity>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
