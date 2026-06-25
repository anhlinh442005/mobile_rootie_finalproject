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
    private val onViewDetailClick: (BookingHistoryEntity) -> Unit,
    private val onCancelClick: (BookingHistoryEntity) -> Unit
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
            val (dayNum, monthDayStr) = BookingDateParser.parseDateDisplay(item.dateDisplay, item.monthDisplay, item.dayOfWeek)
            holder.tvDateNum.text = dayNum
            holder.tvMonthDay.text = monthDayStr
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
            when (item.status) {
                "Sắp diễn ra", "Chờ xác nhận", "pending" -> {
                    if (item.status.equals("Chờ xác nhận", ignoreCase = true) || item.status.equals("pending", ignoreCase = true)) {
                        holder.tvStatusTag.text = "Chờ xác nhận"
                        holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_pending)
                        holder.tvStatusTag.setTextColor(Color.parseColor("#E65100"))
                    } else {
                        holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_upcoming)
                        holder.tvStatusTag.setTextColor(Color.parseColor("#1976D2"))
                    }
                    holder.llActions.visibility = View.VISIBLE
                }
                "Đã hoàn thành" -> {
                    holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_completed)
                    holder.tvStatusTag.setTextColor(Color.parseColor("#2E7D32"))
                    holder.llActions.visibility = View.GONE
                }
                "Đã huỷ" -> {
                    holder.tvStatusTag.setBackgroundResource(R.drawable.skin_bg_badge_cancelled)
                    holder.tvStatusTag.setTextColor(Color.parseColor("#C62828"))
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
                onCancelClick(item)
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

object BookingDateParser {
    fun parseDateDisplay(dateDisplay: String, monthDisplay: String, dayOfWeek: String): Pair<String, String> {
        val dateStr = dateDisplay.trim()
        
        // Case 1: "dd/MM/yyyy"
        if (dateStr.contains("/")) {
            val parts = dateStr.split("/")
            if (parts.isNotEmpty()) {
                val day = parts[0]
                val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val monthStr = if (monthNum > 0) "Tháng $monthNum" else "Tháng --"
                val dayOfWeekShort = when (dayOfWeek.trim()) {
                    "Thứ 2" -> "T.2"
                    "Thứ 3" -> "T.3"
                    "Thứ 4" -> "T.4"
                    "Thứ 5" -> "T.5"
                    "Thứ 6" -> "T.6"
                    "Thứ 7" -> "T.7"
                    "Chủ Nhật", "Chủ nhật", "CN" -> "CN"
                    else -> dayOfWeek
                }
                return Pair(day, "$monthStr\n$dayOfWeekShort")
            }
        }
        
        // Case 2: "20 tháng 5, 2024"
        val regex = Regex("(\\d+)\\s+tháng\\s+(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(dateStr)
        if (match != null) {
            val day = match.groupValues[1]
            val month = match.groupValues[2]
            val dayOfWeekShort = when (dayOfWeek.trim()) {
                "Thứ 2" -> "T.2"
                "Thứ 3" -> "T.3"
                "Thứ 4" -> "T.4"
                "Thứ 5" -> "T.5"
                "Thứ 6" -> "T.6"
                "Thứ 7" -> "T.7"
                "Chủ Nhật", "Chủ nhật", "CN" -> "CN"
                else -> dayOfWeek
            }
            return Pair(day, "Tháng $month\n$dayOfWeekShort")
        }
        
        // Fallback
        return Pair(dateStr, monthDisplay.ifEmpty { dayOfWeek })
    }
}
