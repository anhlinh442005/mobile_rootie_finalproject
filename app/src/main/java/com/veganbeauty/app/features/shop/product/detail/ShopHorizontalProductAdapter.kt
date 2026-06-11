package com.veganbeauty.app.features.shop.product.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopProductCardBinding
import java.text.NumberFormat
import java.util.Locale

class ShopHorizontalProductAdapter(
    private var items: List<ProductEntity>,
    private val onItemClick: (ProductEntity) -> Unit,
    private val onAddToCartClick: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ShopHorizontalProductAdapter.ViewHolder>() {

    fun updateData(newItems: List<ProductEntity>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ShopProductCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        // Set fixed width for horizontal layout
        val density = parent.resources.displayMetrics.density
        val widthPx = (160 * density).toInt()
        val layoutParams = binding.root.layoutParams
        if (layoutParams != null) {
            layoutParams.width = widthPx
            binding.root.layoutParams = layoutParams
        } else {
            binding.root.layoutParams = ViewGroup.LayoutParams(widthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick, onAddToCartClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ShopProductCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            product: ProductEntity,
            onItemClick: (ProductEntity) -> Unit,
            onAddToCartClick: (ProductEntity) -> Unit
        ) {
            binding.tvProductName.text = product.name
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvPrice.text = formatter.format(product.price)
            
            val originalPrice = (product.price * 1.2).toLong()
            binding.tvOriginalPrice.text = formatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            if (product.isNew) {
                binding.tvBadgeNew.visibility = View.VISIBLE
            } else {
                binding.tvBadgeNew.visibility = View.GONE
            }

            binding.ivProduct.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }

            binding.root.setOnClickListener { onItemClick(product) }
            binding.btnAddToCart.setOnClickListener { onAddToCartClick(product) }
        }
    }
}
