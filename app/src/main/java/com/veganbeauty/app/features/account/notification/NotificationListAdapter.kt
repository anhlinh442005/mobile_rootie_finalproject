package com.veganbeauty.app.features.account.notification

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.NotificationItem
import com.veganbeauty.app.databinding.AccountNotificationBannerBinding
import com.veganbeauty.app.databinding.AccountNotificationGridCardsBinding
import com.veganbeauty.app.databinding.AccountNotificationHeaderBinding
import com.veganbeauty.app.databinding.AccountNotificationItemBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_ITEM = 1
private const val VIEW_TYPE_BANNER = 2
private const val VIEW_TYPE_GRID = 3

class NotificationListAdapter(
    private val onItemClick: (NotificationItem) -> Unit,
    private val onActionClick: (NotificationItem) -> Unit,
    private val onBannerClick: () -> Unit,
    private val onLeftGridClick: () -> Unit,
    private val onRightGridClick: () -> Unit
) : ListAdapter<NotificationListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationListItem.Header -> VIEW_TYPE_HEADER
            is NotificationListItem.Notification -> VIEW_TYPE_ITEM
            is NotificationListItem.PromoBanner -> VIEW_TYPE_BANNER
            is NotificationListItem.GridStats -> VIEW_TYPE_GRID
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = AccountNotificationHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_ITEM -> {
                val binding = AccountNotificationItemBinding.inflate(inflater, parent, false)
                NotificationViewHolder(binding)
            }
            VIEW_TYPE_BANNER -> {
                val binding = AccountNotificationBannerBinding.inflate(inflater, parent, false)
                PromoBannerViewHolder(binding)
            }
            VIEW_TYPE_GRID -> {
                val binding = AccountNotificationGridCardsBinding.inflate(inflater, parent, false)
                GridStatsViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is NotificationListItem.Notification -> (holder as NotificationViewHolder).bind(item.item)
            is NotificationListItem.PromoBanner -> (holder as PromoBannerViewHolder).bind()
            is NotificationListItem.GridStats -> (holder as GridStatsViewHolder).bind()
        }
    }

    inner class HeaderViewHolder(
        private val binding: AccountNotificationHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: NotificationListItem.Header) {
            binding.tvHeaderTitle.text = header.title
        }
    }

    inner class NotificationViewHolder(
        private val binding: AccountNotificationItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: NotificationItem) {
            val context = binding.root.context
            
            // Set basic texts
            binding.tvTitle.text = item.title
            binding.tvContent.text = item.content
            binding.tvTime.text = item.time

            // Unread dot visibility
            binding.viewUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

            // Dynamically load category icon
            val resId = context.resources.getIdentifier(item.iconResName, "drawable", context.packageName)
            if (resId != 0) {
                binding.ivIcon.setImageResource(resId)
            } else {
                binding.ivIcon.setImageResource(R.drawable.ic_voucher)
            }

            // Beautiful dynamic category coloring for premium UI feel
            val (iconTint, bgTint) = when (item.category) {
                "Khuyến mãi" -> Pair(
                    ContextCompat.getColor(context, R.color.status_pending_text),
                    ContextCompat.getColor(context, R.color.status_pending_bg)
                )
                "Đơn hàng" -> Pair(
                    ContextCompat.getColor(context, R.color.status_delivering_text),
                    ContextCompat.getColor(context, R.color.status_delivering_bg)
                )
                "Tin nhắn" -> {
                    if (item.iconResName == "ic_ribbon") {
                        Pair(
                            ContextCompat.getColor(context, R.color.status_pending_text),
                            ContextCompat.getColor(context, R.color.status_pending_bg)
                        )
                    } else {
                        Pair(
                            ContextCompat.getColor(context, R.color.status_processing_text),
                            ContextCompat.getColor(context, R.color.status_processing_bg)
                        )
                    }
                }
                else -> Pair(
                    ContextCompat.getColor(context, R.color.primary),
                    ContextCompat.getColor(context, R.color.gray_light)
                )
            }
            binding.ivIcon.setColorFilter(iconTint)
            binding.ivIcon.backgroundTintList = ColorStateList.valueOf(bgTint)

            // Dynamic Action Button visibility & styling
            if (!item.actionText.isNullOrEmpty()) {
                binding.btnAction.visibility = View.VISIBLE
                binding.btnAction.text = item.actionText
                binding.btnAction.setOnClickListener {
                    onActionClick(item)
                }
            } else {
                binding.btnAction.visibility = View.GONE
            }

            // Card click listener
            binding.cardNotification.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    inner class PromoBannerViewHolder(
        private val binding: AccountNotificationBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener {
                onBannerClick()
            }
        }
    }

    inner class GridStatsViewHolder(
        private val binding: AccountNotificationGridCardsBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.cardLeft.setOnClickListener {
                onLeftGridClick()
            }
            binding.cardRight.setOnClickListener {
                onRightGridClick()
            }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<NotificationListItem>() {
        override fun areItemsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return when {
                oldItem is NotificationListItem.Header && newItem is NotificationListItem.Header -> 
                    oldItem.title == newItem.title
                oldItem is NotificationListItem.Notification && newItem is NotificationListItem.Notification -> 
                    oldItem.item.id == newItem.item.id
                oldItem is NotificationListItem.PromoBanner && newItem is NotificationListItem.PromoBanner -> true
                oldItem is NotificationListItem.GridStats && newItem is NotificationListItem.GridStats -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: NotificationListItem, newItem: NotificationListItem): Boolean {
            return oldItem == newItem
        }
    }
}
