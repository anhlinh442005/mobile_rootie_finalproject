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
