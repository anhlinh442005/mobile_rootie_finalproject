package com.veganbeauty.app.features.account.expiry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.AccountBottomSheetExpiryFilterBinding

class ExpiryFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AccountBottomSheetExpiryFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AccountProductExpiryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountBottomSheetExpiryFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val parent = parentFragment ?: return
        viewModel = ViewModelProvider(parent)[AccountProductExpiryViewModel::class.java]

        // Observe filter state from ViewModel to update UI indicators
        viewModel.selectedFilter.observe(viewLifecycleOwner) { selectedState ->
            binding.ivCheckAll.visibility = if (selectedState == ExpiryFilterState.ALL) View.VISIBLE else View.GONE
            binding.ivCheckExpired.visibility = if (selectedState == ExpiryFilterState.EXPIRED) View.VISIBLE else View.GONE
            binding.ivCheckSoon.visibility = if (selectedState == ExpiryFilterState.SOON) View.VISIBLE else View.GONE
            binding.ivCheckValid.visibility = if (selectedState == ExpiryFilterState.VALID) View.VISIBLE else View.GONE
        }

        // Handle item clicks to set filter
        binding.btnFilterAll.setOnClickListener {
            viewModel.setSelectedFilter(ExpiryFilterState.ALL)
            dismiss()
        }

        binding.btnFilterExpired.setOnClickListener {
            viewModel.setSelectedFilter(ExpiryFilterState.EXPIRED)
            dismiss()
        }

        binding.btnFilterSoon.setOnClickListener {
            viewModel.setSelectedFilter(ExpiryFilterState.SOON)
            dismiss()
        }

        binding.btnFilterValid.setOnClickListener {
            viewModel.setSelectedFilter(ExpiryFilterState.VALID)
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ExpiryFilterBottomSheet"

        fun newInstance(): ExpiryFilterBottomSheet {
            return ExpiryFilterBottomSheet()
        }
    }
}
