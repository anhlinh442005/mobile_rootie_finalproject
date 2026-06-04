package com.veganbeauty.app.features.account.checkin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.databinding.AccountCheckinFragmentBinding
import com.veganbeauty.app.databinding.ItemCalendarDayBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AccountCheckinFragment : RootieFragment() {

    private var _binding: AccountCheckinFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: RootieDatabase
    private val calendarInstance = Calendar.getInstance()
    private var checkedInDates = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountCheckinFragmentBinding.inflate(inflater, container, false)
        db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back Button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Highlight active tab
        view.findViewById<android.widget.LinearLayout>(R.id.nav_account)?.let { navAccount ->
            val icon = navAccount.getChildAt(0) as? android.widget.ImageView
            val label = navAccount.getChildAt(1) as? android.widget.TextView
            icon?.setColorFilter(android.graphics.Color.parseColor("#677559"))
            label?.setTextColor(android.graphics.Color.parseColor("#677559"))
            label?.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Month Navigation
        binding.btnPrevMonth.setOnClickListener {
            calendarInstance.add(Calendar.MONTH, -1)
            refreshUI()
        }

        binding.btnNextMonth.setOnClickListener {
            calendarInstance.add(Calendar.MONTH, 1)
            refreshUI()
        }

        // Checkin buttons
        val onCheckInClick = View.OnClickListener {
            performCheckIn()
        }
        binding.btnBannerCheckin.setOnClickListener(onCheckInClick)
        binding.btnBottomCheckin.setOnClickListener(onCheckInClick)

        // Initial refresh
        refreshUI()
    }

    private fun refreshUI() {
        lifecycleScope.launch {
            // Fetch points history
            val allHistory = db.rewardPointDao().getAllRewardHistory().first()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            checkedInDates.clear()
            for (item in allHistory) {
                if (item.reason.contains("Điểm danh")) {
                    checkedInDates.add(sdf.format(Date(item.timestamp)))
                }
            }

            // Update Calendar Header
            val month = calendarInstance.get(Calendar.MONTH) + 1
            val year = calendarInstance.get(Calendar.YEAR)
            binding.tvCalendarMonth.text = "Tháng $month, $year"

            // Render Calendar Grid
            updateCalendarGrid(checkedInDates)

            // Calculate Streak
            val streak = calculateStreak(checkedInDates)
            binding.tvStreakCount.text = "$streak/7"

            // Highlight Reward Cards
            highlightRewardCard(streak)

            // Update today status
            val todayStr = sdf.format(Date())
            val hasCheckedInToday = checkedInDates.contains(todayStr)
            if (hasCheckedInToday) {
                // Alert Banner Status
                binding.ivBannerIcon.setImageResource(R.drawable.ic_check)
                binding.ivBannerIcon.setColorFilter(android.graphics.Color.parseColor("#879578"))
                binding.tvBannerText.text = "Đã check-in hôm nay"
                binding.tvBannerText.setTextColor(android.graphics.Color.parseColor("#879578"))
                binding.btnBannerCheckin.visibility = View.GONE
                binding.tvBannerCompletedText.visibility = View.VISIBLE

                // Bottom Action Button Status
                binding.btnBottomCheckin.isEnabled = false
                binding.btnBottomCheckin.text = "Đã điểm danh hôm nay"
                binding.btnBottomCheckin.setBackgroundResource(R.drawable.bg_pill_grey)
                binding.btnBottomCheckin.setTextColor(android.graphics.Color.parseColor("#888888"))
            } else {
                // Alert Banner Status
                binding.ivBannerIcon.setImageResource(R.drawable.ic_info_olive)
                binding.ivBannerIcon.setColorFilter(android.graphics.Color.parseColor("#8A9A3D"))
                binding.tvBannerText.text = "Chưa check-in hôm nay"
                binding.tvBannerText.setTextColor(android.graphics.Color.parseColor("#8A9A3D"))
                binding.btnBannerCheckin.visibility = View.VISIBLE
                binding.tvBannerCompletedText.visibility = View.GONE

                // Bottom Action Button Status
                binding.btnBottomCheckin.isEnabled = true
                binding.btnBottomCheckin.text = "Check-in ngay"
                binding.btnBottomCheckin.setBackgroundResource(R.drawable.bg_btn_checkin_bottom)
                binding.btnBottomCheckin.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            }
        }
    }

    private fun updateCalendarGrid(checkedInDates: Set<String>) {
        val days = mutableListOf<CalendarDay>()
        val cal = calendarInstance.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val dayOfWeekFirst = cal.get(Calendar.DAY_OF_WEEK)
        // Offset: Mon=0, Tue=1 ... Sun=6
        val offset = (dayOfWeekFirst + 5) % 7

        // Blank slots for previous month offset
        for (i in 0 until offset) {
            days.add(CalendarDay(0, false, null, isChecked = false, isToday = false))
        }

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val dateStr = sdf.format(cal.time)
            val isChecked = checkedInDates.contains(dateStr)
            val isToday = dateStr == todayStr
            days.add(CalendarDay(i, true, cal.time, isChecked, isToday))
        }

        binding.rvCalendarDays.adapter = CalendarAdapter(days) { day ->
            if (day.isToday && !day.isChecked) {
                performCheckIn()
            } else if (day.isChecked) {
                Toast.makeText(context, "Bạn đã điểm danh ngày này!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateStreak(checkedInDates: Set<String>): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        val todayStr = sdf.format(todayCal.time)

        val yesterdayCal = Calendar.getInstance()
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(yesterdayCal.time)

        var streak = 0
        val checkCal = Calendar.getInstance()

        if (checkedInDates.contains(todayStr)) {
            while (checkedInDates.contains(sdf.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (checkedInDates.contains(yesterdayStr)) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (checkedInDates.contains(sdf.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        return streak
    }

    private fun highlightRewardCard(streak: Int) {
        val currentMod = streak % 7
        // Reset all to default backgrounds
        binding.cardReward1.setBackgroundResource(R.drawable.bg_reward_card_default)
        binding.cardReward2.setBackgroundResource(R.drawable.bg_reward_card_default)
        binding.cardReward3.setBackgroundResource(R.drawable.bg_reward_card_default)

        if (currentMod == 0 && streak > 0) {
            binding.cardReward3.setBackgroundResource(R.drawable.bg_reward_card_selected)
        } else if (currentMod >= 3) {
            binding.cardReward2.setBackgroundResource(R.drawable.bg_reward_card_selected)
        } else {
            binding.cardReward1.setBackgroundResource(R.drawable.bg_reward_card_selected)
        }
    }

    private fun performCheckIn() {
        lifecycleScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            if (checkedInDates.contains(todayStr)) {
                Toast.makeText(requireContext(), "Bạn đã điểm danh hôm nay rồi!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Calculate new streak
            val yesterdayCal = Calendar.getInstance()
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = sdf.format(yesterdayCal.time)
            
            val isYesterdayChecked = checkedInDates.contains(yesterdayStr)
            val currentStreak = calculateStreak(checkedInDates)
            val newStreak = if (isYesterdayChecked) currentStreak + 1 else 1

            // Determine points
            val pointsAwarded = when {
                newStreak % 7 == 0 -> 200 // 7 days bonus
                newStreak % 7 == 3 -> 50  // 3 days bonus
                else -> 10                // 1-2 days / standard
            }

            // Insert check-in record
            db.rewardPointDao().insertRewardPoints(
                RewardPointEntity(
                    orderId = "DAILY_CHECKIN",
                    points = pointsAwarded,
                    reason = "Điểm danh hàng ngày (${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())})",
                    timestamp = System.currentTimeMillis()
                )
            )

            // Show custom success dialog
            showSuccessDialog(pointsAwarded)

            // Refresh UI
            refreshUI()
        }
    }

    private fun showSuccessDialog(points: Int) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_checkin_success, null)
        builder.setView(dialogView)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialogView.findViewById<android.widget.TextView>(R.id.tvDialogPoints).text = "+$points xu"
        dialogView.findViewById<android.view.View>(R.id.btnDialogDismiss).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper classes
    data class CalendarDay(
        val dayNum: Int,
        val isCurrentMonth: Boolean,
        val date: Date?,
        val isChecked: Boolean,
        val isToday: Boolean
    )

    class CalendarAdapter(
        private val days: List<CalendarDay>,
        private val onDayClick: (CalendarDay) -> Unit
    ) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val day = days[position]
            if (day.dayNum == 0) {
                holder.binding.tvDayNum.text = ""
                holder.binding.viewCheckedBg.visibility = View.GONE
                holder.binding.viewTodayBorder.visibility = View.GONE
                holder.binding.ivTickBadge.visibility = View.GONE
                holder.binding.root.setOnClickListener(null)
            } else {
                holder.binding.tvDayNum.text = day.dayNum.toString()

                if (day.isChecked) {
                    holder.binding.viewCheckedBg.visibility = View.VISIBLE
                    holder.binding.ivTickBadge.visibility = View.VISIBLE
                    holder.binding.viewTodayBorder.visibility = View.GONE
                    holder.binding.tvDayNum.setTextColor(android.graphics.Color.parseColor("#3E4D44"))
                } else if (day.isToday) {
                    holder.binding.viewCheckedBg.visibility = View.GONE
                    holder.binding.ivTickBadge.visibility = View.GONE
                    holder.binding.viewTodayBorder.visibility = View.VISIBLE
                    holder.binding.tvDayNum.setTextColor(android.graphics.Color.parseColor("#E05D3B"))
                } else {
                    holder.binding.viewCheckedBg.visibility = View.GONE
                    holder.binding.ivTickBadge.visibility = View.GONE
                    holder.binding.viewTodayBorder.visibility = View.GONE
                    holder.binding.tvDayNum.setTextColor(android.graphics.Color.parseColor("#333333"))
                }

                holder.binding.root.setOnClickListener {
                    onDayClick(day)
                }
            }
        }

        override fun getItemCount(): Int = days.size
    }
}
