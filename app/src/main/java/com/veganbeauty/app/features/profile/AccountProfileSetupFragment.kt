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
        val context = requireContext()

        // Back button action
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Highlight the "Tài khoản" tab as active in the bottom navigation menu
        view.findViewById<android.view.ViewGroup>(com.veganbeauty.app.R.id.nav_account)?.let { navAccount ->
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

        // Group 2: Cài đặt (Bật tắt switch trực tiếp & xổ xuống chọn lựa)
        
        // 2.1. Bật/Tắt bong bóng Chat trợ lý AI
        updateSwitchUI(binding.switchChatContainer, binding.switchChatThumb, isChatBubbleEnabled(context))
        binding.switchChatContainer.setOnClickListener {
            val nextState = !isChatBubbleEnabled(context)
            setChatBubbleEnabled(context, nextState)
            updateSwitchUI(binding.switchChatContainer, binding.switchChatThumb, nextState)
            Toast.makeText(context, if (nextState) "Đã bật bong bóng trợ lý AI nổi" else "Đã tắt bong bóng trợ lý AI", Toast.LENGTH_SHORT).show()
        }

        // 2.2. Mở trang Cài đặt thông báo chi tiết
        binding.btnNotificationSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, AccountProfileNotiSettingFragment())
                .addToBackStack(null)
                .commit()
        }

        // 2.3. Bật/Tắt Chế độ riêng tư
        updateSwitchUI(binding.switchPrivacyContainer, binding.switchPrivacyThumb, isPrivateModeEnabled(context))
        binding.switchPrivacyContainer.setOnClickListener {
            val nextState = !isPrivateModeEnabled(context)
            setPrivateModeEnabled(context, nextState)
            updateSwitchUI(binding.switchPrivacyContainer, binding.switchPrivacyThumb, nextState)
            Toast.makeText(context, if (nextState) "Đã kích hoạt chế độ riêng tư" else "Đã tắt chế độ riêng tư", Toast.LENGTH_SHORT).show()
        }

        // 2.4. Ngôn ngữ / Language (Xổ nội dung chọn lựa trực tiếp)
        var isLanguageExpanded = false
        binding.btnLanguage.setOnClickListener {
            isLanguageExpanded = !isLanguageExpanded
            binding.expandableLanguage.visibility = if (isLanguageExpanded) View.VISIBLE else View.GONE
            binding.ivLanguageChevron.animate().rotation(if (isLanguageExpanded) 90f else 0f).setDuration(200).start()
        }

        // Khôi phục trạng thái ngôn ngữ đã chọn
        val savedLang = context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
            .getString("app_language", "Tiếng Việt") ?: "Tiếng Việt"
        binding.tvCurrentLanguage.text = savedLang
        if (savedLang == "English") {
            binding.rbLangEn.isChecked = true
        } else {
            binding.rbLangVi.isChecked = true
        }

        binding.rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val lang = if (checkedId == com.veganbeauty.app.R.id.rb_lang_vi) "Tiếng Việt" else "English"
            binding.tvCurrentLanguage.text = lang
            context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("app_language", lang)
                .apply()
            Toast.makeText(context, "Đã chuyển ngôn ngữ sang $lang", Toast.LENGTH_SHORT).show()
        }

        // Group 3: Hỗ trợ
        binding.btnHelpCenter.setOnClickListener {
            Toast.makeText(context, "Trung tâm hỗ trợ (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnCommunityGuidelines.setOnClickListener {
            Toast.makeText(context, "Tiêu chuẩn cộng đồng (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }
        binding.btnTerms.setOnClickListener {
            Toast.makeText(context, "Điều khoản sử dụng Rootie (Đang phát triển)", Toast.LENGTH_SHORT).show()
        }

        // 3.4. Đánh giá chất lượng ứng dụng (Xổ ra đánh giá sao trực tiếp)
        var isRatingExpanded = false
        binding.btnRateApp.setOnClickListener {
            isRatingExpanded = !isRatingExpanded
            binding.expandableRating.visibility = if (isRatingExpanded) View.VISIBLE else View.GONE
            binding.ivRateChevron.animate().rotation(if (isRatingExpanded) 90f else 0f).setDuration(200).start()
        }

        val stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                for (i in 0..4) {
                    if (i <= index) {
                        stars[i].setColorFilter(android.graphics.Color.parseColor("#FFD700")) // Gold
                    } else {
                        stars[i].setColorFilter(android.graphics.Color.parseColor("#D1D6D2")) // Grey
                    }
                }
                Toast.makeText(context, "Cảm ơn bạn đã đánh giá ${index + 1} sao cho Rootie!", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout
        binding.btnLogout.setOnClickListener {
            com.veganbeauty.app.data.local.ProfileSession.setLoggedIn(context, false)
            context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putLong("last_login", 0L)
                .apply()
            
            Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
            
            val intent = android.content.Intent(activity, com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun updateSwitchUI(container: android.widget.FrameLayout, thumb: android.widget.ImageView, enabled: Boolean) {
        if (enabled) {
            container.setBackgroundResource(com.veganbeauty.app.R.drawable.ic_switch_track_on)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            lp.marginStart = 0
            lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        } else {
            container.setBackgroundResource(com.veganbeauty.app.R.drawable.ic_switch_track_off)
            val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            lp.marginEnd = 0
            lp.marginStart = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        }
    }

    private fun isChatBubbleEnabled(context: android.content.Context): Boolean {
        return context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("chat_bubble_enabled", true)
    }

    private fun setChatBubbleEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("chat_bubble_enabled", enabled)
            .apply()
    }

    private fun isPrivateModeEnabled(context: android.content.Context): Boolean {
        return context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
            .getBoolean("private_mode_enabled", false)
    }

    private fun setPrivateModeEnabled(context: android.content.Context, enabled: Boolean) {
        context.getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("private_mode_enabled", enabled)
            .apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
