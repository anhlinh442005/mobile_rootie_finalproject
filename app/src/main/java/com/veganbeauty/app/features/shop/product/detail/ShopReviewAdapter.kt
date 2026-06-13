package com.veganbeauty.app.features.shop.product.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ShopItemReviewBinding

class ShopReviewAdapter(
    private var items: List<ProductReview>
) : RecyclerView.Adapter<ShopReviewAdapter.ViewHolder>() {

    fun updateData(newItems: List<ProductReview>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopItemReviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ShopItemReviewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductReview) {
            binding.tvReviewerName.text = item.reviewerName
            binding.tvAvatarChar.text = if (item.reviewerName.isNotEmpty()) {
                item.reviewerName.take(1).uppercase()
            } else {
                "U"
            }
            binding.tvReviewComment.text = "\"${item.comment}\""

            // Set star rating
            val starImageViews = listOf(
                binding.ivStar1, 
                binding.ivStar2, 
                binding.ivStar3, 
                binding.ivStar4, 
                binding.ivStar5
            )
            starImageViews.forEachIndexed { index, imageView ->
                if (index < item.rating) {
                    imageView.visibility = View.VISIBLE
                } else {
                    imageView.visibility = View.GONE
                }
            }
        }
    }
}
