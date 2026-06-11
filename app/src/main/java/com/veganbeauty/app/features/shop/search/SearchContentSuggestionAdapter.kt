package com.veganbeauty.app.features.shop.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ShopSearchBarBinding

class SearchContentSuggestionAdapter(
    private val onItemClick: (ContentSuggestion) -> Unit
) : RecyclerView.Adapter<SearchContentSuggestionAdapter.ViewHolder>() {

    private val items = mutableListOf<ContentSuggestion>()

    fun submitList(newItems: List<ContentSuggestion>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ShopSearchBarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ContentSuggestion) {
            binding.tvText.text = item.label
            binding.ivIcon.setImageResource(
                when (item.type) {
                    ContentSuggestionType.CATEGORY -> R.drawable.ic_tag
                    ContentSuggestionType.VIDEO -> R.drawable.ic_chat_video
                    ContentSuggestionType.BLOG -> R.drawable.ic_article_outline
                    ContentSuggestionType.POST -> R.drawable.ic_community
                }
            )
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopSearchBarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
