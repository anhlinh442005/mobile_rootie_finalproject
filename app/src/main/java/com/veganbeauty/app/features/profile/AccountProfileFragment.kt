package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfileBinding
import com.veganbeauty.app.features.home.BottomNavHelper

class AccountProfileFragment : RootieFragment() {

    private var _binding: AccountProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Load elegant placeholder avatar from Unsplash using Coil with CircleCrop
        binding.ivAvatar.load("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=256&q=80") {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(android.R.color.darker_gray)
        }

        // Set click listeners for interactive feel
        binding.llSearch.setOnClickListener {
            Toast.makeText(context, "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
        }

        binding.btnQrScan.setOnClickListener {
            Toast.makeText(context, "Mở trình quét mã QR", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }

        // Navigate to Edit Profile Fragment when clicking the edit pencil button
        binding.btnEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileEditFragment())
                .addToBackStack(null)
                .commit()
        }

        // Action Buttons Click Listeners
        view.findViewById<View>(com.veganbeauty.app.R.id.iv_pin)?.parent?.let { parentLayout ->
            (parentLayout as View).setOnClickListener {
                Toast.makeText(context, "Chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            }
        }

        BottomNavHelper.setup(
            fragment = this,
            root = binding.root,
            activeTabId = R.id.nav_account
        ) { tabId -> BottomNavHelper.navigate(this, tabId) }
    }

    override fun observeViewModel() {
        super.observeViewModel()
        // No ViewModel currently needed as per user request
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
