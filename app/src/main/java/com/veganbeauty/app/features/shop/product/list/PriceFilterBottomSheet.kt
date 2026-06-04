package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R
import com.veganbeauty.app.databinding.ShopBottomSheetPriceFilterBinding
import com.veganbeauty.app.features.shop.ShopViewModel

class PriceFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetPriceFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopViewModel
    private var selectedPriceRange: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetPriceFilterBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireParentFragment())[ShopViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentFilters()
        setupPriceChips()
        setupButtons()
    }

    private fun loadCurrentFilters() {
        selectedPriceRange = viewModel.currentPriceRange
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
                selectedPriceRange = if (selectedPriceRange == value) {
                    null
                } else {
                    value
                }
                updateChipsUi()
            }
        }

        updateChipsUi()
    }

    private fun setupButtons() {
        // Reset button
        binding.btnReset.setOnClickListener {
            selectedPriceRange = null
            setupPriceChips()
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            val currentSkinTypes = viewModel.currentSkinTypes
            val currentBenefits = viewModel.currentBenefits
            val currentIngredients = viewModel.currentIngredients

            viewModel.setAdvancedFilters(
                currentSkinTypes,
                selectedPriceRange,
                currentBenefits,
                currentIngredients
            )
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PriceFilterBottomSheet"
    }
}
