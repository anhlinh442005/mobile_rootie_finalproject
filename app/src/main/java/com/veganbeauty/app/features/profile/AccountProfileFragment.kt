package com.veganbeauty.app.features.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.AccountProfileBinding
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import com.veganbeauty.app.features.account.order.AccountOrderListFragment

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
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Order List Fragment
        binding.btnAllOrders.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Tất cả"))
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Order List Fragment with specific status filters pre-selected
        binding.btnStatusPending.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Chờ xác nhận"))
                .addToBackStack(null)
                .commit()
        }

        binding.btnStatusProcessing.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đang xử lý"))
                .addToBackStack(null)
                .commit()
        }

        binding.btnStatusDelivering.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đang giao"))
                .addToBackStack(null)
                .commit()
        }

        binding.btnStatusSuccess.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Thành công"))
                .addToBackStack(null)
                .commit()
        }

        binding.btnStatusCancelled.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đã hủy"))
                .addToBackStack(null)
                .commit()
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

        // Navigation
        com.veganbeauty.app.utils.NavAppUtils.setupNavApp(this, view, com.veganbeauty.app.R.id.nav_account)
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
