package com.veganbeauty.app.features.community.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.CommunityPostEntity

class ProfileGridAdapter(
    private val posts: List<CommunityPostEntity>,
    private val onItemClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder>() {

    class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.ivGridImage)
        val tvGridText: android.widget.TextView = view.findViewById(R.id.tvGridText)
        val ivMultipleIcon: ImageView = view.findViewById(R.id.ivMultipleIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.com_item_profile_grid, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val post = posts[position]
        val mediaUrls = post.mediaUrlsString.split(",").filter { it.isNotBlank() }
        
        if (mediaUrls.isNotEmpty()) {
            holder.imageView.visibility = View.VISIBLE
            holder.tvGridText.visibility = View.GONE
            holder.imageView.load(mediaUrls[0]) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        } else {
            holder.imageView.visibility = View.GONE
            holder.tvGridText.visibility = View.VISIBLE
            holder.tvGridText.text = post.content
        }
        
        if (mediaUrls.size > 1) {
            holder.ivMultipleIcon.visibility = View.VISIBLE
        } else {
            holder.ivMultipleIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(position)
        }
    }

    override fun getItemCount() = posts.size
}
