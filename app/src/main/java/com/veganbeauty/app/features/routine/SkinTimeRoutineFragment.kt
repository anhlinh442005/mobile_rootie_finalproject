package com.veganbeauty.app.features.routine

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.SkinTimeRoutineBinding
import com.veganbeauty.app.databinding.ItemTimeRoutineStepBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

class SkinTimeRoutineFragment : RootieFragment() {

    private var _binding: SkinTimeRoutineBinding? = null
    private val binding get() = _binding!!

    private var routineType = "morning" // "morning" or "evening"
    private val activeSteps = mutableListOf<SkincareStep>()
    private val stepProducts = mutableMapOf<Int, ProductEntity>()

    data class SkincareStep(
        val index: Int,
        val name: String,
        val description: String,
        val isChecked: Boolean
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinTimeRoutineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val ctx = requireContext()

        // 1. Get arguments
        routineType = arguments?.getString("routine_type") ?: "morning"

        // 2. Load Avatar
        val avatarUrl = ProfileSession.getAvatar(ctx)
        com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)

        // 3. Customize dynamic elements based on routine type
        val fullName = ProfileSession.getFullName(ctx)
        if (routineType == "morning") {
            binding.tvTitle.text = "Morning Routine"
            binding.tvMotivationalSlogan.text = "Bắt đầu ngày mới với làn da tươi tắn và tràn đầy năng lượng nhé ${fullName} ơi!"
            binding.tvTipTitle.text = "Mẹo nhỏ sáng nay"
            binding.tvTipDesc.text = "Đừng quên thoa kem chống nắng ngay cả khi bạn làm việc trong nhà nhé!"
        } else {
            binding.tvTitle.text = "Evening Routine"
            binding.tvMotivationalSlogan.text = "Nuôi dưỡng làn da phục hồi và thư giãn sâu sau một ngày dài nhé ${fullName} ơi!"
            binding.tvTipTitle.text = "Mẹo nhỏ tối nay"
            binding.tvTipDesc.text = "Hãy thoa kem dưỡng trước 20 phút trước khi ngủ để dưỡng chất thẩm thấu tốt nhất nhé!"
        }

        // 4. Back navigation
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 5. Customize routine action
        binding.btnCustomize.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, SkinRoutineSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        // 6. Complete routine action
        binding.btnCompleteRoutine.setOnClickListener {
            completeRoutineAction()
        }



        // 8. Load and populate steps
        loadSteps()

        // 9. Notification action
        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
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

    private fun completeRoutineAction() {
        val ctx = requireContext()
        val targetDate = getRoutineDate(routineType)

        val isAlreadySubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)
        if (isAlreadySubmitted) {
            Toast.makeText(ctx, "Routine đã được hoàn tất trước đó!", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Lock session (submit)
        ProfileSession.setRoutineSubmitted(ctx, routineType, targetDate, true)

        val completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate)
        val totalCount = activeSteps.size
        val completedCount = activeSteps.count { completedSteps.contains("${routineType}_${it.index}") }

        // Mark as completed in ProfileSession for streak only if within time window and at least 1 step is checked
        if (isWithinTimeWindow(routineType) && completedCount > 0) {
            if (routineType == "morning") {
                ProfileSession.addCompletedMorningDate(ctx, targetDate)
            } else {
                ProfileSession.addCompletedEveningDate(ctx, targetDate)
            }
        } else {
            // Remove from completed if submitted outside window or no steps are checked
            if (routineType == "morning") {
                val completedMornings = ProfileSession.getCompletedMorningDates(ctx).toMutableSet()
                completedMornings.remove(targetDate)
                ProfileSession.setCompletedMorningDates(ctx, completedMornings)
            } else {
                val completedEvenings = ProfileSession.getCompletedEveningDates(ctx).toMutableSet()
                completedEvenings.remove(targetDate)
                ProfileSession.setCompletedEveningDates(ctx, completedEvenings)
            }
        }

        if (!isWithinTimeWindow(routineType)) {
            Toast.makeText(ctx, "Đã chốt phiên Routine! Ngoài khung giờ quy định nên không được cộng xu.", Toast.LENGTH_LONG).show()
            viewLifecycleOwner.lifecycleScope.launch {
                checkStreakAndUpdate(routineType)
                parentFragmentManager.popBackStack()
            }
            return
        }

        if (totalCount > 0 && completedCount == totalCount) {
            val isRewardGiven = if (routineType == "morning") {
                ProfileSession.isMorningRewardAwarded(ctx, targetDate)
            } else {
                ProfileSession.isEveningRewardAwarded(ctx, targetDate)
            }

            if (!isRewardGiven) {
                if (routineType == "morning") {
                    ProfileSession.setMorningRewardAwarded(ctx, targetDate, true)
                } else {
                    ProfileSession.setEveningRewardAwarded(ctx, targetDate, true)
                }

                // Award points via database
                val db = RootieDatabase.getDatabase(ctx)
                viewLifecycleOwner.lifecycleScope.launch {
                    db.rewardPointDao().insertRewardPoints(
                        RewardPointEntity(
                            orderId = if (routineType == "morning") "MORNING_ROUTINE" else "EVENING_ROUTINE",
                            points = 10,
                            reason = if (routineType == "morning") "Hoàn thành Routine Sáng" else "Hoàn thành Routine Tối",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx)
                    Toast.makeText(ctx, "Tuyệt vời! Bạn đã hoàn thành 100% Routine và nhận được +10 xu!", Toast.LENGTH_LONG).show()
                    checkStreakAndUpdate(routineType)
                    parentFragmentManager.popBackStack()
                }
            } else {
                Toast.makeText(ctx, "Routine đã được hoàn tất và nhận thưởng trước đó!", Toast.LENGTH_SHORT).show()
                viewLifecycleOwner.lifecycleScope.launch {
                    checkStreakAndUpdate(routineType)
                    parentFragmentManager.popBackStack()
                }
            }
        } else {
            Toast.makeText(ctx, "Đã chốt phiên Routine! Bạn chưa hoàn thành 100% các bước nên không được cộng xu.", Toast.LENGTH_LONG).show()
            viewLifecycleOwner.lifecycleScope.launch {
                checkStreakAndUpdate(routineType)
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadSteps() {
        val ctx = requireContext()
        val rawSteps = if (routineType == "morning") {
            ProfileSession.getMorningSteps(ctx)
        } else {
            ProfileSession.getEveningSteps(ctx)
        }

        activeSteps.clear()
        rawSteps.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 4) {
                val index = parts[0].toIntOrNull() ?: 0
                val name = parts[1]
                val desc = parts[2]
                val isChecked = parts[3].toBoolean()
                if (isChecked) {
                    SkincareStep(index, name, desc, isChecked)
                } else null
            } else null
        }.sortedBy { it.index }.let {
            activeSteps.addAll(it)
        }

        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val allProducts = db.productDao().getAllProducts().first()
                stepProducts.clear()
                for (step in activeSteps) {
                    val matched = allProducts.firstOrNull { prod ->
                        val nameLower = prod.name.lowercase()
                        val catLower = prod.category.lowercase()
                        val stepLower = step.name.lowercase()

                        when {
                            stepLower.contains("cleanser") || stepLower.contains("sữa rửa mặt") || stepLower.contains("rửa mặt") -> {
                                nameLower.contains("sữa rửa mặt") || nameLower.contains("cleanser") || catLower.contains("cleanser")
                            }
                            stepLower.contains("toner") || stepLower.contains("nước hoa hồng") || stepLower.contains("cân bằng") -> {
                                nameLower.contains("toner") || nameLower.contains("nước hoa hồng") || catLower.contains("toner")
                            }
                            stepLower.contains("serum") || stepLower.contains("tinh chất") -> {
                                nameLower.contains("serum") || nameLower.contains("tinh chất") || catLower.contains("serum")
                            }
                            stepLower.contains("moisturizer") || stepLower.contains("kem dưỡng ẩm") || stepLower.contains("dưỡng ẩm") || stepLower.contains("khóa ẩm") -> {
                                nameLower.contains("kem dưỡng") || nameLower.contains("moisturizer") || nameLower.contains("cream") || catLower.contains("moisturizer")
                            }
                            stepLower.contains("sunscreen") || stepLower.contains("chống nắng") || stepLower.contains("kem chống nắng") -> {
                                nameLower.contains("chống nắng") || nameLower.contains("sunscreen") || catLower.contains("sunscreen")
                            }
                            stepLower.contains("makeup remover") || stepLower.contains("tẩy trang") -> {
                                nameLower.contains("tẩy trang") || nameLower.contains("remover") || catLower.contains("remover")
                            }
                            else -> false
                        }
                    }
                    if (matched != null) {
                        stepProducts[step.index] = matched
                    }
                }
                withContext(Dispatchers.Main) {
                    populateStepsList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        populateStepsList()
        updateStatsAndProgress()
    }

    private fun populateStepsList() {
        if (_binding == null) return
        val ctx = requireContext()
        binding.layoutStepsContainer.removeAllViews()

        val targetDate = getRoutineDate(routineType)
        val completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate)
        val isSubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)

        for (step in activeSteps) {
            val stepBinding = ItemTimeRoutineStepBinding.inflate(
                LayoutInflater.from(ctx),
                binding.layoutStepsContainer,
                false
            )

            // Bind values
            stepBinding.tvStepName.text = step.name
            stepBinding.tvStepDesc.text = step.description
            
            val duration = getStepTime(step.name)
            stepBinding.tvStepTime.text = duration
            stepBinding.ivStepIcon.setImageResource(getStepIconRes(step.name))

            val stepId = "${routineType}_${step.index}"
            val isStepCompleted = completedSteps.contains(stepId) || isSubmitted

            if (isStepCompleted) {
                stepBinding.ivCheckbox.setImageResource(R.drawable.quiz_ic_selected)
            } else {
                stepBinding.ivCheckbox.setImageResource(R.drawable.skin_ic_circle_unchecked)
            }

            val matchedProduct = stepProducts[step.index]
            if (matchedProduct != null) {
                stepBinding.tvViewProductLink.visibility = View.VISIBLE
                stepBinding.tvViewProductLink.text = "🛒 Gợi ý: ${matchedProduct.name}"
                stepBinding.tvViewProductLink.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment().apply {
                            setProduct(matchedProduct)
                        })
                        .addToBackStack(null)
                        .commit()
                }
            } else {
                stepBinding.tvViewProductLink.visibility = View.GONE
            }

            // Click listener
            val clickListener = View.OnClickListener {
                toggleStep(step.index)
            }
            stepBinding.root.setOnClickListener(clickListener)
            stepBinding.ivCheckbox.setOnClickListener(clickListener)

            binding.layoutStepsContainer.addView(stepBinding.root)
        }
    }

    private fun toggleStep(stepIndex: Int) {
        val ctx = requireContext()
        val targetDate = getRoutineDate(routineType)

        val isSubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)
        if (isSubmitted) {
            Toast.makeText(ctx, "Routine đã được chốt và hoàn thành, không thể thay đổi!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isWithinTimeWindow(routineType)) {
            Toast.makeText(ctx, "Chưa đến giờ làm routine hoặc đã quá giờ quy định!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentSet = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate).toMutableSet()
        val stepId = "${routineType}_$stepIndex"

        if (currentSet.contains(stepId)) {
            currentSet.remove(stepId)
        } else {
            currentSet.add(stepId)
        }
        ProfileSession.setCompletedStepIdsForDate(ctx, targetDate, currentSet)

        // Count how many steps of the current routineType are checked today
        val count = activeSteps.count { currentSet.contains("${routineType}_${it.index}") }
        if (routineType == "morning") {
            val completedMornings = ProfileSession.getCompletedMorningDates(ctx).toMutableSet()
            if (count > 0 && isWithinTimeWindow(routineType)) {
                completedMornings.add(targetDate)
            } else {
                completedMornings.remove(targetDate)
            }
            ProfileSession.setCompletedMorningDates(ctx, completedMornings)
        } else {
            val completedEvenings = ProfileSession.getCompletedEveningDates(ctx).toMutableSet()
            if (count > 0 && isWithinTimeWindow(routineType)) {
                completedEvenings.add(targetDate)
            } else {
                completedEvenings.remove(targetDate)
            }
            ProfileSession.setCompletedEveningDates(ctx, completedEvenings)
        }

        // Run streak check in background if within time window
        if (isWithinTimeWindow(routineType)) {
            viewLifecycleOwner.lifecycleScope.launch {
                checkStreakAndUpdate(routineType)
            }
        }

        // Refresh lists & metrics
        populateStepsList()
        updateStatsAndProgress()
    }

    private fun updateStatsAndProgress() {
        if (_binding == null) return
        val ctx = requireContext()
        val targetDate = getRoutineDate(routineType)
        val completedSteps = ProfileSession.getCompletedStepIdsForDate(ctx, targetDate)

        val totalCount = activeSteps.size
        val completedCount = activeSteps.count { completedSteps.contains("${routineType}_${it.index}") }

        // Update step badge
        binding.tvStepCountBadge.text = "$totalCount Bước"

        // Calculate expected duration (completed / total minutes)
        var totalMinutes = 0
        var completedMinutes = 0
        val isSubmitted = ProfileSession.isRoutineSubmitted(ctx, routineType, targetDate)
        for (step in activeSteps) {
            val mins = getStepTimeVal(getStepTime(step.name))
            totalMinutes += mins
            
            val stepId = "${routineType}_${step.index}"
            val isStepCompleted = completedSteps.contains(stepId) || isSubmitted
            if (isStepCompleted) {
                completedMinutes += mins
            }
        }
        binding.tvDurationMins.text = "$completedMinutes/$totalMinutes Phút"

        // Progress percentage
        val percentage = if (totalCount > 0) {
            (completedCount * 100) / totalCount
        } else {
            0
        }
        binding.progressBar.progress = percentage

        // Customise Complete Routine button appearance and text based on status and time window
        if (isSubmitted) {
            binding.btnCompleteRoutine.text = "Đã hoàn thành"
            binding.btnCompleteRoutine.isEnabled = false
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.bg_button_disabled)
            binding.btnCompleteRoutine.setTextColor(Color.parseColor("#8E8E93"))
        } else if (!isWithinTimeWindow(routineType)) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (routineType == "morning") {
                if (hour < 6) {
                    binding.btnCompleteRoutine.text = "Chưa đến giờ"
                } else {
                    binding.btnCompleteRoutine.text = "Đã bỏ lỡ"
                }
            } else {
                binding.btnCompleteRoutine.text = "Chưa đến giờ"
            }
            binding.btnCompleteRoutine.isEnabled = false
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.bg_button_disabled)
            binding.btnCompleteRoutine.setTextColor(Color.parseColor("#8E8E93"))
        } else {
            binding.btnCompleteRoutine.text = "Hoàn tất Routine"
            binding.btnCompleteRoutine.isEnabled = true
            binding.btnCompleteRoutine.setBackgroundResource(R.drawable.skin_bg_btn_dark_green)
            binding.btnCompleteRoutine.setTextColor(Color.WHITE)
        }
    }

    private suspend fun checkStreakAndUpdate(type: String) {
        val ctx = requireContext()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val targetDate = getRoutineDate(type)

        val completedMornings = ProfileSession.getCompletedMorningDates(ctx)
        val completedEvenings = ProfileSession.getCompletedEveningDates(ctx)

        if (completedMornings.contains(targetDate) && completedEvenings.contains(targetDate)) {
            val lastCompletedStr = ProfileSession.getSkinLastCompletedDate(ctx)
            if (lastCompletedStr == targetDate) {
                return // Already updated today
            }

            val currentStreak = ProfileSession.getSkinStreak(ctx)
            var newStreak: Int

            if (lastCompletedStr.isNotEmpty()) {
                val lastCal = Calendar.getInstance()
                try {
                    lastCal.time = sdf.parse(lastCompletedStr) ?: Date()
                    lastCal.add(Calendar.DAY_OF_YEAR, 1)
                    val expectedYesterdayStr = sdf.format(lastCal.time)

                    newStreak = if (expectedYesterdayStr == targetDate) {
                        currentStreak + 1
                    } else {
                        1
                    }
                } catch (e: Exception) {
                    newStreak = 1
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
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx)
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
                com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsToFirestore(ctx)
                Toast.makeText(ctx, "Tuyệt vời! Đạt chuỗi 7 ngày chăm da +50 xu!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addAllActiveStepsToCart() {
        val ctx = requireContext()
        if (activeSteps.isEmpty()) {
            Toast.makeText(ctx, "Không có bước nào được kích hoạt để mua!", Toast.LENGTH_SHORT).show()
            return
        }

        val db = RootieDatabase.getDatabase(ctx)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val allProducts = db.productDao().getAllProducts().first()
            val productsToAdd = mutableListOf<ProductEntity>()

            for (step in activeSteps) {
                val matched = allProducts.firstOrNull { prod ->
                    val nameLower = prod.name.lowercase()
                    val catLower = prod.category.lowercase()
                    val stepLower = step.name.lowercase()

                    when {
                        stepLower.contains("cleanser") || stepLower.contains("sữa rửa mặt") || stepLower.contains("rửa mặt") -> {
                            nameLower.contains("sữa rửa mặt") || nameLower.contains("cleanser") || catLower.contains("cleanser")
                        }
                        stepLower.contains("toner") || stepLower.contains("nước hoa hồng") -> {
                            nameLower.contains("toner") || nameLower.contains("nước hoa hồng") || catLower.contains("toner")
                        }
                        stepLower.contains("serum") || stepLower.contains("tinh chất") -> {
                            nameLower.contains("serum") || nameLower.contains("tinh chất") || catLower.contains("serum")
                        }
                        stepLower.contains("moisturizer") || stepLower.contains("kem dưỡng ẩm") || stepLower.contains("dưỡng ẩm") || stepLower.contains("khóa ẩm") -> {
                            nameLower.contains("kem dưỡng") || nameLower.contains("moisturizer") || nameLower.contains("cream") || catLower.contains("moisturizer")
                        }
                        stepLower.contains("sunscreen") || stepLower.contains("chống nắng") || stepLower.contains("kem chống nắng") -> {
                            nameLower.contains("chống nắng") || nameLower.contains("sunscreen") || catLower.contains("sunscreen")
                        }
                        stepLower.contains("makeup remover") || stepLower.contains("tẩy trang") -> {
                            nameLower.contains("tẩy trang") || nameLower.contains("remover") || catLower.contains("remover")
                        }
                        else -> false
                    }
                }
                if (matched != null) {
                    productsToAdd.add(matched)
                }
            }

            if (productsToAdd.isNotEmpty()) {
                for (product in productsToAdd) {
                    val existingItem = db.cartDao().getCartItemById(product.id)
                    if (existingItem != null) {
                        db.cartDao().updateCartItem(existingItem.copy(quantity = existingItem.quantity + 1))
                    } else {
                        db.cartDao().insertCartItem(
                            CartItemEntity(
                                id = product.id,
                                name = product.name,
                                image = product.mainImage,
                                price = product.price,
                                quantity = 1,
                                isSelected = true
                            )
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Đã thêm trọn bộ ${productsToAdd.size} sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(ctx, "Không tìm thấy sản phẩm phù hợp trong cửa hàng", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getStepTime(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("cleanser") || lower.contains("sữa rửa mặt") || lower.contains("rửa mặt") -> "2p"
            lower.contains("toner") || lower.contains("nước hoa hồng") || lower.contains("cân bằng") -> "1p"
            lower.contains("serum") || lower.contains("tinh chất") -> "3p"
            lower.contains("moisturizer") || lower.contains("kem dưỡng ẩm") || lower.contains("dưỡng ẩm") || lower.contains("khóa ẩm") -> "2p"
            lower.contains("sunscreen") || lower.contains("chống nắng") || lower.contains("kem chống nắng") -> "5p"
            lower.contains("makeup remover") || lower.contains("tẩy trang") -> "5p"
            else -> "2p"
        }
    }

    private fun getStepTimeVal(timeStr: String): Int {
        return timeStr.replace("p", "").toIntOrNull() ?: 2
    }

    private fun getStepIconRes(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.contains("cleanser") || lower.contains("sữa rửa mặt") || lower.contains("rửa mặt") -> R.drawable.skin_ic_step_water_drop
            lower.contains("toner") || lower.contains("nước hoa hồng") || lower.contains("cân bằng") -> R.drawable.skin_ic_step_ph
            lower.contains("serum") || lower.contains("tinh chất") -> R.drawable.skin_ic_step_chemistry
            lower.contains("moisturizer") || lower.contains("kem dưỡng ẩm") || lower.contains("dưỡng ẩm") || lower.contains("khóa ẩm") -> R.drawable.skin_ic_step_face
            lower.contains("sunscreen") || lower.contains("chống nắng") || lower.contains("kem chống nắng") -> R.drawable.skin_ic_step_sunscreen
            lower.contains("makeup remover") || lower.contains("tẩy trang") -> R.drawable.skin_ic_step_water_drop
            else -> R.drawable.skin_ic_step_face
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            val ctx = requireContext()
            val avatarUrl = com.veganbeauty.app.data.local.ProfileSession.getAvatar(ctx)
            com.veganbeauty.app.utils.AvatarLoader.loadAvatar(binding.ivAvatar, avatarUrl)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
