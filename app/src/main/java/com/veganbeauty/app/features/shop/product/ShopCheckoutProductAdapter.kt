package com.veganbeauty.app.features.shop.product

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.databinding.ShopItemCheckoutProductBinding
import java.text.NumberFormat
import java.util.Locale

class ShopCheckoutProductAdapter : ListAdapter<CartItemEntity, ShopCheckoutProductAdapter.CheckoutProductViewHolder>(CheckoutProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutProductViewHolder {
        val binding = ShopItemCheckoutProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CheckoutProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CheckoutProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CheckoutProductViewHolder(private val binding: ShopItemCheckoutProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItemEntity) {
            binding.tvProductName.text = item.name
            binding.tvQuantity.text = "x${item.quantity}"

            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvPrice.text = formatter.format(item.price)

            // Setup original price (e.g. 1.2x price)
            val originalPrice = (item.price * 1.2).toLong()
            binding.tvOriginalPrice.text = formatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            // Load product image using Coil
            binding.ivProduct.load(item.image) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
        }
    }

    class CheckoutProductDiffCallback : DiffUtil.ItemCallback<CartItemEntity>() {
        override fun areItemsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) = oldItem == newItem
    }
}
