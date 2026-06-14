package com.veganbeauty.app.features.community.beauty_hub

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.IngredientEntity

class IngredientFragment : Fragment() {

    private lateinit var rvIngredients: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var llFilterChips: LinearLayout
    private lateinit var tvResultCount: TextView

    private var allIngredients: List<IngredientEntity> = emptyList()
    private var selectedType: String? = null
    private var searchQuery: String = ""

    private val allTypes = listOf("Tất cả", "Làm dịu", "Phục hồi", "Sạch sâu", "Da dầu mụn")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.com_fragment_ingredient, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvIngredients = view.findViewById(R.id.rvIngredients)
        etSearch = view.findViewById(R.id.etSearch)
        llFilterChips = view.findViewById(R.id.llFilterChips)
        tvResultCount = view.findViewById(R.id.tvResultCount)

        view.findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Load data
        allIngredients = LocalJsonReader(requireContext()).getIngredients()

        // Setup filter chips
        setupFilterChips()

        // Setup adapter with GridLayoutManager (3 columns)
        val adapter = IngredientFullAdapter(allIngredients) { ingredient ->
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, IngredientDetailFragment.newInstance(ingredient.slug))
                .addToBackStack(null)
                .commit()
        }
        rvIngredients.layoutManager = GridLayoutManager(requireContext(), 2)
        rvIngredients.adapter = adapter

        updateResultCount(allIngredients.size)

        // Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString() ?: ""
                filterAndUpdate(adapter)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilterChips() {
        llFilterChips.removeAllViews()
        val dp = resources.displayMetrics.density.toInt()

        allTypes.forEach { type ->
            val chip = TextView(requireContext()).apply {
                text = type
                textSize = 12f
                setPadding(12 * dp, 6 * dp, 12 * dp, 6 * dp)
                val margin = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 * dp }
                layoutParams = margin
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    selectedType = if (type == "Tất cả") null else type
                    refreshChipStates()
                    filterAndUpdate(rvIngredients.adapter as IngredientFullAdapter)
                }
            }
            llFilterChips.addView(chip)
        }
        refreshChipStates()
    }

    private fun refreshChipStates() {
        for (i in 0 until llFilterChips.childCount) {
            val chip = llFilterChips.getChildAt(i) as TextView
            val chipType = chip.text.toString()
            val isSelected = (chipType == "Tất cả" && selectedType == null) || chipType == selectedType

            if (isSelected) {
                chip.setBackgroundResource(R.drawable.com_bg_chip_selected)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setBackgroundResource(R.drawable.com_bg_chip_default)
                chip.setTextColor(android.graphics.Color.parseColor("#888888"))
            }
        }
    }

    private fun filterAndUpdate(adapter: IngredientFullAdapter) {
        var filtered = allIngredients
        if (selectedType != null) {
            filtered = filtered.filter { it.types.contains(selectedType) }
        }
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.scientificName.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        adapter.updateData(filtered)
        updateResultCount(filtered.size)
    }

    private fun updateResultCount(count: Int) {
        tvResultCount.text = "$count thành phần"
    }
}

class IngredientFullAdapter(
    private var items: List<IngredientEntity>,
    private val onClick: (IngredientEntity) -> Unit
) : RecyclerView.Adapter<IngredientFullAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIngredient: ImageView = view.findViewById(R.id.ivIngredient)
        val tvName: TextView = view.findViewById(R.id.tvIngredientName)
        val tvScientific: TextView = view.findViewById(R.id.tvScientificName)
        val tvDesc: TextView = view.findViewById(R.id.tvIngredientDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.com_item_ingredient_full, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name
        holder.tvScientific.text = item.scientificName
        holder.tvDesc.text = item.description

        if (item.image.isNotEmpty()) {
            holder.ivIngredient.load(item.image) {
                crossfade(true)
                error(R.drawable.img_placeholder)
            }
        } else {
            holder.ivIngredient.setImageResource(R.drawable.img_placeholder)
        }
        
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<IngredientEntity>) {
        items = newItems
        notifyDataSetChanged()
    }
}
