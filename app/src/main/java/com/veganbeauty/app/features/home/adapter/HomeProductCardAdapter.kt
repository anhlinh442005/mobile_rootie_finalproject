package com.veganbeauty.app.features.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.HomeItemProductHorizontalBinding
import java.text.NumberFormat
import java.util.Locale

class HomeProductCardAdapter(
    private val onItemClick: (ProductEntity) -> Unit = {},
    private val onAddToCart: (ProductEntity) -> Unit = {}
) : ListAdapter<ProductEntity, HomeProductCardAdapter.ViewHolder>(DiffCallback()) {

    private val priceFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HomeItemProductHorizontalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: HomeItemProductHorizontalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.root.setOnClickListener { onItemClick(product) }
            binding.btnAddToCart.setOnClickListener { onAddToCart(product) }
            binding.tvProductName.text = product.name
            binding.tvPrice.text = priceFormatter.format(product.price)
            
            val originalPrice = (product.price / 0.65).toLong()
            binding.tvOriginalPrice.text = priceFormatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            
            binding.ivProduct.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
            binding.tvSaleBadge.text = "-35%"
            binding.tvSaleBadge.visibility = View.VISIBLE
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
            oldItem == newItem
    }
}
