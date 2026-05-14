package com.veganbeauty.app.features.shop.product.list

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ShopListAdapter(
    private val onItemClick: (ProductEntity) -> Unit = {}
) : ListAdapter<ProductEntity, ShopListAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ShopItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ProductViewHolder(private val binding: ShopItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: ProductEntity, onItemClick: (ProductEntity) -> Unit) {
            binding.root.setOnClickListener { onItemClick(product) }
            binding.tvProductName.text = product.name
            
            // Định dạng tiền Việt Nam
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvPrice.text = formatter.format(product.price)
            
            // Load ảnh bằng Coil
            binding.ivProduct.load(product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
            }

            // Hiển thị badge "Mới"
            binding.tvBadgeNew.visibility = if (product.isNew) View.VISIBLE else View.GONE
            
            // Xử lý giá gốc (nếu có giảm giá)
            // Tạm thời giả định giá gốc cao hơn 20% để test UI
            val originalPrice = (product.price * 1.2).toLong()
            binding.tvOriginalPrice.text = formatter.format(originalPrice)
            binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity) = oldItem == newItem
    }
}
