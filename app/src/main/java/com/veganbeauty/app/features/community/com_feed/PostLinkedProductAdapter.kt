package com.veganbeauty.app.features.community.com_feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.CommunityProduct

class PostLinkedProductAdapter(
    private var products: List<CommunityProduct> = emptyList(),
    private val postId: String = "",
    private val authorId: String = ""
) : RecyclerView.Adapter<PostLinkedProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductImage: ImageView = view.findViewById(R.id.ivProductImage)
        val tvProductName: TextView = view.findViewById(R.id.tvProductName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_post_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.tvProductName.text = product.name
        
        holder.ivProductImage.load(product.mainImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(R.drawable.logo)
        }
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            
            // Generate Affiliate tracking info
            val affiliateCode = "AFF_${authorId.uppercase()}_${product.id.uppercase()}"
            val trackingJson = org.json.JSONObject().apply {
                put("referrerUserId", authorId)
                put("sourcePostId", postId)
                put("affiliateCode", affiliateCode)
                put("timestamp", System.currentTimeMillis())
            }.toString()
            
            // Save to SharedPreferences so checkout can pick it up
            val prefs = context.getSharedPreferences("affiliate_prefs", android.content.Context.MODE_PRIVATE)
            prefs.edit().putString(product.id, trackingJson).apply()
            
            // Navigate to Product Detail Page
            if (context is androidx.fragment.app.FragmentActivity) {
                // Convert CommunityProduct to ProductEntity
                val productEntity = com.veganbeauty.app.data.local.entities.ProductEntity(
                    id = product.id,
                    name = product.name,
                    sku = product.id,
                    price = product.price.toLong(),
                    category = "Affiliate",
                    stock = 100,
                    mainImage = product.mainImage
                )
                
                val detailFragment = com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment().apply {
                    setProduct(productEntity)
                }
                
                context.supportFragmentManager.beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun getItemCount(): Int = products.size
}
