package com.veganbeauty.app.features.shop.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ShopSearchBarBinding

class SearchSuggestionAdapter(
    private val iconRes: Int,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestionAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ShopSearchBarBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.tvText.text = text
            binding.ivIcon.setImageResource(iconRes)
            binding.root.setOnClickListener { onItemClick(text) }
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
