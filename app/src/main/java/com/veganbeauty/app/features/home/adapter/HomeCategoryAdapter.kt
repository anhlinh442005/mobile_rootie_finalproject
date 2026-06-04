package com.veganbeauty.app.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.HomeItemCategoryBinding

data class HomeCategoryItem(val name: String)

class HomeCategoryAdapter : ListAdapter<HomeCategoryItem, HomeCategoryAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HomeItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: HomeItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeCategoryItem) {
            binding.tvCategoryName.text = item.name
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<HomeCategoryItem>() {
        override fun areItemsTheSame(oldItem: HomeCategoryItem, newItem: HomeCategoryItem) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: HomeCategoryItem, newItem: HomeCategoryItem) =
            oldItem == newItem
    }
}
