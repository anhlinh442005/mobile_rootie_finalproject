package com.veganbeauty.app.features.shop.product.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopProductCardBinding
import java.text.NumberFormat
import java.util.Locale

class ShopListAdapter(
    private val onItemClick: (ProductEntity) -> Unit,
    private val onAddToCartClick: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ShopListAdapter.ViewHolder>() {

    private val items = mutableListOf<ProductEntity>()

    fun submitList(newItems: List<ProductEntity>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ShopProductCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductEntity) {
            binding.tvProductName.text = product.name
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvPrice.text = formatter.format(product.price)
            
            val originalPrice = (product.price * 1.2).toLong()
            binding.tvOriginalPrice.text = formatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            
            binding.ivProduct.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }
            
            // Set Hot/New badge
            if (product.isNew) {
                binding.tvBadgeNew.visibility = android.view.View.VISIBLE
                binding.tvBadgeNew.text = "Mới"
                binding.tvBadgeNew.setBackgroundResource(com.veganbeauty.app.R.drawable.bg_badge_new)
            } else if (product.price >= 500000 || product.category.contains("Combo", ignoreCase = true)) {
                binding.tvBadgeNew.visibility = android.view.View.VISIBLE
                binding.tvBadgeNew.text = "Hot"
                binding.tvBadgeNew.setBackgroundResource(com.veganbeauty.app.R.drawable.bg_badge_hot)
            } else {
                binding.tvBadgeNew.visibility = android.view.View.GONE
            }
            
            binding.root.setOnClickListener { onItemClick(product) }
            binding.btnAddToCart.setOnClickListener { onAddToCartClick(product) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopProductCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
