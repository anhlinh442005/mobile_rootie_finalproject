package com.veganbeauty.app.features.home.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.HomeItemBestsellerBinding
import java.text.NumberFormat
import java.util.Locale

class HomeBestsellerAdapter(
    private val onItemClick: (ProductEntity) -> Unit = {}
) : ListAdapter<ProductEntity, HomeBestsellerAdapter.ViewHolder>(DiffCallback()) {

    private val priceFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = HomeItemBestsellerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class ViewHolder(
        private val binding: HomeItemBestsellerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity, rank: Int) {
            binding.root.setOnClickListener { onItemClick(product) }
            binding.tvRank.text = rank.toString()
            binding.tvProductName.text = product.name
            binding.tvPrice.text = priceFormatter.format(product.price)
            
            val originalPrice = product.price / 0.75 // Assuming 25% discount to match design
            binding.tvOriginalPrice.text = priceFormatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            
            binding.ivProduct.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
            oldItem == newItem
    }
}
