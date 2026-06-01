package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfileEditBinding

class AccountProfileEditFragment : RootieFragment() {

    private var _binding: AccountProfileEditBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Load the elegant placeholder avatar from Unsplash using Coil with CircleCrop matching main profile
        binding.ivAvatar.load("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80") {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(android.R.color.darker_gray)
        }

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

        // Micro-animations & Toast Feedbacks for modern UX
        binding.btnChangeAvatar.setOnClickListener {
            Toast.makeText(context, "Thay đổi ảnh đại diện", Toast.LENGTH_SHORT).show()
        }

        binding.btnSelectDob.setOnClickListener {
            Toast.makeText(context, "Mở trình chọn ngày sinh", Toast.LENGTH_SHORT).show()
        }

        binding.btnLinkedAccounts.setOnClickListener {
            Toast.makeText(context, "Quản lý tài khoản liên kết", Toast.LENGTH_SHORT).show()
        }

        binding.btnPersonalInfo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfilePersonalInfoFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnAccountSettings.setOnClickListener {
            Toast.makeText(context, "Thiết lập tài khoản của bạn", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(context, "Thay đổi mật khẩu", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
