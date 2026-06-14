package com.veganbeauty.app.features.account.expiry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.AccountBottomSheetExpiryActionBinding

class ExpiryActionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AccountBottomSheetExpiryActionBinding? = null
    private val binding get() = _binding!!

    private var productName: String = ""
    private var productExpiry: String = ""
    private var productImage: String = ""
    private var onBuyAgainClicked: (() -> Unit)? = null
    private var onDeleteClicked: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productName = it.getString(ARG_NAME, "")
            productExpiry = it.getString(ARG_EXPIRY, "")
            productImage = it.getString(ARG_IMAGE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountBottomSheetExpiryActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvProductName.text = productName
        binding.tvProductExpiry.text = "Hạn sử dụng: $productExpiry"
        binding.ivProductImage.load(productImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }

        binding.btnBuyAgain.setOnClickListener {
            onBuyAgainClicked?.invoke()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            onDeleteClicked?.invoke()
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

    fun setActions(onBuyAgain: () -> Unit, onDelete: () -> Unit) {
        this.onBuyAgainClicked = onBuyAgain
        this.onDeleteClicked = onDelete
    }

    companion object {
        const val TAG = "ExpiryActionBottomSheet"
        private const val ARG_NAME = "ARG_NAME"
        private const val ARG_EXPIRY = "ARG_EXPIRY"
        private const val ARG_IMAGE = "ARG_IMAGE"

        fun newInstance(
            productName: String,
            productExpiry: String,
            productImage: String
        ): ExpiryActionBottomSheet {
            return ExpiryActionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_NAME, productName)
                    putString(ARG_EXPIRY, productExpiry)
                    putString(ARG_IMAGE, productImage)
                }
            }
        }
    }
}
