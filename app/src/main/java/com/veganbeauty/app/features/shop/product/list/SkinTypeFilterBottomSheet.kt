package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.ShopBottomSheetSkinTypeFilterBinding
import com.veganbeauty.app.features.shop.ShopViewModel

class SkinTypeFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetSkinTypeFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopViewModel
    private val selectedSkinTypes = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetSkinTypeFilterBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireParentFragment())[ShopViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCurrentFilters()
        setupCheckboxes()
        setupButtons()
    }

    private fun loadCurrentFilters() {
        selectedSkinTypes.addAll(viewModel.currentSkinTypes)
    }

    private fun setupCheckboxes() {
        val checkboxes = mapOf(
            binding.cbSkinNormal to "Da thường",
            binding.cbSkinDry to "Da khô",
            binding.cbSkinOily to "Da dầu",
            binding.cbSkinSensitive to "Da nhạy cảm",
            binding.cbSkinCombination to "Da hỗn hợp"
        )

        for ((cb, value) in checkboxes) {
            cb.isChecked = selectedSkinTypes.contains(value)
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedSkinTypes.add(value)
                } else {
                    selectedSkinTypes.remove(value)
                }
            }
        }
    }

    private fun setupButtons() {
        // Reset button
        binding.btnReset.setOnClickListener {
            selectedSkinTypes.clear()
            binding.cbSkinNormal.isChecked = false
            binding.cbSkinDry.isChecked = false
            binding.cbSkinOily.isChecked = false
            binding.cbSkinSensitive.isChecked = false
            binding.cbSkinCombination.isChecked = false
        }

        // Confirm button
        binding.btnConfirm.setOnClickListener {
            val currentPrice = viewModel.currentPriceRange
            val currentBenefits = viewModel.currentBenefits
            val currentIngredients = viewModel.currentIngredients

            viewModel.setAdvancedFilters(
                selectedSkinTypes.toSet(),
                currentPrice,
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
        const val TAG = "SkinTypeFilterBottomSheet"
    }
}
