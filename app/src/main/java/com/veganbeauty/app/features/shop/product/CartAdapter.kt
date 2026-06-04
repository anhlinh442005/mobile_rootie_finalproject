package com.veganbeauty.app.features.shop.product

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.databinding.ShopItemCartBinding
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private val onQuantityChanged: (CartItemEntity, Int) -> Unit,
    private val onSelectionToggled: (CartItemEntity, Boolean) -> Unit
) : ListAdapter<CartItemEntity, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ShopItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ShopItemCartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CartItemEntity) {
            binding.tvProductName.text = item.name
            binding.tvQuantityValue.text = item.quantity.toString()

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

            // Bind checkbox select state
            if (item.isSelected) {
                binding.ivSelect.setImageResource(R.drawable.ic_cart_checked)
            } else {
                binding.ivSelect.setImageResource(R.drawable.ic_cart_unchecked)
            }

            binding.ivSelect.setOnClickListener {
                onSelectionToggled(item, !item.isSelected)
            }

            binding.btnPlus.setOnClickListener {
                onQuantityChanged(item, item.quantity + 1)
            }

            binding.btnMinus.setOnClickListener {
                onQuantityChanged(item, item.quantity - 1)
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItemEntity>() {
        override fun areItemsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CartItemEntity, newItem: CartItemEntity) = oldItem == newItem
    }
}
