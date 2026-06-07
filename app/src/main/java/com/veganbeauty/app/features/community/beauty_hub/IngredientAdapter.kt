package com.veganbeauty.app.features.community.beauty_hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.IngredientEntity

class IngredientAdapter(private var ingredients: List<IngredientEntity>) :
    RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIngredient: ImageView = view.findViewById(R.id.ivIngredient)
        val tvName: TextView = view.findViewById(R.id.tvIngredientName)
        val tvDesc: TextView = view.findViewById(R.id.tvIngredientDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = ingredients[position]
        holder.tvName.text = item.name
        holder.tvDesc.text = item.description
        if (item.image.isNotEmpty()) {
            holder.ivIngredient.load(item.image) {
                crossfade(true)
                placeholder(R.drawable.img_placeholder)
                error(R.drawable.img_placeholder)
            }
        } else {
            holder.ivIngredient.setImageResource(R.drawable.img_placeholder)
        }
    }

    override fun getItemCount() = ingredients.size

    fun updateData(newIngredients: List<IngredientEntity>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }
}
