package com.veganbeauty.app.features.community.beauty_hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.YtVideoEntity

class RelatedVideoAdapter(
    private var videos: List<YtVideoEntity>,
    private val onItemClick: (YtVideoEntity) -> Unit
) : RecyclerView.Adapter<RelatedVideoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivRelatedThumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tvRelatedTitle)
        val tvChannel: TextView = view.findViewById(R.id.tvRelatedChannel)
        val tvStats: TextView = view.findViewById(R.id.tvRelatedStats)

        init {
            view.setOnClickListener { onItemClick(videos[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_related_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        holder.tvTitle.text = video.title
        holder.tvChannel.text = video.username
        
        // Fake views and date
        val fakeViews = (10..500).random() / 10f
        val months = (1..11).random()
        holder.tvStats.text = String.format("%.1fk lượt xem • %d tháng trước", fakeViews, months).replace(".", ",")
        
        // Extract YouTube ID from URL to get Thumbnail
        val videoId = extractYouTubeVideoId(video.url)
        val thumbnailUrl = if (videoId != null) {
            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        } else {
            video.url
        }
        
        holder.ivThumbnail.load(thumbnailUrl) {
            crossfade(true)
        }
    }

    override fun getItemCount() = videos.size

    fun updateData(newVideos: List<YtVideoEntity>) {
        videos = newVideos
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
