package com.veganbeauty.app.features.profile

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.AccountProfileNotiSettingBinding

class AccountProfileNotiSettingFragment : RootieFragment() {

    private var _binding: AccountProfileNotiSettingBinding? = null
    private val binding get() = _binding!!

    // Notification Permission Launcher for API 33+
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val context = requireContext()
        if (isGranted) {
            ProfileSession.setNotiEnabled(context, true)
            updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, true)
            syncAllSettingsEnabledState()
            triggerTestNotification("Đã bật thông báo", "Bạn sẽ nhận được các thông báo mới nhất từ Rootie.")
        } else {
            Toast.makeText(context, "Quyền thông báo bị từ chối", Toast.LENGTH_SHORT).show()
            ProfileSession.setNotiEnabled(context, false)
            updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, false)
            syncAllSettingsEnabledState()
            showSystemNotificationSettingsDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProfileNotiSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val context = requireContext()

        // 1. Back navigation
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Bell click action - navigate to account notifications list
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // 3. Populate Spinners/Dropdowns for promotions/vouchers
        val frequencies = listOf("Mỗi ngày", "Mỗi tuần", "Mỗi tháng")
        val freqAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, frequencies)
        freqAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = freqAdapter

        val timeRanges = listOf("08:00 - 21:00", "09:00 - 18:00", "07:00 - 22:00", "18:00 - 23:00", "Bất cứ lúc nào")
        val timeAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, timeRanges)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTimeRange.adapter = timeAdapter

        // Restore Spinner selections from ProfileSession
        val savedFreqIndex = frequencies.indexOf(ProfileSession.getPromotionFrequency(context))
        if (savedFreqIndex >= 0) {
            binding.spinnerFrequency.setSelection(savedFreqIndex)
        }
        val savedTimeIndex = timeRanges.indexOf(ProfileSession.getPromotionTimeRange(context))
        if (savedTimeIndex >= 0) {
            binding.spinnerTimeRange.setSelection(savedTimeIndex)
        }

        // Spinner item selection listeners
        binding.spinnerFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = frequencies[position]
                ProfileSession.setPromotionFrequency(requireContext(), selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerTimeRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = timeRanges[position]
                ProfileSession.setPromotionTimeRange(requireContext(), selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 4. Initialize Switches UI state from Session
        loadSwitchesState()

        // 5. Setup Switch click listeners
        setupSwitchListeners()

        // 6. Highlight "Tài khoản" Tab in Bottom Nav
        highlightBottomTab(view)

        // 7. Initial sync of layouts based on notification status
        syncAllSettingsEnabledState()
    }

    private fun loadSwitchesState() {
        val context = requireContext()
        updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, ProfileSession.isNotiEnabled(context))
        updateSwitchUI(binding.switchSoundContainer, binding.switchSoundThumb, ProfileSession.isSoundEnabled(context))
        updateSwitchUI(binding.switchVibrateContainer, binding.switchVibrateThumb, ProfileSession.isVibrateEnabled(context))
        updateSwitchUI(binding.switchLockScreenContainer, binding.switchLockScreenThumb, ProfileSession.isLockScreenEnabled(context))
        updateSwitchUI(binding.switchOrderStatusContainer, binding.switchOrderStatusThumb, ProfileSession.isOrderStatusEnabled(context))
        updateSwitchUI(binding.switchPromotionContainer, binding.switchPromotionThumb, ProfileSession.isPromotionEnabled(context))
        updateSwitchUI(binding.switchStaffMessageContainer, binding.switchStaffMessageThumb, ProfileSession.isStaffMessageEnabled(context))
        updateSwitchUI(binding.switchSkinWeatherContainer, binding.switchSkinWeatherThumb, ProfileSession.isSkinWeatherNotiEnabled(context))
        
        // Load Expiry Notification switch states
        updateSwitchUI(binding.switchExpiryNotiContainer, binding.switchExpiryNotiThumb, ProfileSession.isNotiExpiryEnabled(context))
        updateSwitchUI(binding.switchExpiryWeek1Container, binding.switchExpiryWeek1Thumb, ProfileSession.isNotiExpiryWeek1(context))
        updateSwitchUI(binding.switchExpiryWeek2Container, binding.switchExpiryWeek2Thumb, ProfileSession.isNotiExpiryWeek2(context))
    }

    private fun setupSwitchListeners() {
        val context = requireContext()

        // General Notification Switch
        binding.switchNotiEnabledContainer.setOnClickListener {
            val nextState = !ProfileSession.isNotiEnabled(context)
            if (nextState) {
                // Check if system notifications allowed
                if (!areNotificationsAllowed()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        showSystemNotificationSettingsDialog()
                    }
                } else {
                    ProfileSession.setNotiEnabled(context, true)
                    updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, true)
                    syncAllSettingsEnabledState()
                    triggerTestNotification("Thông báo đã bật", "Bạn sẽ nhận được các thông báo mới nhất từ Rootie.")
                }
            } else {
                ProfileSession.setNotiEnabled(context, false)
                updateSwitchUI(binding.switchNotiEnabledContainer, binding.switchNotiEnabledThumb, false)
                syncAllSettingsEnabledState()
            }
        }

        // Sound Switch
        binding.switchSoundContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isSoundEnabled(context)
            ProfileSession.setSoundEnabled(context, nextState)
            updateSwitchUI(binding.switchSoundContainer, binding.switchSoundThumb, nextState)
            if (nextState) {
                triggerTestNotification("Âm thanh thông báo", "Đã bật âm thanh thông báo.")
            }
        }

        // Vibrate Switch
        binding.switchVibrateContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isVibrateEnabled(context)
            ProfileSession.setVibrateEnabled(context, nextState)
            updateSwitchUI(binding.switchVibrateContainer, binding.switchVibrateThumb, nextState)
            if (nextState) {
                triggerTestNotification("Rung phản hồi", "Đã bật rung cho thông báo.")
            }
        }

        // Lock Screen Switch
        binding.switchLockScreenContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isLockScreenEnabled(context)
            ProfileSession.setLockScreenEnabled(context, nextState)
            updateSwitchUI(binding.switchLockScreenContainer, binding.switchLockScreenThumb, nextState)
            if (nextState) {
                triggerTestNotification("Hiển thị Lock Screen", "Thông báo sẽ hiển thị trên màn hình khóa.")
            }
        }

        // Order Status Switch
        binding.switchOrderStatusContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isOrderStatusEnabled(context)
            ProfileSession.setOrderStatusEnabled(context, nextState)
            updateSwitchUI(binding.switchOrderStatusContainer, binding.switchOrderStatusThumb, nextState)
            if (nextState) {
                triggerTestNotification("Cập nhật đơn hàng", "Trạng thái đơn hàng của bạn sẽ liên tục được cập nhật.")
            }
        }

        // Promotions Switch
        binding.switchPromotionContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isPromotionEnabled(context)
            ProfileSession.setPromotionEnabled(context, nextState)
            updateSwitchUI(binding.switchPromotionContainer, binding.switchPromotionThumb, nextState)
            
            // Toggle visibility of frequency and time selectors
            binding.layoutPromotionOptions.visibility = if (nextState) View.VISIBLE else View.GONE
            
            if (nextState) {
                triggerTestNotification("Khuyến mãi cá nhân", "Bạn sẽ nhận được voucher ưu đãi phù hợp nhất.")
            }
        }

        // Staff Messages Switch
        binding.switchStaffMessageContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isStaffMessageEnabled(context)
            ProfileSession.setStaffMessageEnabled(context, nextState)
            updateSwitchUI(binding.switchStaffMessageContainer, binding.switchStaffMessageThumb, nextState)
            if (nextState) {
                triggerTestNotification("Tin nhắn nhân viên", "Bạn có tin nhắn mới từ tư vấn viên.")
            }
        }

        // Weather & Skin Switch
        binding.switchSkinWeatherContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isSkinWeatherNotiEnabled(context)
            ProfileSession.setSkinWeatherNotiEnabled(context, nextState)
            updateSwitchUI(binding.switchSkinWeatherContainer, binding.switchSkinWeatherThumb, nextState)
            if (nextState) {
                com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.scheduleDailyNotification(context)
            } else {
                com.veganbeauty.app.features.weather.DailySkinWeatherScheduler.cancelDailyNotification(context)
            }
        }

        // Expiry Notifications Master Switch
        binding.switchExpiryNotiContainer.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isNotiExpiryEnabled(context)
            ProfileSession.setNotiExpiryEnabled(context, nextState)
            updateSwitchUI(binding.switchExpiryNotiContainer, binding.switchExpiryNotiThumb, nextState)
            
            binding.layoutExpiryOptions.visibility = if (nextState) View.VISIBLE else View.GONE
            
            if (nextState) {
                triggerTestNotification("Thông báo hết hạn", "Đã bật nhắc nhở hạn sử dụng sản phẩm.")
            }
        }

        // Expiry Week 1 Switch
        binding.switchExpiryWeek1Container.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context) || !ProfileSession.isNotiExpiryEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isNotiExpiryWeek1(context)
            ProfileSession.setNotiExpiryWeek1(context, nextState)
            updateSwitchUI(binding.switchExpiryWeek1Container, binding.switchExpiryWeek1Thumb, nextState)
        }

        // Expiry Week 2 Switch
        binding.switchExpiryWeek2Container.setOnClickListener {
            if (!ProfileSession.isNotiEnabled(context) || !ProfileSession.isNotiExpiryEnabled(context)) return@setOnClickListener
            val nextState = !ProfileSession.isNotiExpiryWeek2(context)
            ProfileSession.setNotiExpiryWeek2(context, nextState)
            updateSwitchUI(binding.switchExpiryWeek2Container, binding.switchExpiryWeek2Thumb, nextState)
        }
    }

    private fun syncAllSettingsEnabledState() {
        val isEnabled = ProfileSession.isNotiEnabled(requireContext())
        val alpha = if (isEnabled) 1.0f else 0.5f

        // Apply alpha visually to sub-options
        binding.switchSoundContainer.isEnabled = isEnabled
        binding.switchSoundContainer.alpha = alpha
        binding.switchVibrateContainer.isEnabled = isEnabled
        binding.switchVibrateContainer.alpha = alpha
        binding.switchLockScreenContainer.isEnabled = isEnabled
        binding.switchLockScreenContainer.alpha = alpha
        binding.switchOrderStatusContainer.isEnabled = isEnabled
        binding.switchOrderStatusContainer.alpha = alpha
        binding.switchPromotionContainer.isEnabled = isEnabled
        binding.switchPromotionContainer.alpha = alpha
        binding.switchStaffMessageContainer.isEnabled = isEnabled
        binding.switchStaffMessageContainer.alpha = alpha
        binding.switchSkinWeatherContainer.isEnabled = isEnabled
        binding.switchSkinWeatherContainer.alpha = alpha
        
        // Expiry toggles activation
        binding.switchExpiryNotiContainer.isEnabled = isEnabled
        binding.switchExpiryNotiContainer.alpha = alpha

        // Disable or hide promotional dropdown options if promotions are off, or if general notification is off
        val isPromotionVisible = isEnabled && ProfileSession.isPromotionEnabled(requireContext())
        binding.layoutPromotionOptions.visibility = if (isPromotionVisible) View.VISIBLE else View.GONE

        // Disable or hide expiry sub-options
        val isExpiryEnabled = ProfileSession.isNotiExpiryEnabled(requireContext())
        val isExpiryVisible = isEnabled && isExpiryEnabled
        binding.layoutExpiryOptions.visibility = if (isExpiryVisible) View.VISIBLE else View.GONE
        
        binding.switchExpiryWeek1Container.isEnabled = isExpiryVisible
        binding.switchExpiryWeek1Container.alpha = if (isExpiryVisible) 1.0f else 0.5f
        binding.switchExpiryWeek2Container.isEnabled = isExpiryVisible
        binding.switchExpiryWeek2Container.alpha = if (isExpiryVisible) 1.0f else 0.5f
    }

    private fun updateSwitchUI(container: FrameLayout, thumb: ImageView, enabled: Boolean) {
        if (enabled) {
            container.setBackgroundResource(R.drawable.ic_switch_track_on)
            val lp = thumb.layoutParams as FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
            lp.marginStart = 0
            lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        } else {
            container.setBackgroundResource(R.drawable.ic_switch_track_off)
            val lp = thumb.layoutParams as FrameLayout.LayoutParams
            lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            lp.marginEnd = 0
            lp.marginStart = (2 * resources.displayMetrics.density).toInt()
            thumb.layoutParams = lp
        }
    }

    private fun areNotificationsAllowed(): Boolean {
        val context = requireContext()
        val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!systemNotificationsEnabled) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showSystemNotificationSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cho phép thông báo")
            .setMessage("Thông báo hiện đang bị tắt cho ứng dụng này. Vui lòng bật thông báo trong Cài đặt hệ thống để nhận cập nhật từ Rootie.")
            .setPositiveButton("Cài đặt") { _, _ ->
                openSystemNotificationSettings()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openSystemNotificationSettings() {
        val context = requireContext()
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", context.packageName, null)
            }
            startActivity(fallbackIntent)
        }
    }

    private fun triggerTestNotification(title: String, content: String) {
        val context = requireContext()
        if (!ProfileSession.isNotiEnabled(context) || !areNotificationsAllowed()) {
            return
        }

        val soundEnabled = ProfileSession.isSoundEnabled(context)
        val vibrateEnabled = ProfileSession.isVibrateEnabled(context)

        // Dynamic channel ID based on sound/vibrate states to bypass Android channel configuration immutability
        val channelId = "rootie_channel_${if (soundEnabled) "s1" else "s0"}_${if (vibrateEnabled) "v1" else "v0"}"
        val notificationId = 101
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Cài đặt thông báo Rootie"
            val descriptionText = "Kênh gửi thông báo thử nghiệm từ cài đặt thông báo"
            
            // Silenced channels must use IMPORTANCE_LOW, otherwise they will still play sound
            val importance = if (soundEnabled) {
                NotificationManager.IMPORTANCE_DEFAULT
            } else {
                NotificationManager.IMPORTANCE_LOW
            }
            
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                if (!soundEnabled) {
                    setSound(null, null)
                }
                enableVibration(vibrateEnabled)
                if (!vibrateEnabled) {
                    vibrationPattern = longArrayOf(0L)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)

        if (!soundEnabled) {
            builder.setSound(null)
            builder.priority = NotificationCompat.PRIORITY_LOW
        } else {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            builder.priority = NotificationCompat.PRIORITY_DEFAULT
        }

        if (!vibrateEnabled) {
            builder.setVibrate(longArrayOf(0L))
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun highlightBottomTab(view: View) {
        view.findViewById<ViewGroup>(R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? ImageView
            val label = navAccount.getChildAt(1) as? TextView
            icon?.setColorFilter(Color.parseColor("#677559"))
            label?.setTextColor(Color.parseColor("#677559"))
            label?.setTypeface(null, Typeface.BOLD)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadSwitchesState()
            syncAllSettingsEnabledState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
