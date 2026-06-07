package com.veganbeauty.app.features.community.beauty_hub

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.YtVideoEntity

class NotebookVideoAdapter(private var items: List<YtVideoEntity>) :
    RecyclerView.Adapter<NotebookVideoAdapter.NotebookViewHolder>() {

    class NotebookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotebookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_notebook_video, parent, false)
        return NotebookViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotebookViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        
        // Extract YouTube ID from URL to get Thumbnail
        val videoId = extractYouTubeVideoId(item.url)
        val thumbnailUrl = if (videoId != null) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else {
            item.url
        }
        
        holder.ivThumbnail.load(thumbnailUrl) {
            crossfade(true)
        }
            
        holder.tvDuration.text = "14:25"
        
        val randomLikes = (100..1000).random() * 10 // to ensure a neat number like 1200 or 1500
        holder.tvLikes.text = String.format(java.util.Locale.US, "%.1fk", randomLikes / 1000.0)
        
        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<YtVideoEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    private fun extractYouTubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|shorts\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\\u200C\\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }
}
