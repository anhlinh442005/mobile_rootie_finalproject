package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.SkinFragmentHomeBinding
import com.veganbeauty.app.utils.NavAppUtils
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.data.local.LocalJsonReader

class MySkinFragment : RootieFragment() {

    private var _binding: SkinFragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Cài đặt tên chào người dùng
        val fullName = ProfileSession.getFullName(requireContext())
        if (!fullName.isNullOrBlank()) {
            val firstName = fullName.trim().split(" ").lastOrNull() ?: fullName
            binding.skinGreetingName.text = "Xin chào, $firstName"
        }

        // Cài đặt bottom nav
        NavAppUtils.setupNavApp(this, view, R.id.nav_myskin)

        // Banner và shortcut Quét da AI → mở SkinScanFragment
        binding.skinBannerContainer.setOnClickListener { openScanFragment() }
        binding.skinShortcutScanAi.setOnClickListener { openScanFragment() }
        binding.skinShortcutProfile.setOnClickListener {
            android.widget.Toast.makeText(context, "Hồ sơ da — đang phát triển", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.skinShortcutRoutine.setOnClickListener {
            android.widget.Toast.makeText(context, "Routine chăm da — đang phát triển", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.skinShortcutReminder.setOnClickListener {
            android.widget.Toast.makeText(context, "Nhắc lịch — đang phát triển", android.widget.Toast.LENGTH_SHORT).show()
        }
        binding.skinShortcutWeather.setOnClickListener {
            android.widget.Toast.makeText(context, "Skin Weather — đang phát triển", android.widget.Toast.LENGTH_SHORT).show()
        }

        // Setup RecyclerView cho cửa hàng
        val jsonReader = LocalJsonReader(requireContext())
        val allStores = jsonReader.getAllStores()
        val topStores = allStores.take(5)
        
        binding.rvSkinStores.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = SkinStoreAdapter(topStores) { store ->
            val bookingFragment = BookingFragment.newInstance(
                store.storeName,
                store.address,
                store.imageUrl
            )
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, bookingFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvSkinStores.adapter = adapter

        binding.skinStoreSeeAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, ChooseBranchFragment())
                .addToBackStack(null)
                .commit()
        }

        // Lịch sử lịch đặt
        binding.skinBtnBookingHistory.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, BookingHistoryFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun observeViewModel() {
        // Not used yet
    }

    private fun openScanFragment() {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.slide_out_right
            )
            .replace(R.id.main_container, SkinScanFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
