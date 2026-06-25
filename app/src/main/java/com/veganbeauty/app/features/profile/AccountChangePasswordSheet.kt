package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.databinding.ChangePasswordSheetBinding

class AccountChangePasswordSheet : BottomSheetDialogFragment() {

    private var _binding: ChangePasswordSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChangePasswordSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Let user advance through the UI logic if needed later
        binding.fpBtnChooseEmail.setOnClickListener {
            binding.fpStep0Container.visibility = View.GONE
            binding.fpStep1Container.visibility = View.VISIBLE
        }

        binding.fpBtnChoosePhone.setOnClickListener {
            binding.fpStep0Container.visibility = View.GONE
            binding.fpStep1Container.visibility = View.VISIBLE
        }

        binding.fpBtnSendOtp.setOnClickListener {
            binding.fpStep1Container.visibility = View.GONE
            binding.fpStep2Container.visibility = View.VISIBLE
        }

        binding.fpBtnVerifyOtp.setOnClickListener {
            binding.fpStep2Container.visibility = View.GONE
            binding.fpStep3Container.visibility = View.VISIBLE
        }

        binding.fpBtnResetPassword.setOnClickListener {
            binding.fpStep3Container.visibility = View.GONE
            binding.fpStep4Container.visibility = View.VISIBLE
        }

        binding.fpBtnGoLogin.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
