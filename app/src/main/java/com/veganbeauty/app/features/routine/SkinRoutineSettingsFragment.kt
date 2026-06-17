package com.veganbeauty.app.features.routine

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.databinding.SkinRoutineSettingsBinding
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment
import java.util.*

class SkinRoutineSettingsFragment : RootieFragment() {

    private var _binding: SkinRoutineSettingsBinding? = null
    private val binding get() = _binding!!

    private var isMorningTabSelected = true

    // State lists
    private val morningSteps = mutableListOf<SkincareStep>()
    private val eveningSteps = mutableListOf<SkincareStep>()

    private var morningTime = "06:30"
    private var eveningTime = "21:45"

    private var isMorningReminderEnabled = false
    private var isEveningReminderEnabled = false
    private var isLeadReminderEnabled = false

    data class SkincareStep(
        var index: Int,
        var name: String,
        var description: String,
        var isChecked: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinRoutineSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        if (_binding == null) return
        val ctx = requireContext()

            // 1. Navigation Hooks
            binding.btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            binding.btnNotification.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit()
            }

            // AI Suggestion Navigate to SkinAiChatFragment
            binding.btnAiSuggestion.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, com.veganbeauty.app.features.ai.SkinAiChatFragment())
                    .addToBackStack(null)
                    .commit()
            }

            // 2. Load settings state
            isMorningReminderEnabled = ProfileSession.isMorningReminderEnabled(ctx)
            isEveningReminderEnabled = ProfileSession.isEveningReminderEnabled(ctx)
            isLeadReminderEnabled = ProfileSession.isLeadReminderEnabled(ctx)

            fun updateSwitchUI(container: android.view.ViewGroup, thumb: android.widget.ImageView, enabled: Boolean) {
                if (enabled) {
                    container.setBackgroundResource(R.drawable.ic_switch_track_on)
                    val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
                    lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                    lp.marginStart = 0
                    lp.marginEnd = (2 * resources.displayMetrics.density).toInt()
                    thumb.layoutParams = lp
                } else {
                    container.setBackgroundResource(R.drawable.ic_switch_track_off)
                    val lp = thumb.layoutParams as android.widget.FrameLayout.LayoutParams
                    lp.gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                    lp.marginEnd = 0
                    lp.marginStart = (2 * resources.displayMetrics.density).toInt()
                    thumb.layoutParams = lp
                }
            }

            updateSwitchUI(binding.switchMorningReminderContainer, binding.switchMorningReminderThumb, isMorningReminderEnabled)
            updateSwitchUI(binding.switchEveningReminderContainer, binding.switchEveningReminderThumb, isEveningReminderEnabled)
            updateSwitchUI(binding.switchLeadReminderContainer, binding.switchLeadReminderThumb, isLeadReminderEnabled)

            binding.switchMorningReminderContainer.setOnClickListener {
                isMorningReminderEnabled = !isMorningReminderEnabled
                updateSwitchUI(binding.switchMorningReminderContainer, binding.switchMorningReminderThumb, isMorningReminderEnabled)
            }
            binding.switchEveningReminderContainer.setOnClickListener {
                isEveningReminderEnabled = !isEveningReminderEnabled
                updateSwitchUI(binding.switchEveningReminderContainer, binding.switchEveningReminderThumb, isEveningReminderEnabled)
            }
            binding.switchLeadReminderContainer.setOnClickListener {
                isLeadReminderEnabled = !isLeadReminderEnabled
                updateSwitchUI(binding.switchLeadReminderContainer, binding.switchLeadReminderThumb, isLeadReminderEnabled)
            }

            morningTime = ProfileSession.getMorningReminderTime(ctx)
            eveningTime = ProfileSession.getEveningReminderTime(ctx)

            // Parse and set times
            val morningParts = morningTime.split(":")
            binding.tvMorningTime.text = formatDisplayTime(
                morningParts.getOrNull(0)?.toIntOrNull() ?: 6,
                morningParts.getOrNull(1)?.toIntOrNull() ?: 30
            )

            val eveningParts = eveningTime.split(":")
            binding.tvEveningTime.text = formatDisplayTime(
                eveningParts.getOrNull(0)?.toIntOrNull() ?: 21,
                eveningParts.getOrNull(1)?.toIntOrNull() ?: 45
            )

            // 3. Time click listeners
            binding.tvMorningTime.setOnClickListener {
                showTimePickerDialog(true)
            }
            binding.tvEveningTime.setOnClickListener {
                showTimePickerDialog(false)
            }

            // 4. Initialize Skincare Steps lists
            loadStepsFromPreferences()

            // Render default morning steps list
            populateStepsList()

            // 5. Tab selector listeners
            binding.tabMorning.setOnClickListener {
                if (!isMorningTabSelected) {
                    isMorningTabSelected = true
                    binding.tabMorning.setBackgroundResource(R.drawable.skin_bg_tab_selected)
                    binding.tabMorning.setTextColor(Color.WHITE)
                    binding.tabEvening.setBackgroundColor(Color.TRANSPARENT)
                    binding.tabEvening.setTextColor(Color.parseColor("#3E4D44"))
                    populateStepsList()
                }
            }

            binding.tabEvening.setOnClickListener {
                if (isMorningTabSelected) {
                    isMorningTabSelected = false
                    binding.tabEvening.setBackgroundResource(R.drawable.skin_bg_tab_selected)
                    binding.tabEvening.setTextColor(Color.WHITE)
                    binding.tabMorning.setBackgroundColor(Color.TRANSPARENT)
                    binding.tabMorning.setTextColor(Color.parseColor("#3E4D44"))
                    populateStepsList()
                }
            }

            // 6. Add step button listener
            binding.btnAddStep.setOnClickListener {
                showAddStepDialog()
            }

            // 7. Save settings action listener
            binding.btnSaveConfig.setOnClickListener {
                saveConfiguration()
            }
    }

    private fun loadStepsFromPreferences() {
        val ctx = requireContext()
        val morningRaw = ProfileSession.getMorningSteps(ctx)
        val eveningRaw = ProfileSession.getEveningSteps(ctx)

        morningSteps.clear()
        morningSteps.addAll(parseRawSteps(morningRaw))
        morningSteps.sortBy { it.index }

        eveningSteps.clear()
        eveningSteps.addAll(parseRawSteps(eveningRaw))
        eveningSteps.sortBy { it.index }
    }

    private fun parseRawSteps(rawSet: Set<String>): List<SkincareStep> {
        return rawSet.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 4) {
                SkincareStep(
                    index = parts[0].toIntOrNull() ?: 0,
                    name = parts[1],
                    description = parts[2],
                    isChecked = parts[3].toBoolean()
                )
            } else if (parts.size == 3) {
                // Backward compatibility if no index prefixed
                SkincareStep(
                    index = 99,
                    name = parts[0],
                    description = parts[1],
                    isChecked = parts[2].toBoolean()
                )
            } else {
                null
            }
        }
    }

    private fun populateStepsList() {
        binding.layoutSkincareSteps.removeAllViews()
        val steps = if (isMorningTabSelected) morningSteps else eveningSteps
        steps.forEachIndexed { idx, step ->
            val stepView = LayoutInflater.from(requireContext()).inflate(R.layout.item_routine_step, binding.layoutSkincareSteps, false)
            
            val tvName = stepView.findViewById<TextView>(R.id.tvStepName)
            val tvDesc = stepView.findViewById<TextView>(R.id.tvStepDesc)
            val ivCheckbox = stepView.findViewById<View>(R.id.ivStepCheckbox) as android.widget.ImageView
            val layoutStepCard = stepView.findViewById<View>(R.id.layoutStepCard)

            tvName.text = step.name
            tvDesc.text = step.description

            fun updateCheckboxUI() {
                if (step.isChecked) {
                    ivCheckbox.setImageResource(R.drawable.skin_ic_checkbox_checked)
                    ivCheckbox.background = null
                    ivCheckbox.setPadding(0, 0, 0, 0)
                } else {
                    ivCheckbox.setImageResource(R.drawable.skin_ic_checkbox_unchecked)
                    ivCheckbox.background = null
                    ivCheckbox.setPadding(0, 0, 0, 0)
                }
            }

            updateCheckboxUI()

            ivCheckbox.setOnClickListener {
                step.isChecked = !step.isChecked
                updateCheckboxUI()
            }

            layoutStepCard.setOnClickListener {
                showEditStepDialog(step, idx)
            }

            binding.layoutSkincareSteps.addView(stepView)
        }
    }

    private fun showTimePickerDialog(isMorning: Boolean) {
        val currentTime = if (isMorning) morningTime else eveningTime
        val parts = currentTime.split(":")
        val currentHour = parts.getOrNull(0)?.toIntOrNull() ?: if (isMorning) 6 else 21
        val currentMinute = parts.getOrNull(1)?.toIntOrNull() ?: if (isMorning) 30 else 45

        val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            val displayTime = formatDisplayTime(hourOfDay, minute)
            if (isMorning) {
                morningTime = formattedTime
                binding.tvMorningTime.text = displayTime
            } else {
                eveningTime = formattedTime
                binding.tvEveningTime.text = displayTime
            }
        }

        TimePickerDialog(
            requireContext(),
            timeSetListener,
            currentHour,
            currentMinute,
            false
        ).show()
    }

    private fun formatDisplayTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%02d : %02d %s", displayHour, minute, amPm)
    }

    private fun showAddStepDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_step, null)
        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = view.findViewById<EditText>(R.id.etStepName)
        val etDesc = view.findViewById<EditText>(R.id.etStepDesc)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirm)

        tvDialogTitle.text = "Thêm bước dưỡng da mới"
        btnConfirm.text = "Thêm"

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            if (name.isNotEmpty() && desc.isNotEmpty()) {
                val list = if (isMorningTabSelected) morningSteps else eveningSteps
                val newIndex = if (list.isEmpty()) 0 else list.maxOf { it.index } + 1
                list.add(SkincareStep(newIndex, name, desc, true))
                populateStepsList()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showEditStepDialog(step: SkincareStep, indexInList: Int) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_edit_step, null)
        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val etName = view.findViewById<EditText>(R.id.etStepName)
        val etDesc = view.findViewById<EditText>(R.id.etStepDesc)
        val btnDelete = view.findViewById<View>(R.id.btnDelete)
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val btnConfirm = view.findViewById<TextView>(R.id.btnConfirm)

        tvDialogTitle.text = "Chỉnh sửa bước dưỡng da"
        btnConfirm.text = "Lưu"
        btnDelete.visibility = View.VISIBLE

        etName.setText(step.name)
        etDesc.setText(step.description)

        val dialog = AlertDialog.Builder(requireContext()).setView(view).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val list = if (isMorningTabSelected) morningSteps else eveningSteps
            list.removeAt(indexInList)
            populateStepsList()
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val desc = etDesc.text.toString().trim()
            if (name.isNotEmpty() && desc.isNotEmpty()) {
                step.name = name
                step.description = desc
                populateStepsList()
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveConfiguration() {
        val ctx = requireContext()

        // 1. Save Switches
        val morningEnabled = isMorningReminderEnabled
        val eveningEnabled = isEveningReminderEnabled
        val leadEnabled = isLeadReminderEnabled

        ProfileSession.setMorningReminderEnabled(ctx, morningEnabled)
        ProfileSession.setEveningReminderEnabled(ctx, eveningEnabled)
        ProfileSession.setLeadReminderEnabled(ctx, leadEnabled)

        // 2. Save Times
        ProfileSession.setMorningReminderTime(ctx, morningTime)
        ProfileSession.setEveningReminderTime(ctx, eveningTime)

        // 3. Save Steps Lists
        val morningRaw = morningSteps.map { "${it.index}:${it.name}:${it.description}:${it.isChecked}" }.toSet()
        val eveningRaw = eveningSteps.map { "${it.index}:${it.name}:${it.description}:${it.isChecked}" }.toSet()
        ProfileSession.setMorningSteps(ctx, morningRaw)
        ProfileSession.setEveningSteps(ctx, eveningRaw)

        // 4. Update Alarm scheduler
        if (morningEnabled) {
            val morningParts = morningTime.split(":")
            var mHour = morningParts.getOrNull(0)?.toIntOrNull() ?: 6
            var mMinute = morningParts.getOrNull(1)?.toIntOrNull() ?: 30
            
            // If lead reminder is enabled, schedule 15 minutes early
            if (leadEnabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, mHour)
                    set(Calendar.MINUTE, mMinute)
                }
                cal.add(Calendar.MINUTE, -15)
                mHour = cal.get(Calendar.HOUR_OF_DAY)
                mMinute = cal.get(Calendar.MINUTE)
            }
            
            RoutineAlarmScheduler.scheduleRoutineAlarm(ctx, "MORNING", mHour, mMinute, leadEnabled)
        } else {
            RoutineAlarmScheduler.cancelRoutineAlarm(ctx, "MORNING")
        }

        if (eveningEnabled) {
            val eveningParts = eveningTime.split(":")
            var eHour = eveningParts.getOrNull(0)?.toIntOrNull() ?: 21
            var eMinute = eveningParts.getOrNull(1)?.toIntOrNull() ?: 45
            
            // If lead reminder is enabled, schedule 15 minutes early
            if (leadEnabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, eHour)
                    set(Calendar.MINUTE, eMinute)
                }
                cal.add(Calendar.MINUTE, -15)
                eHour = cal.get(Calendar.HOUR_OF_DAY)
                eMinute = cal.get(Calendar.MINUTE)
            }
            
            RoutineAlarmScheduler.scheduleRoutineAlarm(ctx, "EVENING", eHour, eMinute, leadEnabled)
        } else {
            RoutineAlarmScheduler.cancelRoutineAlarm(ctx, "EVENING")
        }

        // Trigger immediate test notifications so the user can preview them instantly
        if (morningEnabled) {
            val testIntent = Intent(ctx, RoutineAlarmReceiver::class.java).apply {
                putExtra("REMINDER_TYPE", "MORNING")
                putExtra("IS_LEAD_REMINDER", leadEnabled)
            }
            ctx.sendBroadcast(testIntent)
        }
        if (eveningEnabled) {
            val testIntent = Intent(ctx, RoutineAlarmReceiver::class.java).apply {
                putExtra("REMINDER_TYPE", "EVENING")
                putExtra("IS_LEAD_REMINDER", leadEnabled)
            }
            ctx.sendBroadcast(testIntent)
        }

        Toast.makeText(ctx, "Đã lưu cấu hình routine và gửi thông báo xem thử thành công!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
