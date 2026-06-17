package com.veganbeauty.app.features.community.com_feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.UserEntity

class UserSearchAdapter(
    private var users: List<UserEntity>,
    private val onUserClick: (UserEntity) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_search_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.tvName.text = user.full_name.ifBlank { user.username }
        holder.tvUsername.text = "@${user.username.lowercase().replace(" ", "_")}"
        
        if (!user.avatar.isNullOrEmpty()) {
            holder.ivAvatar.load(user.avatar) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.img_avatar)
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.img_avatar)
        }

        holder.itemView.setOnClickListener {
            onUserClick(user)
        }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<UserEntity>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
