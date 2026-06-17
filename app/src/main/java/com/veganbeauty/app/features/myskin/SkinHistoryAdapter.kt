package com.veganbeauty.app.features.myskin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.databinding.ItemSkinHistoryBinding
import org.json.JSONArray
import org.json.JSONObject

class SkinHistoryAdapter(
    private var data: JSONArray,
    private val onItemClick: (JSONObject) -> Unit
) : RecyclerView.Adapter<SkinHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemSkinHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: JSONObject) {
            binding.itemSkinHistoryDate.text = item.optString("date", "")
            binding.itemSkinHistoryTime.text = item.optString("time", "")
            binding.itemSkinHistoryType.text = item.optString("scanType", "Quét AI")
            binding.itemSkinHistoryScore.text = item.optInt("score", 0).toString()

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSkinHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data.getJSONObject(position)
        holder.bind(item)
        
        // Hide divider for the last item
        if (position == data.length() - 1) {
            holder.binding.itemSkinHistoryDivider.visibility = android.view.View.GONE
        } else {
            holder.binding.itemSkinHistoryDivider.visibility = android.view.View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return data.length()
    }

    fun updateData(newData: JSONArray) {
        data = newData
        notifyDataSetChanged()
    }
}
