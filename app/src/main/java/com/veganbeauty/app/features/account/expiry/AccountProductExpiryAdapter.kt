package com.veganbeauty.app.features.account.expiry

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R

class AccountProductExpiryAdapter(
    private val layoutMode: ExpiryLayoutMode,
    private val onItemLongClick: ((ExpiryProductUiModel) -> Unit)? = null,
    private val onItemClick: (ExpiryProductUiModel) -> Unit
) : ListAdapter<ExpiryProductUiModel, AccountProductExpiryAdapter.ViewHolder>(DiffCallback) {

    enum class ExpiryLayoutMode {
        GRID,
        HORIZONTAL,
        LIST
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = when (layoutMode) {
            ExpiryLayoutMode.GRID -> R.layout.account_product_expiry_item_grid
            ExpiryLayoutMode.HORIZONTAL -> R.layout.account_product_expiry_item_horizontal
            ExpiryLayoutMode.LIST -> R.layout.account_product_expiry_item_list
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ViewHolder(view, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        itemView: View,
        private val onItemClick: (ExpiryProductUiModel) -> Unit,
        private val onItemLongClick: ((ExpiryProductUiModel) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivProductImage: ImageView = itemView.findViewById(R.id.ivProductImage)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvExpiryDuration: TextView = itemView.findViewById(R.id.tvExpiryDuration)
        private val pbExpiry: ProgressBar = itemView.findViewById(R.id.pbExpiry)

        fun bind(uiModel: ExpiryProductUiModel) {
            tvProductName.text = uiModel.product.name
            tvExpiryDuration.text = uiModel.durationText

            // Load product image using Coil
            ivProductImage.load(uiModel.product.mainImage) {
                crossfade(true)
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }

            // Expiry progress
            pbExpiry.progress = uiModel.progressPercent

            // Dynamic progress tint & text color based on status (Expired/Urgent/Normal)
            if (uiModel.remainingDays <= 0) {
                tvExpiryDuration.setTextColor(Color.parseColor("#8E8E8E")) // Grey
                pbExpiry.progressTintList = ColorStateList.valueOf(Color.parseColor("#8E8E8E"))
            } else if (uiModel.isUrgent) {
                tvExpiryDuration.setTextColor(Color.parseColor("#C62828")) // Red
                pbExpiry.progressTintList = ColorStateList.valueOf(Color.parseColor("#C62828"))
            } else {
                tvExpiryDuration.setTextColor(Color.parseColor("#677559")) // Green
                pbExpiry.progressTintList = ColorStateList.valueOf(Color.parseColor("#677559"))
            }

            itemView.setOnClickListener { onItemClick(uiModel) }
            itemView.setOnLongClickListener {
                onItemLongClick?.invoke(uiModel)
                true
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ExpiryProductUiModel>() {
        override fun areItemsTheSame(oldItem: ExpiryProductUiModel, newItem: ExpiryProductUiModel): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: ExpiryProductUiModel, newItem: ExpiryProductUiModel): Boolean {
            return oldItem == newItem
        }
    }
}
