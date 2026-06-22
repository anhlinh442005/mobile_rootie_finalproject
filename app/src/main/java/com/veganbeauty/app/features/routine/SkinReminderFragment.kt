package com.veganbeauty.app.features.routine

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.SkinReminderBinding
import com.veganbeauty.app.data.local.ProfileSession
import androidx.lifecycle.lifecycleScope
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SkinReminderFragment : RootieFragment() {

    private var _binding: SkinReminderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        refreshUI()

        // Navigation Back
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Notification Click
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Morning Routine Action
        binding.btnStartMorningRoutine.setOnClickListener {
            val fragment = SkinTimeRoutineFragment().apply {
                arguments = Bundle().apply {
                    putString("routine_type", "morning")
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Evening Routine Action
        binding.btnStartEveningRoutine.setOnClickListener {
            val fragment = SkinTimeRoutineFragment().apply {
                arguments = Bundle().apply {
                    putString("routine_type", "evening")
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Setup Routine Settings
        binding.btnSetupRoutine.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, SkinRoutineSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        // Skincare Calendar
        binding.btnSkincareCalendar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, SkinCalendarFragment())
                .addToBackStack(null)
                .commit()
        }

        // Social Task (Connect Friends / Share)
        binding.btnSocialTask.setOnClickListener {
            triggerSocialTaskReward()
        }
    }

    private fun refreshUI() {
        val ctx = requireContext()

        // Load Avatar & Full Name
        val avatarUrl = ProfileSession.getAvatar(ctx)
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)

        val fullName = ProfileSession.getFullName(ctx)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingWord = when (hour) {
            in 5..10 -> "Chào buổi sáng"
            in 11..13 -> "Chào buổi trưa"
            in 14..17 -> "Chào buổi chiều"
            else -> "Chào buổi tối"
        }
        binding.tvUserGreeting.text = "$greetingWord, $fullName"

        // Load dynamic reward points count from Room database
        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getTotalPointsFlow().collect { points ->
                val totalPoints = points ?: 0
                binding.tvUserCoins.text = "$totalPoints ROOTIE COINS"
            }
        }

        // 1. Load Streak Days
        val currentStreak = ProfileSession.getSkinStreak(ctx)
        binding.tvStreakDays.text = currentStreak.toString()

        // Update dynamic motivation message based on streak days
        val motivationMsg = when (currentStreak) {
            0 -> "Hãy bắt đầu ngày đầu tiên để chăm sóc làn da của bạn nhé! ✨"
            1 -> "Khởi đầu tuyệt vời! Hãy duy trì chuỗi chăm sóc da ngày mai nhé! 🌱"
            in 2..4 -> "Tuyệt vời! Bạn đang dần hình thành thói quen chăm da tốt đó! 👏"
            in 5..6 -> "Cố lên! Sắp đạt chuỗi 7 ngày để nhận thưởng lớn rồi! 🔥"
            else -> "Bạn đang duy trì thói quen rất tốt cho làn da của mình! Rất tự hào về bạn! 🌟"
        }
        binding.tvStreakMotivation.text = motivationMsg

        // 2. Render Current Week's Calendar Status (Mon to Sun)
        updateWeekCalendar()

        // 3. Render Morning Routine Card Status
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val hasCompletedMorningToday = ProfileSession.isMorningRewardAwarded(ctx, todayStr)
        val isMorningSubmitted = ProfileSession.isRoutineSubmitted(ctx, "morning", todayStr)

        val rawMorningSteps = ProfileSession.getMorningSteps(ctx)
        val activeMorningStepsCount = rawMorningSteps.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 4 && parts[3].toBoolean()) 1 else null
        }.size

        val completedMorningStepsCount = if (hasCompletedMorningToday || isMorningSubmitted) {
            activeMorningStepsCount
        } else {
            val completedStepIds = ProfileSession.getCompletedStepIdsForDate(ctx, todayStr)
            completedStepIds.count { it.startsWith("morning_") }
        }

        val morningProgressPercentage = if (activeMorningStepsCount > 0) {
            (completedMorningStepsCount * 100) / activeMorningStepsCount
        } else {
            0
        }

        binding.tvMorningProgressText.text = "$completedMorningStepsCount/$activeMorningStepsCount BƯỚC"
        binding.viewMorningProgressBar.progress = morningProgressPercentage



        if (isMorningSubmitted) {
            binding.tvMorningHeaderStatus.text = "SÁNG NAY • ĐÃ HOÀN THÀNH"
            binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#677559"))
            binding.btnStartMorningRoutine.text = "Đã hoàn thành"
            binding.btnStartMorningRoutine.isEnabled = true
            binding.btnStartMorningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.quiz_ic_selected, 0, 0, 0)

            // Update Morning Reward Item Status (Keep "+ 10 xu" & "HÀNG NGÀY", only fade left details if not awarded)
            binding.tvMorningRewardXu.text = "+ 10 xu"
            binding.tvMorningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvMorningRewardFrequency.text = "HÀNG NGÀY"
            binding.tvMorningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutMorningReward.alpha = 1.0f
            binding.layoutMorningReward.getChildAt(0)?.alpha = 0.5f
            binding.layoutMorningReward.getChildAt(1)?.alpha = 0.5f
            binding.layoutMorningReward.getChildAt(2)?.alpha = if (hasCompletedMorningToday) 1.0f else 0.5f
        } else {
            if (hour < 6) {
                // Not yet time
                binding.tvMorningHeaderStatus.text = "SÁNG NAY • CHƯA ĐẾN GIỜ"
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"))
                binding.btnStartMorningRoutine.text = "Chưa đến giờ"
                binding.btnStartMorningRoutine.isEnabled = true
                binding.btnStartMorningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_skin_routine_white, 0, 0, 0)

                // Reward status
                binding.tvMorningRewardXu.text = "+ 10 xu"
                binding.tvMorningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
                binding.tvMorningRewardFrequency.text = "HÀNG NGÀY"
                binding.tvMorningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
                binding.layoutMorningReward.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(0)?.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(1)?.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(2)?.alpha = 1.0f
            } else if (hour >= 11) {
                // Missed morning routine
                binding.tvMorningHeaderStatus.text = "SÁNG NAY • ĐÃ BỎ LỠ"
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#FF3B30"))
                binding.btnStartMorningRoutine.text = "Đã bỏ lỡ"
                binding.btnStartMorningRoutine.isEnabled = true
                binding.btnStartMorningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_skin_routine_white, 0, 0, 0)

                // Gray out Morning Reward Item
                binding.tvMorningRewardXu.text = "+ 10 xu"
                binding.tvMorningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
                binding.tvMorningRewardFrequency.text = "HÀNG NGÀY"
                binding.tvMorningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
                binding.layoutMorningReward.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(0)?.alpha = 0.5f
                binding.layoutMorningReward.getChildAt(1)?.alpha = 0.5f
                binding.layoutMorningReward.getChildAt(2)?.alpha = 0.5f
            } else {
                if (completedMorningStepsCount > 0) {
                    binding.tvMorningHeaderStatus.text = "SÁNG NAY • ĐANG THỰC HIỆN"
                    binding.btnStartMorningRoutine.text = "Tiếp tục Routine"
                } else {
                    binding.tvMorningHeaderStatus.text = "SÁNG NAY • CHƯA BẮT ĐẦU"
                    binding.btnStartMorningRoutine.text = "Bắt đầu Routine"
                }
                binding.tvMorningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"))
                binding.btnStartMorningRoutine.isEnabled = true
                binding.btnStartMorningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_skin_routine_white, 0, 0, 0)

                // Update Morning Reward Item Status
                binding.tvMorningRewardXu.text = "+ 10 xu"
                binding.tvMorningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
                binding.tvMorningRewardFrequency.text = "HÀNG NGÀY"
                binding.tvMorningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
                binding.layoutMorningReward.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(0)?.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(1)?.alpha = 1.0f
                binding.layoutMorningReward.getChildAt(2)?.alpha = 1.0f
            }
        }

        // 4. Render Evening Routine Card Status
        val isEveningActive = hour >= 18 || hour < 2
        val eveningTargetDate = if (hour < 2) {
            val prevCal = Calendar.getInstance()
            prevCal.add(Calendar.DAY_OF_YEAR, -1)
            sdf.format(prevCal.time)
        } else {
            todayStr
        }
        val hasCompletedEveningToday = ProfileSession.isEveningRewardAwarded(ctx, eveningTargetDate)
        val isEveningSubmitted = ProfileSession.isRoutineSubmitted(ctx, "evening", eveningTargetDate)

        val rawEveningSteps = ProfileSession.getEveningSteps(ctx)
        val activeEveningStepsCount = rawEveningSteps.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 4 && parts[3].toBoolean()) 1 else null
        }.size

        val completedEveningStepsCount = if (hasCompletedEveningToday || isEveningSubmitted) {
            activeEveningStepsCount
        } else {
            val completedStepIds = ProfileSession.getCompletedStepIdsForDate(ctx, eveningTargetDate)
            completedStepIds.count { it.startsWith("evening_") }
        }

        val eveningProgressPercentage = if (activeEveningStepsCount > 0) {
            (completedEveningStepsCount * 100) / activeEveningStepsCount
        } else {
            0
        }

        binding.tvEveningProgressText.text = "$completedEveningStepsCount/$activeEveningStepsCount BƯỚC"
        binding.viewEveningProgressBar.progress = eveningProgressPercentage

        if (isEveningSubmitted) {
            if (hour < 2) {
                binding.tvEveningHeaderStatus.text = "TỐI QUA • ĐÃ HOÀN THÀNH"
            } else {
                binding.tvEveningHeaderStatus.text = "TỐI NAY • ĐÃ HOÀN THÀNH"
            }
            binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#677559"))
            binding.btnStartEveningRoutine.text = "Đã hoàn thành"
            binding.btnStartEveningRoutine.isEnabled = true
            binding.btnStartEveningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.quiz_ic_selected, 0, 0, 0)

            // Update Evening Reward Item Status (Keep "+ 10 xu" & "HÀNG NGÀY", only fade left details if not awarded)
            binding.tvEveningRewardXu.text = "+ 10 xu"
            binding.tvEveningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvEveningRewardFrequency.text = "HÀNG NGÀY"
            binding.tvEveningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutEveningReward.alpha = 1.0f
            binding.layoutEveningReward.getChildAt(0)?.alpha = 0.5f
            binding.layoutEveningReward.getChildAt(1)?.alpha = 0.5f
            binding.layoutEveningReward.getChildAt(2)?.alpha = if (hasCompletedEveningToday) 1.0f else 0.5f
        } else {
            if (isEveningActive) {
                val prefixStr = if (hour < 2) "TỐI QUA" else "TỐI NAY"
                if (completedEveningStepsCount > 0) {
                    binding.tvEveningHeaderStatus.text = "$prefixStr • ĐANG THỰC HIỆN"
                    binding.btnStartEveningRoutine.text = "Tiếp tục Routine"
                } else {
                    binding.tvEveningHeaderStatus.text = "$prefixStr • CHƯA BẮT ĐẦU"
                    binding.btnStartEveningRoutine.text = "Bắt đầu Routine"
                }
                binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"))
                binding.btnStartEveningRoutine.isEnabled = true
                binding.btnStartEveningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_skin_routine_white, 0, 0, 0)

                // Update Evening Reward Item Status
                binding.tvEveningRewardXu.text = "+ 10 xu"
                binding.tvEveningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
                binding.tvEveningRewardFrequency.text = "HÀNG NGÀY"
                binding.tvEveningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
                binding.layoutEveningReward.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(0)?.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(1)?.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(2)?.alpha = 1.0f
            } else {
                // Outside evening routine hours. Previous night's routine is missed, tonight's hasn't started.
                binding.tvEveningHeaderStatus.text = "TỐI NAY • CHƯA ĐẾN GIỜ"
                binding.tvEveningHeaderStatus.setTextColor(Color.parseColor("#3E4D44"))
                binding.btnStartEveningRoutine.text = "Chưa đến giờ"
                binding.btnStartEveningRoutine.isEnabled = true
                binding.btnStartEveningRoutine.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_skin_routine_white, 0, 0, 0)

                // Update Evening Reward Item Status
                binding.tvEveningRewardXu.text = "+ 10 xu"
                binding.tvEveningRewardXu.setTextColor(Color.parseColor("#FFCC00"))
                binding.tvEveningRewardFrequency.text = "HÀNG NGÀY"
                binding.tvEveningRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
                binding.layoutEveningReward.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(0)?.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(1)?.alpha = 1.0f
                binding.layoutEveningReward.getChildAt(2)?.alpha = 1.0f
            }
        }

        // 5. Update Weekly Bonus Reward Item Status
        if (currentStreak >= 7) {
            binding.tvWeeklyRewardXu.text = "+ 50 xu"
            binding.tvWeeklyRewardXu.setTextColor(Color.parseColor("#E1C02E"))
            binding.tvWeeklyRewardFrequency.text = "HÀNG TUẦN"
            binding.tvWeeklyRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutWeeklyReward.alpha = 1.0f
            binding.layoutWeeklyReward.getChildAt(0)?.alpha = 0.5f
            binding.layoutWeeklyReward.getChildAt(1)?.alpha = 0.5f
            binding.layoutWeeklyReward.getChildAt(2)?.alpha = 1.0f
        } else {
            binding.tvWeeklyRewardXu.text = "+ 50 xu"
            binding.tvWeeklyRewardXu.setTextColor(Color.parseColor("#E1C02E"))
            binding.tvWeeklyRewardFrequency.text = "HÀNG TUẦN"
            binding.tvWeeklyRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutWeeklyReward.alpha = 1.0f
            binding.layoutWeeklyReward.getChildAt(0)?.alpha = 1.0f
            binding.layoutWeeklyReward.getChildAt(1)?.alpha = 1.0f
            binding.layoutWeeklyReward.getChildAt(2)?.alpha = 1.0f
        }

        // 6. Update Loyalty Reward Item Status
        if (currentStreak >= 30) {
            binding.tvLoyaltyRewardXu.text = "+ 200 xu"
            binding.tvLoyaltyRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvLoyaltyRewardFrequency.text = "HÀNG THÁNG"
            binding.tvLoyaltyRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutLoyaltyReward.alpha = 1.0f
            binding.layoutLoyaltyReward.getChildAt(0)?.alpha = 0.5f
            binding.layoutLoyaltyReward.getChildAt(1)?.alpha = 0.5f
            binding.layoutLoyaltyReward.getChildAt(2)?.alpha = 1.0f
        } else {
            binding.tvLoyaltyRewardXu.text = "+ 200 xu"
            binding.tvLoyaltyRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvLoyaltyRewardFrequency.text = "HÀNG THÁNG"
            binding.tvLoyaltyRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.layoutLoyaltyReward.alpha = 1.0f
            binding.layoutLoyaltyReward.getChildAt(0)?.alpha = 1.0f
            binding.layoutLoyaltyReward.getChildAt(1)?.alpha = 1.0f
            binding.layoutLoyaltyReward.getChildAt(2)?.alpha = 1.0f
        }

        // 7. Update Social Task Reward Item Status
        val completedSocialDates = ProfileSession.getSkinSocialCompletedDates(ctx)
        val hasCompletedSocialToday = completedSocialDates.contains(todayStr)
        if (hasCompletedSocialToday) {
            binding.tvSocialRewardXu.text = "+ 10 xu"
            binding.tvSocialRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvSocialRewardFrequency.text = "MỖI LƯỢT"
            binding.tvSocialRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.btnSocialTask.alpha = 1.0f
            binding.btnSocialTask.isEnabled = false
            binding.btnSocialTask.getChildAt(0)?.alpha = 0.5f
            binding.btnSocialTask.getChildAt(1)?.alpha = 0.5f
            binding.btnSocialTask.getChildAt(2)?.alpha = 1.0f
        } else {
            binding.tvSocialRewardXu.text = "+ 10 xu"
            binding.tvSocialRewardXu.setTextColor(Color.parseColor("#FFCC00"))
            binding.tvSocialRewardFrequency.text = "MỖI LƯỢT"
            binding.tvSocialRewardFrequency.setTextColor(Color.parseColor("#AEAEB2"))
            binding.btnSocialTask.alpha = 1.0f
            binding.btnSocialTask.isEnabled = true
            binding.btnSocialTask.getChildAt(0)?.alpha = 1.0f
            binding.btnSocialTask.getChildAt(1)?.alpha = 1.0f
            binding.btnSocialTask.getChildAt(2)?.alpha = 1.0f
        }
    }

    private fun getRoutineDate(type: String): String {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        if (type == "evening" && hour < 2) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return sdf.format(calendar.time)
    }

    private fun isWithinTimeWindow(type: String): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (type == "morning") {
            hour in 6..10
        } else {
            hour >= 18 || hour < 2
        }
    }

    private fun updateWeekCalendar() {
        val ctx = requireContext()
        binding.layoutDaysContainer.removeAllViews()

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val completedMornings = ProfileSession.getCompletedMorningDates(ctx)
        val completedEvenings = ProfileSession.getCompletedEveningDates(ctx)

        // Show 14 days (current week + next week)
        for (i in 0 until 14) {
            val dateStr = sdf.format(calendar.time)
            val isCompleted = completedMornings.contains(dateStr) && completedEvenings.contains(dateStr)
            val isToday = dateStr == todayStr

            val itemBinding = com.veganbeauty.app.databinding.ItemStreakDayBinding.inflate(
                LayoutInflater.from(ctx),
                binding.layoutDaysContainer,
                false
            )

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayLabel = when (dayOfWeek) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }

            itemBinding.tvDayLabel.text = dayLabel
            itemBinding.tvDayNum.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            if (isToday) {
                itemBinding.tvDayLabel.setTextColor(Color.WHITE)
                itemBinding.tvDayLabel.alpha = 1.0f
            } else {
                itemBinding.tvDayLabel.setTextColor(Color.parseColor("#80FFFFFF"))
            }

            if (isCompleted) {
                itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.bg_circle_white)
                itemBinding.ivDayIcon.setImageResource(R.drawable.ic_check)
                itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#3E4D44")) // Dark green check
            } else {
                if (isToday) {
                    itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.bg_circle_today_border)
                    itemBinding.ivDayIcon.setImageResource(R.drawable.ic_calendar_outline)
                    itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#E05D3B")) // Orange-red for today
                } else {
                    itemBinding.layoutIconContainer.setBackgroundResource(R.drawable.bg_circle_white_border)
                    itemBinding.ivDayIcon.setImageResource(R.drawable.ic_calendar_outline)
                    itemBinding.ivDayIcon.setColorFilter(Color.parseColor("#B3FFFFFF")) // 70% white
                }
            }

            binding.layoutDaysContainer.addView(itemBinding.root)

            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun completeMorningRoutine() {
        val ctx = requireContext()
        val targetDate = getRoutineDate("morning")

        ProfileSession.addCompletedMorningDate(ctx, targetDate)

        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().insertRewardPoints(
                RewardPointEntity(
                    orderId = "MORNING_ROUTINE",
                    points = 10,
                    reason = "Hoàn thành Routine Sáng",
                    timestamp = System.currentTimeMillis()
                )
            )
            Toast.makeText(ctx, "Đã hoàn thành Routine Sáng! +10 xu", Toast.LENGTH_SHORT).show()
            checkStreakAndUpdate("morning")
            refreshUI()
        }
    }

    private fun completeEveningRoutine() {
        val ctx = requireContext()
        val targetDate = getRoutineDate("evening")

        ProfileSession.addCompletedEveningDate(ctx, targetDate)

        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().insertRewardPoints(
                RewardPointEntity(
                    orderId = "EVENING_ROUTINE",
                    points = 10,
                    reason = "Hoàn thành Routine Tối",
                    timestamp = System.currentTimeMillis()
                )
            )
            Toast.makeText(ctx, "Đã hoàn thành Routine Tối! +10 xu", Toast.LENGTH_SHORT).show()
            checkStreakAndUpdate("evening")
            refreshUI()
        }
    }

    private fun triggerSocialTaskReward() {
        val ctx = requireContext()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val completedSocialDates = ProfileSession.getSkinSocialCompletedDates(ctx)

        if (completedSocialDates.contains(todayStr)) {
            Toast.makeText(ctx, "Bạn đã nhận phần thưởng liên kết hôm nay rồi!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().insertRewardPoints(
                RewardPointEntity(
                    orderId = "SOCIAL_TASK",
                    points = 10,
                    reason = "Kết nối bạn bè (Social Task)",
                    timestamp = System.currentTimeMillis()
                )
            )
            ProfileSession.addSkinSocialCompletedDate(ctx, todayStr)
            Toast.makeText(ctx, "Kết nối thành công! +10 xu", Toast.LENGTH_SHORT).show()
            refreshUI()
        }
    }

    private suspend fun checkStreakAndUpdate(type: String) {
        val ctx = requireContext()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = getRoutineDate(type)

        val completedMornings = ProfileSession.getCompletedMorningDates(ctx)
        val completedEvenings = ProfileSession.getCompletedEveningDates(ctx)

        // Check if both routines are completed today
        if (completedMornings.contains(targetDate) && completedEvenings.contains(targetDate)) {
            val lastCompletedStr = ProfileSession.getSkinLastCompletedDate(ctx)
            if (lastCompletedStr == targetDate) {
                return // Already updated today
            }

            val currentStreak = ProfileSession.getSkinStreak(ctx)
            val newStreak: Int

            if (lastCompletedStr.isNotEmpty()) {
                val lastCal = Calendar.getInstance()
                lastCal.time = sdf.parse(lastCompletedStr) ?: Date()
                
                // Add 1 day to last completed date
                lastCal.add(Calendar.DAY_OF_YEAR, 1)
                val expectedYesterdayStr = sdf.format(lastCal.time)

                if (expectedYesterdayStr == targetDate) {
                    newStreak = currentStreak + 1
                } else {
                    newStreak = 1 // Streak broken
                }
            } else {
                newStreak = 1
            }

            ProfileSession.setSkinStreak(ctx, newStreak)
            ProfileSession.setSkinLastCompletedDate(ctx, targetDate)

            val db = RootieDatabase.getDatabase(ctx)
            if (newStreak % 30 == 0) {
                db.rewardPointDao().insertRewardPoints(
                    RewardPointEntity(
                        orderId = "MONTHLY_STREAK",
                        points = 200,
                        reason = "Thưởng chuỗi 30 ngày chăm da",
                        timestamp = System.currentTimeMillis()
                    )
                )
                Toast.makeText(ctx, "Tuyệt vời! Đạt chuỗi 30 ngày chăm da +200 xu!", Toast.LENGTH_LONG).show()
            } else if (newStreak % 7 == 0) {
                db.rewardPointDao().insertRewardPoints(
                    RewardPointEntity(
                        orderId = "WEEKLY_STREAK",
                        points = 50,
                        reason = "Thưởng chuỗi 7 ngày chăm da",
                        timestamp = System.currentTimeMillis()
                    )
                )
                Toast.makeText(ctx, "Tuyệt vời! Đạt chuỗi 7 ngày chăm da +50 xu!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
