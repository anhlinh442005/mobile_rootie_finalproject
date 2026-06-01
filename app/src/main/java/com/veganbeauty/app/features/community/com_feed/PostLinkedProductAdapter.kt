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
    private var products: List<CommunityProduct> = emptyList()
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
    }

    override fun getItemCount(): Int = products.size
}
