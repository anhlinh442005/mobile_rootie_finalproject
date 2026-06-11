package com.veganbeauty.app.features.shop.search

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.databinding.ShopSearchVideoItemBinding
import java.util.regex.Pattern

class SearchVideoAdapter : RecyclerView.Adapter<SearchVideoAdapter.ViewHolder>() {

    private val items = mutableListOf<YtVideoEntity>()

    fun submitList(newItems: List<YtVideoEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ShopSearchVideoItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(video: YtVideoEntity) {
            binding.tvTitle.text = video.title
            binding.tvDescription.text = video.description.ifBlank { video.username }

            val videoId = extractYouTubeVideoId(video.url)
            val thumbnailUrl = when {
                videoId != null -> "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                video.url.contains("cloudinary.com") && video.url.endsWith(".mp4", true) ->
                    video.url.substringBeforeLast(".") + ".jpg"
                else -> video.url
            }
            binding.ivThumbnail.load(thumbnailUrl) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }

            binding.root.setOnClickListener {
                binding.root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url)))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopSearchVideoItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    private fun extractYouTubeVideoId(url: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|shorts\\/)[^#\\&\\?\\n]*"
        val matcher = Pattern.compile(pattern).matcher(url)
        return if (matcher.find()) matcher.group() else null
    }
}
