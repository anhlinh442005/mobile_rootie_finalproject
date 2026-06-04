package com.veganbeauty.app.features.shop.product.list

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R

class SubcategoryAdapter(
    private val onSubcategoryClick: (String) -> Unit
) : ListAdapter<String, SubcategoryAdapter.SubcategoryViewHolder>(SubcategoryDiffCallback()) {

    var selectedSubcategory: String = "Tất cả"
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubcategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.shop_category_subcategory, parent, false)
        return SubcategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubcategoryViewHolder, position: Int) {
        val subcategory = getItem(position)
        holder.bind(subcategory, subcategory == selectedSubcategory, onSubcategoryClick)
    }

    class SubcategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvChip: TextView = itemView.findViewById(R.id.tvChip)

        fun bind(subcategory: String, isSelected: Boolean, onSubcategoryClick: (String) -> Unit) {
            tvChip.text = subcategory
            
            if (isSelected) {
                tvChip.setBackgroundResource(R.drawable.bg_chip_selected)
                tvChip.setTextColor(Color.parseColor("#3D5A40"))
            } else {
                tvChip.setBackgroundResource(R.drawable.bg_chip_normal)
                tvChip.setTextColor(Color.parseColor("#555555"))
            }

            itemView.setOnClickListener {
                onSubcategoryClick(subcategory)
            }
        }
    }

    class SubcategoryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
    }
}
