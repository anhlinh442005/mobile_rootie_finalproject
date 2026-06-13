package com.veganbeauty.app.features.home.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ItemHomeFlashsaleProductBinding
import java.text.NumberFormat
import java.util.Locale

class HomeFlashsaleAdapter(
    private val onItemClick: (ProductEntity) -> Unit = {},
    private val onAddToCart: (ProductEntity) -> Unit = {}
) : ListAdapter<ProductEntity, HomeFlashsaleAdapter.ViewHolder>(DiffCallback()) {

    private val priceFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeFlashsaleProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemHomeFlashsaleProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.root.setOnClickListener { onItemClick(product) }
            binding.btnFlashBuy.setOnClickListener { onAddToCart(product) }
            binding.tvFlashProductName.text = product.name
            val random = java.util.Random(product.id.hashCode().toLong())
            val discount = random.nextInt(31) + 20 // 20% to 50%
            binding.tvFlashDiscountBadge.text = "-$discount%"
            
            val originalPrice = product.price
            val salePrice = originalPrice * (100 - discount) / 100.0

            binding.tvFlashPrice.text = priceFormatter.format(salePrice)
            
            binding.tvFlashOriginalPrice.text = priceFormatter.format(originalPrice)
            binding.tvFlashOriginalPrice.paintFlags = binding.tvFlashOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            
            binding.ivFlashProduct.load(product.mainImage) {
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
