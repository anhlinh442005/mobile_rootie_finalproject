package com.veganbeauty.app.features.community.beauty_hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.YtVideoEntity

class HandbookVideoAdapter(
    private var videos: List<YtVideoEntity>,
    private val isHorizontal: Boolean = false,
    private val isSaved: (YtVideoEntity) -> Boolean = { false },
    private val onItemClick: (YtVideoEntity) -> Unit,
    private val onHeartClick: (YtVideoEntity) -> Unit,
    private val onDeleteClick: ((YtVideoEntity) -> Unit)? = null
) : RecyclerView.Adapter<HandbookVideoAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvLikes: TextView = view.findViewById(R.id.tvLikes)
        val tvDuration: TextView = view.findViewById(R.id.tvDuration)
        val ivHeart: ImageView = view.findViewById(R.id.ivHeart)
        val llHeartContainer: LinearLayout = view.findViewById(R.id.llHeartContainer)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)

        init {
            view.setOnClickListener { onItemClick(videos[adapterPosition]) }
            llHeartContainer.setOnClickListener { onHeartClick(videos[adapterPosition]) }
            ivDelete.setOnClickListener { onDeleteClick?.invoke(videos[adapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_handbook_video, parent, false)
        if (isHorizontal) {
            val dp = parent.context.resources.displayMetrics.density
            val params = view.layoutParams
            params.width = (160 * dp).toInt()
            view.layoutParams = params
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = videos[position]
        holder.tvTitle.text = video.title
        
        // Toggle Delete / Details
        if (isHorizontal && onDeleteClick != null) {
            holder.tvDetails.visibility = View.GONE
            holder.ivDelete.visibility = View.VISIBLE
        } else {
            holder.tvDetails.visibility = View.VISIBLE
            holder.ivDelete.visibility = View.GONE
        }
        
        // Fake likes between 1.0k and 50.0k
        val fakeLikes = (10..500).random() / 10f
        holder.tvLikes.text = String.format("%.1fk", fakeLikes).replace(".", ",")
        
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
        
        // Random duration for UI 14:25
        val min = (5..20).random()
        val sec = (10..59).random()
        holder.tvDuration.text = String.format("%d:%02d", min, sec)

        // Red heart if saved
        if (isSaved(video)) {
            holder.ivHeart.setColorFilter(android.graphics.Color.RED)
            holder.ivHeart.setImageResource(R.drawable.ic_heart_filled) // assuming it exists, or just tint it
        } else {
            holder.ivHeart.clearColorFilter()
        }
    }

    override fun getItemCount() = videos.size

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
