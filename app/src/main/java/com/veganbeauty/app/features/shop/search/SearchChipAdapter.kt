package com.veganbeauty.app.features.shop.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ShopItemSearchChipBinding

class SearchChipAdapter(
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SearchChipAdapter.ViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ShopItemSearchChipBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.tvChip.text = text
            binding.root.setOnClickListener { onItemClick(text) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopItemSearchChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
