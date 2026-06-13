package com.veganbeauty.app.features.myskin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.StoreEntity

class SkinStoreAdapter(
    private val stores: List<StoreEntity>,
    private val onBookClick: (StoreEntity) -> Unit
) : RecyclerView.Adapter<SkinStoreAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val storeImage: ImageView = view.findViewById(R.id.skin_store_image)
        val storeName: TextView = view.findViewById(R.id.skin_store_name)
        val storeAddress: TextView = view.findViewById(R.id.skin_store_address)
        val storeHours: TextView = view.findViewById(R.id.skin_store_hours)
        val btnBook: TextView = view.findViewById(R.id.skin_store_btn_book)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item_store_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.storeName.text = store.storeName
        holder.storeAddress.text = store.address
        holder.storeHours.text = store.openHours
        
        if (store.imageUrl.isNotEmpty()) {
            holder.storeImage.load(store.imageUrl) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        } else {
            holder.storeImage.setImageResource(R.drawable.imv_logo)
        }

        holder.btnBook.setOnClickListener {
            onBookClick(store)
        }
    }

    override fun getItemCount(): Int = stores.size
}
