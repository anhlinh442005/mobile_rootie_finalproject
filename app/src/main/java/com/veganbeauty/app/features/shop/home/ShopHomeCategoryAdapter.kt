package com.veganbeauty.app.features.shop.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ShopHomeCategoryBinding
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel

class ShopHomeCategoryAdapter(
    private val onCategoryClick: (CategoryUiModel) -> Unit
) : ListAdapter<CategoryUiModel, ShopHomeCategoryAdapter.CategoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ShopHomeCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(private val binding: ShopHomeCategoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: CategoryUiModel) {
            binding.tvCategoryName.text = category.name
            binding.tvProductCount.text = "${category.productCount} sản phẩm"
            binding.ivCategoryIcon.setImageResource(category.iconRes)
            
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryUiModel>() {
        override fun areItemsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel) = oldItem == newItem
    }
}
