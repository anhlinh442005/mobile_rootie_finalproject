package com.veganbeauty.app.features.community.notification

import android.content.res.ColorStateList
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ComItemNotificationBinding
import com.veganbeauty.app.databinding.ComItemNotificationHeaderBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_ITEM = 1

class ComNotificationAdapter(
    private val onItemClick: (ComNotificationItem) -> Unit,
    private val onDeleteClick: (ComNotificationItem) -> Unit
) : ListAdapter<ComNotificationListItem, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ComNotificationListItem.Header -> VIEW_TYPE_HEADER
            is ComNotificationListItem.Notification -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = ComItemNotificationHeaderBinding.inflate(inflater, parent, false)
                HeaderViewHolder(binding)
            }
            VIEW_TYPE_ITEM -> {
                val binding = ComItemNotificationBinding.inflate(inflater, parent, false)
                NotificationViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ComNotificationListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ComNotificationListItem.Notification -> (holder as NotificationViewHolder).bind(item.item)
        }
    }

    inner class HeaderViewHolder(
        private val binding: ComItemNotificationHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ComNotificationListItem.Header) {
            binding.tvHeaderTitle.text = header.title
        }
    }

    inner class NotificationViewHolder(
        private val binding: ComItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ComNotificationItem) {
            val context = binding.root.context

            // Format username in bold
            val htmlContent = "<b>${item.userName}</b> ${item.content}"
            binding.tvContent.text = HtmlCompat.fromHtml(htmlContent, HtmlCompat.FROM_HTML_MODE_LEGACY)
            binding.tvTime.text = "${item.time} • ${item.date}"

            // Read/unread state
            binding.viewUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

            // Avatar cropped to circular
            binding.ivAvatar.load(item.userAvatar?.takeIf { it.isNotEmpty() } ?: R.drawable.img_avatar) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.img_avatar)
                error(R.drawable.img_avatar)
            }

            // Category type badge setup (Instagram/Facebook style badge)
            val badgeRes = when (item.actionType) {
                "COMMENT" -> R.drawable.ic_chat
                "REPLY" -> R.drawable.ic_chat
                "LIKE" -> R.drawable.ic_heart
                "SHARE" -> R.drawable.ic_share
                "REPOST" -> R.drawable.ic_chat // Fallback to chat if ic_reup is png/broken, but list shows ic_reup is available
                "ORDER_PLACED" -> R.drawable.ic_truck
                "ORDER_COMPLETED" -> R.drawable.ic_check_circle
                "WITHDRAW" -> R.drawable.ic_cash
                else -> R.drawable.ic_bell
            }

            val badgeColor = when (item.actionType) {
                "COMMENT", "REPLY" -> ContextCompat.getColor(context, R.color.status_processing_text)
                "LIKE" -> ContextCompat.getColor(context, R.color.status_pending_text)
                "SHARE", "REPOST" -> ContextCompat.getColor(context, R.color.primary)
                "ORDER_PLACED" -> ContextCompat.getColor(context, R.color.status_delivering_text)
                "ORDER_COMPLETED", "WITHDRAW" -> ContextCompat.getColor(context, R.color.status_success_text)
                else -> ContextCompat.getColor(context, R.color.primary)
            }

            val badgeBgColor = when (item.actionType) {
                "COMMENT", "REPLY" -> ContextCompat.getColor(context, R.color.status_processing_bg)
                "LIKE" -> ContextCompat.getColor(context, R.color.status_pending_bg)
                "SHARE", "REPOST" -> ContextCompat.getColor(context, R.color.gray_light)
                "ORDER_PLACED" -> ContextCompat.getColor(context, R.color.status_delivering_bg)
                "ORDER_COMPLETED", "WITHDRAW" -> ContextCompat.getColor(context, R.color.status_success_bg)
                else -> ContextCompat.getColor(context, R.color.gray_light)
            }

            binding.ivTypeBadge.setImageResource(badgeRes)
            binding.ivTypeBadge.setColorFilter(badgeColor)
            binding.ivTypeBadge.backgroundTintList = ColorStateList.valueOf(badgeBgColor)

            binding.cardNotification.setOnClickListener {
                onItemClick(item)
            }

            binding.btnDeleteNotification.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    private companion object DiffCallback : DiffUtil.ItemCallback<ComNotificationListItem>() {
        override fun areItemsTheSame(oldItem: ComNotificationListItem, newItem: ComNotificationListItem): Boolean {
            return when {
                oldItem is ComNotificationListItem.Header && newItem is ComNotificationListItem.Header ->
                    oldItem.title == newItem.title
                oldItem is ComNotificationListItem.Notification && newItem is ComNotificationListItem.Notification ->
                    oldItem.item.id == newItem.item.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ComNotificationListItem, newItem: ComNotificationListItem): Boolean {
            return oldItem == newItem
        }
    }
}
