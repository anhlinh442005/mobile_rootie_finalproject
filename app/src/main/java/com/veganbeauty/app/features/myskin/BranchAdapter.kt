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

class BranchAdapter(
    private var stores: List<StoreEntity>,
    private val onBranchSelected: (StoreEntity) -> Unit
) : RecyclerView.Adapter<BranchAdapter.ViewHolder>() {

    private var selectedStoreId: String? = null

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.store_branch_container)
        val image: ImageView = view.findViewById(R.id.store_branch_image)
        val name: TextView = view.findViewById(R.id.store_branch_name)
        val address: TextView = view.findViewById(R.id.store_branch_address)
        val distance: TextView = view.findViewById(R.id.store_branch_distance)
        val checkIcon: ImageView = view.findViewById(R.id.store_branch_check)
    }

    fun updateData(newStores: List<StoreEntity>) {
        stores = newStores
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item_store_branch, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        holder.name.text = store.tenCuaHang
        holder.address.text = store.diaChiDayDu
        
        val randomSeed = store.id.hashCode()
        val mockDistance = 1.0 + (Math.abs(randomSeed) % 140) / 10.0
        holder.distance.text = String.format("%.1fkm", mockDistance)
        
        val imageUrl = ""
        if (imageUrl.isNotEmpty()) {
            holder.image.load(imageUrl) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        } else {
            holder.image.setImageResource(R.drawable.imv_logo)
        }

        val isSelected = store.id == selectedStoreId
        holder.checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
        
        // Optional: Change background or stroke if selected
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_store_card) // Or a selected variant if available
        } else {
            holder.container.setBackgroundResource(R.drawable.skin_bg_store_card)
        }

        holder.container.setOnClickListener {
            val oldSelectedId = selectedStoreId
            selectedStoreId = store.id
            
            // Re-bind to update check marks
            notifyDataSetChanged()
            
            onBranchSelected(store)
        }
    }

    override fun getItemCount(): Int = stores.size
}
