package com.veganbeauty.app.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.HomeItemShortcutBinding

data class HomeShortcutItem(
    val title: String,
    val iconResId: Int,
    val action: () -> Unit
)

class HomeShortcutAdapter : RecyclerView.Adapter<HomeShortcutAdapter.ViewHolder>() {

    private val items = mutableListOf<HomeShortcutItem>()

    fun submitList(newItems: List<HomeShortcutItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HomeItemShortcutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: HomeItemShortcutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeShortcutItem) {
            binding.tvShortcutName.text = item.title
            binding.ivShortcutIcon.setImageResource(item.iconResId)
            binding.root.setOnClickListener { item.action() }
        }
    }
}
