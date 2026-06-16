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
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import com.veganbeauty.app.features.account.order.AccountOrderListFragment
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.repository.OrderRepository
import kotlinx.coroutines.launch
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
        val ctx = requireContext()
        val isLoggedIn = com.veganbeauty.app.data.local.ProfileSession.isLoggedIn(ctx)

        // Load dynamic values from ProfileSession
        val avatarUrl = com.veganbeauty.app.data.local.ProfileSession.getAvatar(ctx)
        binding.ivAvatar.load(avatarUrl) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(android.R.color.darker_gray)
        }

        val guestRedirectListener = View.OnClickListener {
            Toast.makeText(ctx, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            val intent = android.content.Intent(ctx, com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }

        if (isLoggedIn) {
            val fullName = com.veganbeauty.app.data.local.ProfileSession.getFullName(ctx)
            val email = com.veganbeauty.app.data.local.ProfileSession.getEmail(ctx)
            binding.tvUsername.text = fullName
            binding.tvEmail.text = email
        } else {
            binding.tvUsername.text = "Khách hàng"
            binding.tvEmail.text = "Chạm để đăng nhập"
            binding.tvUsername.setOnClickListener(guestRedirectListener)
            binding.tvEmail.setOnClickListener(guestRedirectListener)
            binding.ivAvatar.setOnClickListener(guestRedirectListener)
        }

        val setSecuredOnClickListener = { view: View, action: () -> Unit ->
            view.setOnClickListener {
                if (isLoggedIn) {
                    action()
                } else {
                    guestRedirectListener.onClick(it)
                }
            }
        }

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
        setSecuredOnClickListener(binding.btnAllOrders) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Tất cả"))
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Order List Fragment with specific status filters pre-selected
        setSecuredOnClickListener(binding.btnStatusPending) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Chờ xử lý"))
                .addToBackStack(null)
                .commit()
        }

        setSecuredOnClickListener(binding.btnStatusProcessing) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đang xử lý"))
                .addToBackStack(null)
                .commit()
        }

        setSecuredOnClickListener(binding.btnStatusDelivering) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đang giao"))
                .addToBackStack(null)
                .commit()
        }

        setSecuredOnClickListener(binding.btnStatusSuccess) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                .addToBackStack(null)
                .commit()
        }

        setSecuredOnClickListener(binding.btnStatusCancelled) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Đã hủy"))
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Edit Profile Fragment when clicking the edit pencil button
        setSecuredOnClickListener(binding.btnEditProfile) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileEditFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Loyalty Reward & Exchange Fragment
        setSecuredOnClickListener(binding.btnRewardExchange) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.account.reward.AccountRewardFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Order List for Reviews
        setSecuredOnClickListener(binding.btnReviewProducts) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountOrderListFragment.newInstance("Hoàn tất"))
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Daily Check-in Fragment
        setSecuredOnClickListener(binding.layoutCoinsBadge) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.account.checkin.AccountCheckinFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Account Setup Fragment
        setSecuredOnClickListener(binding.btnAccountSetup) {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileSetupFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Voucher Wallet Fragment
        binding.btnRootieDeal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountVoucherFragment())
                .addToBackStack(null)
                .commit()
        }

        // Action Buttons Click Listeners
        view.findViewById<View>(com.veganbeauty.app.R.id.iv_pin)?.parent?.let { parentLayout ->
            (parentLayout as View).setOnClickListener {
                Toast.makeText(context, "Chọn địa chỉ giao hàng", Toast.LENGTH_SHORT).show()
            }
        }

        // Highlight the "Tài khoản" tab as active in the bottom navigation menu and set up click listeners
        com.veganbeauty.app.utils.NavAppUtils.setupNavApp(this, view, com.veganbeauty.app.R.id.nav_account)



        // Navigate to Weather & Skin page
        binding.btnSkinWeather.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.weather.WeatherForecastFragment())
                .addToBackStack(null)
                .commit()
        }

        // Navigate to Skincare Routine Reminder page
        binding.btnSkinReminder.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.routine.SkinReminderFragment())
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
                    .replace(com.veganbeauty.app.R.id.main_container, com.veganbeauty.app.features.profile.SkinAllergyProfileFragment())
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

        if (isLoggedIn) {
            // Retrieve and observe dynamic reward points count from Room database
            val db = RootieDatabase.getDatabase(requireContext())
            val repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
            viewLifecycleOwner.lifecycleScope.launch {
                repository.refreshOrders()
            }
            viewLifecycleOwner.lifecycleScope.launch {
                db.rewardPointDao().getTotalPointsFlow().collect { points ->
                    binding.tvCoins.text = (points ?: 0).toString()
                }
            }
        } else {
            binding.tvCoins.text = "0"
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
