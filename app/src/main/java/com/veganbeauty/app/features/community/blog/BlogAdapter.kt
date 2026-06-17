package com.veganbeauty.app.features.community.blog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.veganbeauty.app.R

class BlogAdapter(
    private var posts: List<BlogPost>,
    private val onItemClick: (BlogPost) -> Unit
) : RecyclerView.Adapter<BlogAdapter.BlogViewHolder>() {

    class BlogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPostImage: ImageView = view.findViewById(R.id.ivPostImage)
        val tvPostCategory: TextView = view.findViewById(R.id.tvPostCategory)
        val tvPostTitle: TextView = view.findViewById(R.id.tvPostTitle)
        val tvPostDesc: TextView = view.findViewById(R.id.tvPostDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.com_item_blog_post, parent, false)
        return BlogViewHolder(view)
    }

    override fun onBindViewHolder(holder: BlogViewHolder, position: Int) {
        val post = posts[position]
        holder.tvPostTitle.text = post.title
        holder.tvPostCategory.text = post.category.ifEmpty { "Dưỡng da" }
        holder.tvPostDesc.text = "🕒 ${post.date} ${post.description}"
        if (post.imageUrl.isNotEmpty()) {
            holder.ivPostImage.load(post.imageUrl) {
                crossfade(true)
                error(R.color.gray_light)
                placeholder(R.color.gray_light)
            }
        } else {
            holder.ivPostImage.setImageResource(R.color.gray_light)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(post)
        }
    }

    override fun getItemCount() = posts.size

    fun updateData(newPosts: List<BlogPost>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}
