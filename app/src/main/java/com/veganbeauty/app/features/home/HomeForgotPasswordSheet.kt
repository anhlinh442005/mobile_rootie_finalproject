package com.veganbeauty.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.HomeForgotPasswordSheetBinding

class HomeForgotPasswordSheet : BottomSheetDialogFragment() {

    private var _binding: HomeForgotPasswordSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HomeForgotPasswordSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Simple initial implementation to handle click events
        binding.fpBackToLogin0.setOnClickListener {
            dismiss()
        }
        
        // Let user advance through the UI logic if needed later
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
