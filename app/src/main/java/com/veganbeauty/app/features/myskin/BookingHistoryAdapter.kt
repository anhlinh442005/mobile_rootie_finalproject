package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity

class BookingHistoryAdapter(
    private var items: List<Any>,
    private val onViewDetailClick: (BookingHistoryEntity) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.skin_item_booking_history_header, parent, false))
        } else {
            ItemViewHolder(inflater.inflate(R.layout.skin_item_booking_history, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder && item is String) {
            holder.tvHeaderTitle.text = item
        } else if (holder is ItemViewHolder && item is BookingHistoryEntity) {
            holder.tvDateNum.text = item.dateDisplay
            holder.tvMonthDay.text = item.monthDisplay
            holder.tvServiceName.text = item.serviceName
            holder.tvTime.text = item.time
            
            // Highlight name store bold in HTML
            val parts = item.storeName.split("\n")
            if (parts.isNotEmpty()) {
                val boldName = "<b>${parts[0]}</b>"
                val rest = item.storeAddress
                holder.tvStore.text = Html.fromHtml("$boldName<br/>$rest", Html.FROM_HTML_MODE_COMPACT)
            } else {
                holder.tvStore.text = "${item.storeName}\n${item.storeAddress}"
            }

            holder.tvStatusTag.text = item.status

            // Style tag based on status
            val context = holder.itemView.context
            when (item.status) {
                "Sắp diễn ra" -> {
                    holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_upcoming)
                    holder.tvStatusTag.setTextColor(Color.WHITE)
                    holder.llActions.visibility = View.VISIBLE
                }
                "Đã hoàn thành" -> {
                    holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_completed)
                    holder.tvStatusTag.setTextColor(Color.WHITE)
                    holder.llActions.visibility = View.GONE
                }
                "Đã huỷ" -> {
                    holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_cancelled)
                    holder.tvStatusTag.setTextColor(Color.WHITE)
                    holder.llActions.visibility = View.GONE
                }
                else -> {
                    holder.tvStatusTag.setBackgroundColor(Color.GRAY)
                    holder.tvStatusTag.setTextColor(Color.WHITE)
                    holder.llActions.visibility = View.GONE
                }
            }

            // Button actions
            holder.btnViewDetail.setOnClickListener {
                onViewDetailClick(item)
            }
            holder.btnCancel.setOnClickListener {
                Toast.makeText(context, "Hủy lịch soi da", Toast.LENGTH_SHORT).show()
            }
            
            // Allow clicking entire card to view details
            holder.itemView.setOnClickListener {
                onViewDetailClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHeaderTitle: TextView = view.findViewById(R.id.tv_header_title)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDateNum: TextView = view.findViewById(R.id.tv_history_date_num)
        val tvMonthDay: TextView = view.findViewById(R.id.tv_history_month_day)
        val tvServiceName: TextView = view.findViewById(R.id.tv_history_service_name)
        val tvStatusTag: TextView = view.findViewById(R.id.tv_history_status_tag)
        val tvTime: TextView = view.findViewById(R.id.tv_history_time)
        val tvStore: TextView = view.findViewById(R.id.tv_history_store)
        val llActions: View = view.findViewById(R.id.ll_history_actions)
        val btnViewDetail: TextView = view.findViewById(R.id.btn_view_detail)
        val btnCancel: TextView = view.findViewById(R.id.btn_cancel_booking)
    }
}
