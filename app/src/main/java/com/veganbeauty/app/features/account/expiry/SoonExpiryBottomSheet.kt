package com.veganbeauty.app.features.account.expiry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.AccountBottomSheetSoonExpiryBinding

class SoonExpiryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AccountBottomSheetSoonExpiryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AccountProductExpiryViewModel

    private val listAdapter = AccountProductExpiryAdapter(AccountProductExpiryAdapter.ExpiryLayoutMode.LIST) { uiModel ->
        (parentFragment as? AccountProductExpiryFragment)?.navigateToDetail(uiModel)
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountBottomSheetSoonExpiryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = listAdapter

        // Use parent fragment's ViewModel to share data
        val parent = parentFragment ?: return
        viewModel = ViewModelProvider(parent)[AccountProductExpiryViewModel::class.java]

        viewModel.soonExpiryProducts.observe(viewLifecycleOwner) { products ->
            listAdapter.submitList(products)
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
        const val TAG = "SoonExpiryBottomSheet"

        fun newInstance(): SoonExpiryBottomSheet {
            return SoonExpiryBottomSheet()
        }
    }
}
