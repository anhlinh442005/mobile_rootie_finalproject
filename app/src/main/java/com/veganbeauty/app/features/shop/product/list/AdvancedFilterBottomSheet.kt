package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ShopBottomSheetAdvancedFilterBinding
import com.veganbeauty.app.features.shop.ShopViewModel

class AdvancedFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetAdvancedFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopViewModel

    // State holders
    private val selectedSkinTypes = mutableSetOf<String>()
    private var selectedPriceRange: String? = null
    private val selectedBenefits = mutableSetOf<String>()
    private val selectedIngredients = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetAdvancedFilterBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireParentFragment())[ShopViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadCurrentFilters()
        setupExpandCollapse()
        setupPriceChips()
        setupCheckboxes()
        setupFooterButtons()
        renderFilterTags()
    }

    private fun loadCurrentFilters() {
        // Load current filters from VM
        selectedSkinTypes.addAll(viewModel.currentSkinTypes)
        selectedPriceRange = viewModel.currentPriceRange
        selectedBenefits.addAll(viewModel.currentBenefits)
        selectedIngredients.addAll(viewModel.currentIngredients)
    }

    private fun setupExpandCollapse() {
        // Collapsible: Skin Type
        binding.headerSkinType.setOnClickListener {
            val isVisible = binding.layoutSkinTypeOptions.visibility == View.VISIBLE
            binding.layoutSkinTypeOptions.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivChevronSkinType.rotation = if (isVisible) 0f else 90f
        }

        // Collapsible: Price Range
        binding.headerPrice.setOnClickListener {
            val isVisible = binding.layoutPriceOptions.visibility == View.VISIBLE
            binding.layoutPriceOptions.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivChevronPrice.rotation = if (isVisible) 0f else 90f
        }

        // Collapsible: Usages
        binding.headerUsage.setOnClickListener {
            val isVisible = binding.layoutUsageOptions.visibility == View.VISIBLE
            binding.layoutUsageOptions.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivChevronUsage.rotation = if (isVisible) 0f else 90f
        }

        // Collapsible: Ingredients
        binding.headerIngredients.setOnClickListener {
            val isVisible = binding.layoutIngredientsOptions.visibility == View.VISIBLE
            binding.layoutIngredientsOptions.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.ivChevronIngredients.rotation = if (isVisible) 0f else 90f
        }
    }

    private fun setupPriceChips() {
        val chips = listOf(
            binding.chipPrice1 to "Dưới 100.000đ",
            binding.chipPrice2 to "100.000đ - 300.000đ",
            binding.chipPrice3 to "300.000đ - 500.000đ",
            binding.chipPrice4 to "Trên 500.000đ"
        )

        fun updateChipsUi() {
            val regTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular)
            val medTypeface = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium)

            for ((chip, value) in chips) {
                if (selectedPriceRange == value) {
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                    chip.setTextColor(android.graphics.Color.parseColor("#3E4D44"))
                    chip.typeface = medTypeface
                } else {
                    chip.setBackgroundResource(R.drawable.bg_chip_normal)
                    chip.setTextColor(android.graphics.Color.parseColor("#555555"))
                    chip.typeface = regTypeface
                }
            }
        }

        for ((chip, value) in chips) {
            chip.setOnClickListener {
                if (selectedPriceRange == value) {
                    selectedPriceRange = null
                } else {
                    selectedPriceRange = value
                }
                updateChipsUi()
                renderFilterTags()
            }
        }

        updateChipsUi()
    }

    private fun setupCheckboxes() {
        // Skin Types checkboxes map
        val skinCheckboxes = mapOf(
            binding.cbSkinNormal to "Da thường",
            binding.cbSkinDry to "Da khô",
            binding.cbSkinOily to "Da dầu",
            binding.cbSkinSensitive to "Da nhạy cảm",
            binding.cbSkinCombination to "Da hỗn hợp"
        )

        for ((cb, value) in skinCheckboxes) {
            cb.isChecked = selectedSkinTypes.contains(value)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedSkinTypes.add(value) else selectedSkinTypes.remove(value)
                renderFilterTags()
            }
        }

        // Usages checkboxes map
        val usageCheckboxes = mapOf(
            binding.cbUsageClean to "Làm sạch",
            binding.cbUsageMoisturize to "Dưỡng ẩm"
        )

        for ((cb, value) in usageCheckboxes) {
            cb.isChecked = selectedBenefits.contains(value)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedBenefits.add(value) else selectedBenefits.remove(value)
                renderFilterTags()
            }
        }

        // Ingredients checkboxes map
        val ingredientCheckboxes = mapOf(
            binding.cbIngredientVegan to "Chay",
            binding.cbIngredientNatural to "Tự nhiên"
        )

        for ((cb, value) in ingredientCheckboxes) {
            cb.isChecked = selectedIngredients.contains(value)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedIngredients.add(value) else selectedIngredients.remove(value)
                renderFilterTags()
            }
        }
    }

    private fun renderFilterTags() {
        binding.layoutFilterTags.removeAllViews()

        val allActiveFilters = mutableListOf<Pair<String, () -> Unit>>()

        // Add skin type tags
        for (skinType in selectedSkinTypes) {
            allActiveFilters.add(skinType to {
                selectedSkinTypes.remove(skinType)
                refreshCheckboxes()
                renderFilterTags()
            })
        }

        // Add price tag
        selectedPriceRange?.let { price ->
            allActiveFilters.add(price to {
                selectedPriceRange = null
                setupPriceChips()
                renderFilterTags()
            })
        }

        // Add usage tags
        for (benefit in selectedBenefits) {
            allActiveFilters.add(benefit to {
                selectedBenefits.remove(benefit)
                refreshCheckboxes()
                renderFilterTags()
            })
        }

        // Add ingredient tags
        for (ingredient in selectedIngredients) {
            allActiveFilters.add(ingredient to {
                selectedIngredients.remove(ingredient)
                refreshCheckboxes()
                renderFilterTags()
            })
        }

        // Update count
        binding.tvActiveFiltersCount.text = "Lọc theo (${allActiveFilters.size})"

        // Populate tags layout
        val inflater = LayoutInflater.from(requireContext())
        for ((name, removeAction) in allActiveFilters) {
            val tagView = inflater.inflate(R.layout.shop_item_filter_tag, binding.layoutFilterTags, false)
            val tvName = tagView.findViewById<TextView>(R.id.tvTagName)
            val ivRemove = tagView.findViewById<View>(R.id.ivRemoveTag)

            tvName.text = name
            ivRemove.setOnClickListener { removeAction() }
            binding.layoutFilterTags.addView(tagView)
        }
    }

    private fun refreshCheckboxes() {
        binding.cbSkinNormal.isChecked = selectedSkinTypes.contains("Da thường")
        binding.cbSkinDry.isChecked = selectedSkinTypes.contains("Da khô")
        binding.cbSkinOily.isChecked = selectedSkinTypes.contains("Da dầu")
        binding.cbSkinSensitive.isChecked = selectedSkinTypes.contains("Da nhạy cảm")
        binding.cbSkinCombination.isChecked = selectedSkinTypes.contains("Da hỗn hợp")

        binding.cbUsageClean.isChecked = selectedBenefits.contains("Làm sạch")
        binding.cbUsageMoisturize.isChecked = selectedBenefits.contains("Dưỡng ẩm")

        binding.cbIngredientVegan.isChecked = selectedIngredients.contains("Chay")
        binding.cbIngredientNatural.isChecked = selectedIngredients.contains("Tự nhiên")
    }

    private fun setupFooterButtons() {
        // Reset button
        binding.btnReset.setOnClickListener {
            selectedSkinTypes.clear()
            selectedPriceRange = null
            selectedBenefits.clear()
            selectedIngredients.clear()
            
            refreshCheckboxes()
            setupPriceChips()
            renderFilterTags()
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            viewModel.setAdvancedFilters(
                selectedSkinTypes.toSet(),
                selectedPriceRange,
                selectedBenefits.toSet(),
                selectedIngredients.toSet()
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AdvancedFilterBottomSheet"
    }
}
