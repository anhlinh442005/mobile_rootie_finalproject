package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfileSetupBinding

class AccountProfileSetupFragment : RootieFragment() {

    private var _binding: AccountProfileSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileSetupBinding.inflate(inflater, container, false)
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

        // Notification bell
        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }

        // Group 1: Tài Khoản Của Tôi
        binding.btnAccountSecurity.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileSecurityFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnAddress.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileAddressFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.btnBankAccount.setOnClickListener {
            Toast.makeText(context, "Tài khoản / Thẻ ngân hàng (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        // Group 2: Cài đặt
        binding.btnChatSettings.setOnClickListener {
            Toast.makeText(context, "Cài đặt chat (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotificationSettings.setOnClickListener {
            Toast.makeText(context, "Cài đặt thông báo (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnPrivacySettings.setOnClickListener {
            Toast.makeText(context, "Cài đặt riêng tư (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnLanguage.setOnClickListener {
            Toast.makeText(context, "Đổi ngôn ngữ (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        // Group 3: Hỗ trợ
        binding.btnHelpCenter.setOnClickListener {
            Toast.makeText(context, "Trung tâm hỗ trợ (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnCommunityGuidelines.setOnClickListener {
            Toast.makeText(context, "Tiêu chuẩn cộng đồng (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnTerms.setOnClickListener {
            Toast.makeText(context, "Điều khoản Rootie (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnRateApp.setOnClickListener {
            Toast.makeText(context, "Cảm ơn bạn đã đánh giá ứng dụng!", Toast.LENGTH_SHORT).show()
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            // In a real app, clear session and navigate to Login screen
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
