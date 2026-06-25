package com.veganbeauty.app.features.myskin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R

// --- Data Classes ---
data class BookingService(val id: String, val name: String, val description: String, val duration: String)
data class BookingDate(val id: String, val dayOfWeek: String, val date: String, val fullDate: java.util.Calendar? = null)
data class BookingTime(val id: String, val time: String, val isLocked: Boolean = false)

// --- BookingServiceAdapter ---
class BookingServiceAdapter(
    private val items: List<BookingService>,
    private val onItemSelected: (BookingService) -> Unit
) : RecyclerView.Adapter<BookingServiceAdapter.ViewHolder>() {

    private var selectedIndex = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.container_service)
        val tvName: TextView = view.findViewById(R.id.tv_service_name)
        val tvDesc: TextView = view.findViewById(R.id.tv_service_desc)
        val ivCheck: ImageView = view.findViewById(R.id.iv_service_check)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item_booking_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvDesc.text = item.description

        val isSelected = position == selectedIndex
        holder.ivCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
        
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item)
        } else {
            holder.container.setBackgroundResource(R.drawable.skin_bg_outline)
        }

        holder.container.setOnClickListener {
            val oldIndex = selectedIndex
            selectedIndex = position
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedIndex)
            onItemSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

// --- BookingDateAdapter ---
class BookingDateAdapter(
    private val items: List<BookingDate>,
    private val onItemSelected: (BookingDate) -> Unit
) : RecyclerView.Adapter<BookingDateAdapter.ViewHolder>() {

    private var selectedIndex = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.container_date)
        val tvDay: TextView = view.findViewById(R.id.tv_date_day)
        val tvDate: TextView = view.findViewById(R.id.tv_date_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item_booking_date, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvDay.text = item.dayOfWeek
        holder.tvDate.text = item.date

        val isSelected = position == selectedIndex
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item)
        } else {
            holder.container.setBackgroundResource(R.drawable.skin_bg_outline)
        }

        holder.container.setOnClickListener {
            val oldIndex = selectedIndex
            selectedIndex = position
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedIndex)
            onItemSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(selectedIndex)
    }
}

// --- BookingTimeAdapter ---
class BookingTimeAdapter(
    private var items: List<BookingTime>,
    private val onItemSelected: (BookingTime) -> Unit
) : RecyclerView.Adapter<BookingTimeAdapter.ViewHolder>() {

    private var selectedIndex = -1

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: View = view.findViewById(R.id.container_time)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.skin_item_booking_time, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTime.text = item.time

        val isSelected = position == selectedIndex
        
        if (item.isLocked) {
            holder.container.setBackgroundResource(R.drawable.skin_bg_outline)
            holder.container.alpha = 0.4f
            holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.content))
        } else {
            holder.container.alpha = 1.0f
            if (isSelected) {
                holder.container.setBackgroundResource(R.drawable.skin_bg_selected_item)
                holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.primary))
            } else {
                holder.container.setBackgroundResource(R.drawable.skin_bg_outline)
                holder.tvTime.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.content))
            }
        }

        holder.container.setOnClickListener {
            if (item.isLocked) return@setOnClickListener
            
            val oldIndex = selectedIndex
            selectedIndex = position
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectedIndex)
            onItemSelected(item)
        }
    }

    override fun getItemCount(): Int = items.size
    
    fun updateData(newItems: List<BookingTime>) {
        items = newItems
        selectedIndex = -1
        notifyDataSetChanged()
    }
}
