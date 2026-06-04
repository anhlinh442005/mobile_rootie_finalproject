package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfilePersonalInfoBinding

class AccountProfilePersonalInfoFragment : RootieFragment() {

    private var _binding: AccountProfilePersonalInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfilePersonalInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val context = requireContext()
        val fullName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context)
        val cccd = com.veganbeauty.app.data.local.ProfileSession.getCCCD(context)
        val address = com.veganbeauty.app.data.local.ProfileSession.getAddress(context)

        // Initial setup with masking
        binding.etFullname.setText(maskFullName(fullName))
        binding.etCccd.setText(maskCCCD(cccd))
        binding.etAddress.setText(maskAddress(address))
        binding.tvAddressCount.text = "${address.length}/200"

        // Set focus change listeners to handle secure editing
        binding.etFullname.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val current = binding.etFullname.text.toString()
                if (current.contains("*")) {
                    binding.etFullname.setText(com.veganbeauty.app.data.local.ProfileSession.getFullName(context))
                }
            } else {
                val entered = binding.etFullname.text.toString()
                if (!entered.contains("*") && entered.isNotBlank()) {
                    com.veganbeauty.app.data.local.ProfileSession.setFullName(context, entered)
                }
                binding.etFullname.setText(maskFullName(com.veganbeauty.app.data.local.ProfileSession.getFullName(context)))
            }
        }

        binding.etCccd.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val current = binding.etCccd.text.toString()
                if (current.contains("*")) {
                    binding.etCccd.setText(com.veganbeauty.app.data.local.ProfileSession.getCCCD(context))
                }
            } else {
                val entered = binding.etCccd.text.toString()
                if (!entered.contains("*") && entered.isNotBlank()) {
                    com.veganbeauty.app.data.local.ProfileSession.setCCCD(context, entered)
                }
                binding.etCccd.setText(maskCCCD(com.veganbeauty.app.data.local.ProfileSession.getCCCD(context)))
            }
        }

        binding.etAddress.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val currentText = s?.toString() ?: ""
                if (binding.etAddress.hasFocus() && !currentText.contains("*")) {
                    binding.tvAddressCount.text = "${currentText.length}/200"
                } else {
                    val realAddr = com.veganbeauty.app.data.local.ProfileSession.getAddress(context)
                    binding.tvAddressCount.text = "${realAddr.length}/200"
                }
            }
        })

        binding.etAddress.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val current = binding.etAddress.text.toString()
                if (current.contains("*")) {
                    binding.etAddress.setText(com.veganbeauty.app.data.local.ProfileSession.getAddress(context))
                }
            } else {
                val entered = binding.etAddress.text.toString()
                if (!entered.contains("*") && entered.isNotBlank()) {
                    com.veganbeauty.app.data.local.ProfileSession.setAddress(context, entered)
                }
                binding.etAddress.setText(maskAddress(com.veganbeauty.app.data.local.ProfileSession.getAddress(context)))
                binding.tvAddressCount.text = "${com.veganbeauty.app.data.local.ProfileSession.getAddress(context).length}/200"
            }
        }

        // Back button action
        binding.btnBack.setOnClickListener {
            binding.etFullname.clearFocus()
            binding.etCccd.clearFocus()
            binding.etAddress.clearFocus()
            parentFragmentManager.popBackStack()
        }

        // Highlight the "Tài khoản" tab as active in the bottom navigation menu
        view.findViewById<android.widget.LinearLayout>(com.veganbeauty.app.R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? android.widget.ImageView
            val label = navAccount.getChildAt(1) as? android.widget.TextView
            
            // Set active green color tint to the icon (#677559)
            icon?.setColorFilter(android.graphics.Color.parseColor("#677559"))
            
            // Set active green color and bold style to the text label
            label?.setTextColor(android.graphics.Color.parseColor("#677559"))
            label?.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Interactive Ux feedback Toasts
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }
    }

    private fun maskFullName(fullName: String): String {
        if (fullName.isBlank()) return ""
        val firstChar = fullName.first().uppercase()
        val lastChar = fullName.last().uppercase()
        return "$firstChar*** **** ***$lastChar"
    }

    private fun maskCCCD(cccd: String): String {
        if (cccd.length < 4) return cccd
        return "*********${cccd.takeLast(4)}"
    }

    private fun maskAddress(address: String): String {
        if (address.isBlank()) return ""
        return "********"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
