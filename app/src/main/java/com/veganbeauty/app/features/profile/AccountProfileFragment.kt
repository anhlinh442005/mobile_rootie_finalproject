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
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.repository.OrderRepository
import kotlinx.coroutines.launch

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

        // Load dynamic values from ProfileSession
        val ctx = requireContext()
        val fullName = com.veganbeauty.app.data.local.ProfileSession.getFullName(ctx)
        val email = com.veganbeauty.app.data.local.ProfileSession.getEmail(ctx)
        binding.tvUsername.text = fullName
        binding.tvEmail.text = email

        // Load skin type from SharedPreferences saved by Quiz
        val prefs = ctx.getSharedPreferences("RootieQuizPrefs", android.content.Context.MODE_PRIVATE)
        val savedSkinType = prefs.getString("SAVED_USER_SKIN_TYPE", null)
        if (savedSkinType != null) {
            binding.tvProfileSkinType.text = savedSkinType
            binding.llProfileSkinBadge.visibility = android.view.View.VISIBLE
        } else {
            binding.tvProfileSkinType.text = "Chưa làm quiz"
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

        binding.btnExpiryShelf.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.account.expiry.AccountProductExpiryFragment())
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

        // Navigate to Loyalty Reward & Exchange Fragment
        binding.btnRewardExchange.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.account.reward.AccountRewardFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Daily Check-in Fragment
        binding.layoutCoinsBadge.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.account.checkin.AccountCheckinFragment())
                .addToBackStack(null)
                .commit()
        }

        // Action Buttons Click Listeners
        view.findViewById<View>(com.veganbeauty.app.R.id.iv_pin)?.parent?.let { parentLayout ->
            (parentLayout as View).setOnClickListener {
                Toast.makeText(context, "Chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            }
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

        // Navigate to Weather or Quiz on nav_myskin click depending on profile existence
        view.findViewById<android.widget.LinearLayout>(com.veganbeauty.app.R.id.nav_myskin)?.setOnClickListener {
            val savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null)
            val destination = if (savedSkin != null) {
                com.veganbeauty.app.features.weather.WeatherForecastFragment()
            } else {
                QuizTestIntroFragment()
            }
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, destination)
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Weather & Skin page
        binding.btnWeatherSkin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.weather.WeatherForecastFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnTreatmentHistory.setOnClickListener {
            Toast.makeText(context, "Tính năng Lịch sử liệu trình đang được phát triển", Toast.LENGTH_SHORT).show()
        }

        // Navigate to Skin Profile (Quiz Result) if skin analysis exists, otherwise navigate to start Quiz
        binding.btnSkinProfile.setOnClickListener {
            val savedSkin = prefs.getString("SAVED_USER_SKIN_TYPE", null)
            if (savedSkin != null) {
                val recommendation = prefs.getString("SAVED_RECOMMENDATION", null)
                val flaggedGroups = prefs.getStringSet("SAVED_FLAGGED_GROUPS", null)
                
                prefs.edit().apply {
                    putString("SKIN_TYPE_RESULT", savedSkin)
                    if (recommendation != null) {
                        putString("RECOMMENDATION", recommendation)
                    }
                    if (flaggedGroups != null) {
                        putStringSet("FLAGGED_GROUPS", flaggedGroups)
                    }
                    apply()
                }

                parentFragmentManager.beginTransaction()
                    .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.quiz.QuizTestResultFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Bạn chưa thực hiện khảo sát da. Đang chuyển hướng đến bài test...", Toast.LENGTH_LONG).show()
                parentFragmentManager.beginTransaction()
                    .replace(com.veganbeauty.app.R.id.main_container, QuizTestIntroFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        // Retrieve and observe dynamic reward points count from Room database
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshOrders()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getTotalPointsFlow().collect { points ->
                binding.tvCoins.text = (points ?: 0).toString()
            }
        }
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
