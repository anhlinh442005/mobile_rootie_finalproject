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
        // Back button action
        binding.btnBack.setOnClickListener {
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

        // Text change listener or click listener on Address field
        binding.etAddress.setOnClickListener {
            Toast.makeText(context, "Chỉnh sửa địa chỉ", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
