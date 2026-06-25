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
        @android.annotation.SuppressLint("ClickableViewAccessibility")
        fun bind(category: CategoryUiModel) {
            binding.tvCategoryName.text = category.name
            binding.tvProductCount.text = "${category.productCount} sản phẩm"
            binding.ivCategoryIcon.setImageResource(category.iconRes)
            
            binding.root.setOnClickListener {
                onCategoryClick(category)
            }

            val density = binding.root.context.resources.displayMetrics.density
            binding.root.setOnTouchListener { view, event ->
                val card = view as com.google.android.material.card.MaterialCardView
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        card.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .translationZ(6f * density)
                            .setDuration(120)
                            .start()
                        card.strokeColor = androidx.core.content.ContextCompat.getColor(card.context, com.veganbeauty.app.R.color.primary)
                        card.strokeWidth = (2.5f * density).toInt()
                    }
                    android.view.MotionEvent.ACTION_UP -> {
                        card.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationZ(0f)
                            .setDuration(150)
                            .start()
                        card.strokeColor = android.graphics.Color.parseColor("#EAEAEA")
                        card.strokeWidth = (1f * density).toInt()
                        card.performClick()
                    }
                    android.view.MotionEvent.ACTION_CANCEL -> {
                        card.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .translationZ(0f)
                            .setDuration(150)
                            .start()
                        card.strokeColor = android.graphics.Color.parseColor("#EAEAEA")
                        card.strokeWidth = (1f * density).toInt()
                    }
                }
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryUiModel>() {
        override fun areItemsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel) = oldItem == newItem
    }
}
