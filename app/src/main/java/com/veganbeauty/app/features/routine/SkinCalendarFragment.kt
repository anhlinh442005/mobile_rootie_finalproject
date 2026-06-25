package com.veganbeauty.app.features.routine

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.SkinCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class SkinCalendarFragment : RootieFragment() {

    private var _binding: SkinCalendarBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private val sdfYearMonth = SimpleDateFormat("'Tháng' MM, yyyy", Locale("vi", "VN"))
    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        if (_binding == null) return
        val ctx = requireContext()

        // 1. Header Navigation
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Load Avatar
        val avatarUrl = ProfileSession.getAvatar(ctx)
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)

        // 2. Load Streaks & Stats
        val morningDates = ProfileSession.getCompletedMorningDates(ctx)
        val eveningDates = ProfileSession.getCompletedEveningDates(ctx)

        val currentStreak = calculateCurrentStreak(morningDates, eveningDates)
        binding.tvCurrentStreak.text = currentStreak.toString()
        ProfileSession.setSkinStreak(ctx, currentStreak)

        val maxStreak = calculateMaxStreak(morningDates, eveningDates)
        val prefs = ctx.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("skin_max_streak", maxStreak).apply()
        binding.tvMaxStreak.text = "$maxStreak ngày"

        val unionDates = morningDates.union(eveningDates)
        val totalCompletedDays = unionDates.size
        binding.tvTotalCompletedDays.text = "$totalCompletedDays ngày"

        val pillText = when (currentStreak) {
            0 -> "Bắt đầu chuỗi chăm da ngay hôm nay nhé!"
            1 -> "Vượt 10% người dùng khác. Bạn hãy duy trì nhé!"
            2 -> "Vượt 25% người dùng khác. Bạn hãy duy trì nhé!"
            3 -> "Vượt 40% người dùng khác. Bạn hãy duy trì nhé!"
            4 -> "Vượt 55% người dùng khác. Bạn hãy duy trì nhé!"
            5 -> "Vượt 70% người dùng khác. Bạn hãy duy trì nhé!"
            6 -> "Vượt 80% người dùng khác. Bạn hãy duy trì nhé!"
            in 7..13 -> "Vượt 90% người dùng khác. Bạn hãy duy trì nhé!"
            in 14..29 -> "Vượt 95% người dùng khác. Bạn hãy duy trì nhé!"
            else -> "Vượt 99% người dùng khác. Bạn hãy duy trì nhé!"
        }
        binding.tvStreakPillText.text = pillText

        // 3. Habit Analysis (Dynamic)
        calculateHabitAnalysis(ctx)

        // 4. Render Calendar Month
        renderCalendar(ctx)

        binding.btnPrevMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            renderCalendar(ctx)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            renderCalendar(ctx)
        }

        // 5. Tabs/Filters Navigation
        setupTabs(ctx)

        // 6. Populate Timeline Log
        populateTimeline(ctx)

        // 7. Dynamic Detailed History button text
        val currentMonthFormat = SimpleDateFormat("'Nhật ký chi tiết tháng' M", Locale("vi", "VN"))
        binding.btnDetailedHistory.text = currentMonthFormat.format(Date())
    }


    private fun calculateMaxStreak(morningDates: Set<String>, eveningDates: Set<String>): Int {
        val completedDaysSet = morningDates.intersect(eveningDates).mapNotNull {
            try {
                sdfDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }.sorted()

        if (completedDaysSet.isEmpty()) return 0

        var maxStreak = 0
        var currentStreak = 0
        var prevDate: Date? = null

        val msInDay = 24 * 60 * 60 * 1000L

        for (date in completedDaysSet) {
            if (prevDate == null) {
                currentStreak = 1
            } else {
                val diffMs = date.time - prevDate.time
                val diffDays = Math.round(diffMs.toDouble() / msInDay).toInt()
                if (diffDays == 1) {
                    currentStreak++
                } else if (diffDays > 1) {
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                    currentStreak = 1
                }
            }
            prevDate = date
        }
        if (currentStreak > maxStreak) {
            maxStreak = currentStreak
        }
        return maxStreak
    }

    private fun calculateCurrentStreak(morningDates: Set<String>, eveningDates: Set<String>): Int {
        val completedDaysSet = morningDates.intersect(eveningDates)
        if (completedDaysSet.isEmpty()) return 0

        val todayStr = sdfDate.format(Date())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdfDate.format(yesterdayCal.time)

        if (!completedDaysSet.contains(todayStr) && !completedDaysSet.contains(yesterdayStr)) {
            return 0
        }

        var streak = 0
        val checkCal = Calendar.getInstance()
        var dateStr = sdfDate.format(checkCal.time)
        
        if (!completedDaysSet.contains(dateStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            dateStr = sdfDate.format(checkCal.time)
        }

        while (completedDaysSet.contains(dateStr)) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            dateStr = sdfDate.format(checkCal.time)
        }
        return streak
    }

    private fun calculateHabitAnalysis(ctx: Context) {
        val morningDates = ProfileSession.getCompletedMorningDates(ctx)
        val eveningDates = ProfileSession.getCompletedEveningDates(ctx)

        var morningCount = 0
        var eveningCount = 0

        val tempCal = Calendar.getInstance()
        for (i in 0 until 30) {
            val dateStr = sdfDate.format(tempCal.time)
            if (ProfileSession.isMorningRewardAwarded(ctx, dateStr) || morningDates.contains(dateStr)) {
                morningCount++
            }
            if (ProfileSession.isEveningRewardAwarded(ctx, dateStr) || eveningDates.contains(dateStr)) {
                eveningCount++
            }
            tempCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val morningRate = (morningCount * 100) / 30
        val eveningRate = (eveningCount * 100) / 30

        if (morningCount == 0 && eveningCount == 0) {
            binding.tvHabitAnalysisHeading.text = "Hãy bắt đầu hành trình chăm sóc da của bạn!"
            binding.tvHabitAnalysisSub.text = "Chưa có dữ liệu hoàn thành routine nào trong 30 ngày qua."
            binding.tvHabitTip.text = "Hãy thử đặt báo thức nhắc nhở và chuẩn bị sẵn các sản phẩm skincare ở vị trí dễ thấy nhé!"
        } else if (morningRate == eveningRate) {
            binding.tvHabitAnalysisHeading.text = "Thói quen chăm da của bạn cực kỳ cân bằng và đều đặn!"
            binding.tvHabitAnalysisSub.text = "Tỷ lệ hoàn thành cả sáng và tối đều đạt $morningRate%"
            binding.tvHabitTip.text = "Bạn đang làm rất tốt! Hãy tiếp tục duy trì nhịp điệu tuyệt vời này nhé!"
        } else if (morningRate > eveningRate) {
            binding.tvHabitAnalysisHeading.text = "Bạn thường hoàn thành Routine buổi sáng tốt hơn buổi tối."
            binding.tvHabitAnalysisSub.text = "Tỷ lệ hoàn thành buổi sáng đạt $morningRate%, buổi tối đạt $eveningRate%"
            binding.tvHabitTip.text = "Hãy thử đặt báo thức skincare tối sớm hơn."
        } else {
            binding.tvHabitAnalysisHeading.text = "Bạn thường hoàn thành Routine buổi tối tốt hơn buổi sáng."
            binding.tvHabitAnalysisSub.text = "Tỷ lệ hoàn thành buổi tối đạt $eveningRate%, buổi sáng chỉ đạt $morningRate%"
            binding.tvHabitTip.text = "Hãy thử chuẩn bị đồ skincare từ tối hôm trước lên bàn trang điểm để dễ duy trì buổi sáng."
        }
    }

    private fun renderCalendar(ctx: Context) {
        binding.tvMonthYear.text = sdfYearMonth.format(calendar.time)
        binding.layoutCalendarGrid.removeAllViews()

        val tempCal = calendar.clone() as Calendar
        tempCal.set(Calendar.DAY_OF_MONTH, 1)

        val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
        val leadDays = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // We will add cells to rows
        var rowLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        binding.layoutCalendarGrid.addView(rowLayout)

        // Lead empty days from previous month
        for (i in 0 until leadDays) {
            val cell = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            rowLayout.addView(cell)
        }

        val todayStr = sdfDate.format(Date())
        val morningDates = ProfileSession.getCompletedMorningDates(ctx)
        val eveningDates = ProfileSession.getCompletedEveningDates(ctx)

        for (day in 1..maxDays) {
            if (rowLayout.childCount == 7) {
                rowLayout = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        val density = ctx.resources.displayMetrics.density
                        topMargin = (12 * density).toInt()
                    }
                }
                binding.layoutCalendarGrid.addView(rowLayout)
            }

            val cell = LayoutInflater.from(ctx).inflate(R.layout.item_skin_calendar_day, rowLayout, false)
            cell.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            
            val tvDayNum = cell.findViewById<TextView>(R.id.tvDayNum)
            val viewDot = cell.findViewById<View>(R.id.viewDot)

            tvDayNum.text = day.toString()

            tempCal.set(Calendar.DAY_OF_MONTH, day)
            val cellDateStr = sdfDate.format(tempCal.time)

            // Highlight today
            if (cellDateStr == todayStr) {
                tvDayNum.setTextColor(Color.WHITE)
                tvDayNum.setBackgroundResource(R.drawable.bg_circle_dark_olive)
            } else {
                tvDayNum.setTextColor(Color.parseColor("#3E4D44"))
                tvDayNum.background = null
            }

            // Dot coloring based on activity
            val isMorningDone = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr) || morningDates.contains(cellDateStr)
            val isEveningDone = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr) || eveningDates.contains(cellDateStr)

            val isMorningFull = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr)
            val isEveningFull = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr)

            if (tempCal.time.after(Date())) {
                // Future day
                viewDot.visibility = View.INVISIBLE
            } else {
                viewDot.visibility = View.VISIBLE
                if (isMorningFull && isEveningFull) {
                    viewDot.setBackgroundResource(R.drawable.bg_circle_green_indicator) // Green (Đủ)
                } else if (isMorningDone || isEveningDone) {
                    viewDot.setBackgroundResource(R.drawable.bg_circle_yellow_indicator) // Yellow (Một phần)
                } else {
                    viewDot.setBackgroundResource(R.drawable.bg_circle_red_indicator) // Red (Bỏ lỡ)
                }
            }

            rowLayout.addView(cell)
        }

        // Fill remaining spaces in last row
        while (rowLayout.childCount < 7) {
            val cell = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            }
            rowLayout.addView(cell)
        }
    }

    private fun setupTabs(ctx: Context) {
        binding.btnTabMonth.setOnClickListener {
            selectTab(0)
            populateTimeline(ctx)
        }
        binding.btnTabWeek.setOnClickListener {
            selectTab(1)
            populateTimeline(ctx)
        }
        binding.btnTabToday.setOnClickListener {
            selectTab(2)
            populateTimeline(ctx)
        }
        selectTab(0)
    }

    private var selectedTab = 0

    private fun selectTab(index: Int) {
        selectedTab = index
        val ctx = context ?: return
        val activeBg = ContextCompat.getDrawable(ctx, R.drawable.com_bg_btn_dark_green)
        val inactiveBg = ContextCompat.getDrawable(ctx, R.drawable.bg_pill_grey_translucent)
        
        binding.btnTabMonth.background = if (index == 0) activeBg else inactiveBg
        binding.btnTabMonth.setTextColor(if (index == 0) Color.WHITE else Color.parseColor("#3E4D44"))
        binding.btnTabMonth.backgroundTintList = null

        binding.btnTabWeek.background = if (index == 1) activeBg else inactiveBg
        binding.btnTabWeek.setTextColor(if (index == 1) Color.WHITE else Color.parseColor("#3E4D44"))
        binding.btnTabWeek.backgroundTintList = null

        binding.btnTabToday.background = if (index == 2) activeBg else inactiveBg
        binding.btnTabToday.setTextColor(if (index == 2) Color.WHITE else Color.parseColor("#3E4D44"))
        binding.btnTabToday.backgroundTintList = null
    }

    private fun populateTimeline(ctx: Context) {
        binding.layoutTimelineContainer.removeAllViews()

        val listDaysCount = when (selectedTab) {
            0 -> 7
            1 -> 3
            2 -> 1
            else -> 7
        }

        val tempCal = Calendar.getInstance()
        val morningDates = ProfileSession.getCompletedMorningDates(ctx)
        val eveningDates = ProfileSession.getCompletedEveningDates(ctx)

        val formatEnglish = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        val formatVietnamese = SimpleDateFormat("EEEE", Locale("vi", "VN"))

        // Raw steps lists definitions
        val activeMorningStepsRaw = ProfileSession.getMorningSteps(ctx).mapNotNull {
            val parts = it.split(":")
            if (parts.size >= 4) {
                val index = parts[0].toIntOrNull() ?: 0
                val name = parts[1]
                val desc = parts[2]
                val isChecked = parts[3].toBoolean()
                if (isChecked) SkincareStep(index, name, desc) else null
            } else null
        }.sortedBy { it.index }

        val activeEveningStepsRaw = ProfileSession.getEveningSteps(ctx).mapNotNull {
            val parts = it.split(":")
            if (parts.size >= 4) {
                val index = parts[0].toIntOrNull() ?: 0
                val name = parts[1]
                val desc = parts[2]
                val isChecked = parts[3].toBoolean()
                if (isChecked) SkincareStep(index, name, desc) else null
            } else null
        }.sortedBy { it.index }

        for (i in 0 until listDaysCount) {
            val cellDateStr = sdfDate.format(tempCal.time)
            
            val viewDay = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_day, binding.layoutTimelineContainer, false)
            val tvDate = viewDay.findViewById<TextView>(R.id.tvTimelineDate)
            val tvDayOfWeek = viewDay.findViewById<TextView>(R.id.tvTimelineDayOfWeek)

            tvDate.text = formatEnglish.format(tempCal.time)
            tvDayOfWeek.text = formatVietnamese.format(tempCal.time).uppercase(Locale.getDefault())

            val cardMorning = viewDay.findViewById<View>(R.id.cardMorningTimeline)
            val cardEvening = viewDay.findViewById<View>(R.id.cardEveningTimeline)
            val tvMorningBadge = viewDay.findViewById<TextView>(R.id.tvMorningBadge)
            val tvEveningBadge = viewDay.findViewById<TextView>(R.id.tvEveningBadge)
            val layoutMorningSteps = viewDay.findViewById<LinearLayout>(R.id.layoutMorningSteps)
            val layoutEveningSteps = viewDay.findViewById<LinearLayout>(R.id.layoutEveningSteps)
            val tvNoActivityMsg = viewDay.findViewById<TextView>(R.id.tvNoActivityMsg)
            val layoutRewardPill = viewDay.findViewById<View>(R.id.layoutRewardPill)

            val morningTickedIds = ProfileSession.getCompletedStepIdsForDate(ctx, cellDateStr)
            val isMorningRewardAwarded = ProfileSession.isMorningRewardAwarded(ctx, cellDateStr)
            val isEveningRewardAwarded = ProfileSession.isEveningRewardAwarded(ctx, cellDateStr)

            val hasMorningCheckIn = morningDates.contains(cellDateStr) || isMorningRewardAwarded
            val hasEveningCheckIn = eveningDates.contains(cellDateStr) || isEveningRewardAwarded

            // Morning Setup
            if (hasMorningCheckIn) {
                cardMorning.visibility = View.VISIBLE
                if (isMorningRewardAwarded) {
                    tvMorningBadge.text = "HOÀN THÀNH"
                    tvMorningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_completed)
                    tvMorningBadge.setTextColor(Color.parseColor("#3E4D44"))
                } else {
                    tvMorningBadge.text = "MỘT PHẦN"
                    tvMorningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_partial)
                    tvMorningBadge.setTextColor(Color.parseColor("#F04758"))
                }

                layoutMorningSteps.removeAllViews()
                for (step in activeMorningStepsRaw) {
                    val stepView = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_step, layoutMorningSteps, false)
                    val tvName = stepView.findViewById<TextView>(R.id.tvStepName)
                    val imvCheck = stepView.findViewById<ImageView>(R.id.imvStepCheck)

                    tvName.text = step.name
                    val isTicked = morningTickedIds.contains("morning_${step.index}")
                    if (isTicked) {
                        imvCheck.setImageResource(R.drawable.ic_check_circle)
                        imvCheck.imageTintList = null
                        tvName.setTextColor(Color.parseColor("#3E4D44"))
                    } else {
                        imvCheck.setImageResource(R.drawable.ic_close_grey)
                        imvCheck.imageTintList = null
                        tvName.setTextColor(Color.parseColor("#AEAEB2"))
                    }
                    layoutMorningSteps.addView(stepView)
                }
            } else {
                cardMorning.visibility = View.GONE
            }

            // Evening Setup
            if (hasEveningCheckIn) {
                cardEvening.visibility = View.VISIBLE
                if (isEveningRewardAwarded) {
                    tvEveningBadge.text = "HOÀN THÀNH"
                    tvEveningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_completed)
                    tvEveningBadge.setTextColor(Color.parseColor("#3E4D44"))
                } else {
                    tvEveningBadge.text = "MỘT PHẦN"
                    tvEveningBadge.setBackgroundResource(R.drawable.bg_timeline_badge_partial)
                    tvEveningBadge.setTextColor(Color.parseColor("#F04758"))
                }

                layoutEveningSteps.removeAllViews()
                for (step in activeEveningStepsRaw) {
                    val stepView = LayoutInflater.from(ctx).inflate(R.layout.item_skin_timeline_step, layoutEveningSteps, false)
                    val tvName = stepView.findViewById<TextView>(R.id.tvStepName)
                    val imvCheck = stepView.findViewById<ImageView>(R.id.imvStepCheck)

                    tvName.text = step.name
                    val isTicked = morningTickedIds.contains("evening_${step.index}")
                    if (isTicked) {
                        imvCheck.setImageResource(R.drawable.ic_check_circle)
                        imvCheck.imageTintList = null
                        tvName.setTextColor(Color.parseColor("#3E4D44"))
                    } else {
                        imvCheck.setImageResource(R.drawable.ic_close_grey)
                        imvCheck.imageTintList = null
                        tvName.setTextColor(Color.parseColor("#AEAEB2"))
                    }
                    layoutEveningSteps.addView(stepView)
                }
            } else {
                cardEvening.visibility = View.GONE
            }

            // No Activity MSG
            if (!hasMorningCheckIn && !hasEveningCheckIn) {
                tvNoActivityMsg.visibility = View.VISIBLE
            } else {
                tvNoActivityMsg.visibility = View.GONE
            }

            // Reward Pill
            if (isMorningRewardAwarded && isEveningRewardAwarded) {
                layoutRewardPill.visibility = View.VISIBLE
            } else {
                layoutRewardPill.visibility = View.GONE
            }

            binding.layoutTimelineContainer.addView(viewDay)
            tempCal.add(Calendar.DAY_OF_YEAR, -1)
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val ctx = requireContext()
            val avatarUrl = ProfileSession.getAvatar(ctx)
            com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class SkincareStep(val index: Int, val name: String, val description: String)
}
