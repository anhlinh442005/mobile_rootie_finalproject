package com.veganbeauty.app.features.community.beauty_hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity
import java.text.SimpleDateFormat
import java.util.Locale

class LatestKnowledgeAdapter(private var blogs: List<CommunityBlogEntity>) :
    RecyclerView.Adapter<LatestKnowledgeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBlog: ImageView = view.findViewById(R.id.ivBlog)
        val tvTitle: TextView = view.findViewById(R.id.tvBlogTitle)
        val tvDesc: TextView = view.findViewById(R.id.tvBlogDesc)
        val tvDate: TextView = view.findViewById(R.id.tvBlogDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_latest_knowledge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = blogs[position]
        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.shortDescription
        
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val date = parser.parse(item.publishedAt)
            holder.tvDate.text = if (date != null) formatter.format(date) else item.publishedAt
        } catch (e: Exception) {
            holder.tvDate.text = item.publishedAt
        }
        
        if (item.imageUrl.isNotEmpty()) {
            holder.ivBlog.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.img_placeholder)
                error(R.drawable.img_placeholder)
            }
        } else {
            holder.ivBlog.setImageResource(R.drawable.img_placeholder)
        }
    }

    override fun getItemCount() = blogs.size

    fun updateData(newBlogs: List<CommunityBlogEntity>) {
        blogs = newBlogs
        notifyDataSetChanged()
    }
}
