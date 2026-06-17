package com.veganbeauty.app.features.account.order

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountOrderTrackingFragmentBinding
import com.veganbeauty.app.databinding.AccountOrderTrackingStepItemBinding
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment
import kotlinx.coroutines.launch

class AccountOrderTrackingFragment : RootieFragment() {

    private var _binding: AccountOrderTrackingFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: OrderDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountOrderTrackingFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
        val orderId = arguments?.getString(ARG_ORDER_ID) ?: ""

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OrderDetailViewModel(repository, orderId) as T
            }
        })[OrderDetailViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Back Navigation
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Notification Navigation
        val navigateToNotification = View.OnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }
        binding.layoutNotification.setOnClickListener(navigateToNotification)
        binding.btnNotification.setOnClickListener(navigateToNotification)

        // Shipper Call Action
        binding.btnCallShipper.setOnClickListener {
            Toast.makeText(context, "Đang kết nối cuộc gọi đến shipper...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun observeViewModel() {
        viewModel.order.observe(viewLifecycleOwner) { order ->
            if (order != null) {
                bindOrderTracking(order)
            }
        }

        // Notification badge
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationRepository.getInstance(requireContext()).unreadCount.collect { count ->
                _binding?.viewNotificationBadge?.visibility = if (count > 0) View.VISIBLE else View.GONE
            }
        }
    }

    private fun bindOrderTracking(order: OrderEntity) {
        val context = context ?: return

        // 1. Expected Delivery & Order ID
        binding.tvExpectedTime.text = order.expectedDeliveryTime ?: "Hôm nay, 18:00 - 20:00"
        binding.tvOrderCode.text = "Mã đơn: ${order.orderId}"

        // Badge styling based on status
        val (badgeBgRes, badgeTextRes) = when (order.status) {
            "Chờ xử lý" -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
            "Đang xử lý" -> Pair(R.color.status_processing_bg, R.color.status_processing_text)
            "Đang giao" -> Pair(R.color.status_delivering_bg, R.color.status_delivering_text)
            "Hoàn tất" -> Pair(R.color.status_success_bg, R.color.status_success_text)
            "Đã hủy" -> Pair(R.color.status_cancelled_bg, R.color.status_cancelled_text)
            else -> Pair(R.color.status_pending_bg, R.color.status_pending_text)
        }
        binding.tvStatusBadge.background.mutate().setTint(ContextCompat.getColor(context, badgeBgRes))
        binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, badgeTextRes))
        binding.tvStatusBadge.text = order.status.uppercase()

        // 2. Shipper Details
        binding.tvShipperName.text = "Nhân viên giao hàng"
        binding.tvDeliveryService.text = if (order.paymentMethod.contains("MoMo")) {
            "Giao hàng tiết kiệm (GHTK)"
        } else {
            "Giao hàng nhanh (GHN)"
        }

        // 3. Journey Timeline
        buildJourneyTimeline(order)
    }

    private fun buildJourneyTimeline(order: OrderEntity) {
        val context = context ?: return
        binding.layoutTimelineContainer.removeAllViews()

        val orderCal = parseDateTime(order.orderDate, order.orderTime)
        val steps = mutableListOf<TrackingStep>()

        if (order.status == "Đã hủy") {
            val calCancel = (orderCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.HOUR_OF_DAY, 1) }
            steps.add(TrackingStep(
                title = "Đơn hàng đã hủy",
                description = "Đơn hàng của bạn đã bị hủy bỏ.",
                dateTimeStr = formatDateTime(calCancel),
                isCompleted = true,
                isActive = true
            ))
            steps.add(TrackingStep(
                title = "Đặt hàng thành công",
                description = "Đơn hàng đã được ghi nhận trên hệ thống.",
                dateTimeStr = formatDateTime(orderCal),
                isCompleted = true,
                isActive = false
            ))
        } else {
            val cal1 = orderCal
            val cal2 = (orderCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.HOUR_OF_DAY, 2) }
            val cal3 = (orderCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.HOUR_OF_DAY, 6) }
            val cal4 = (orderCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 1); add(java.util.Calendar.HOUR_OF_DAY, 2) }
            val cal5 = (orderCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 1); add(java.util.Calendar.HOUR_OF_DAY, 8) }

            val status = order.status
            
            // Step 5: Giao hàng thành công
            steps.add(TrackingStep(
                title = "Giao hàng thành công",
                description = "Đơn hàng đã được giao thành công đến người nhận.",
                dateTimeStr = formatDateTime(cal5),
                isCompleted = status == "Hoàn tất",
                isActive = status == "Hoàn tất"
            ))
            // Step 4: Đang giao đến bạn
            steps.add(TrackingStep(
                title = "Đang giao đến bạn",
                description = "Shipper đang trên đường giao hàng đến địa chỉ của bạn.",
                dateTimeStr = formatDateTime(cal4),
                isCompleted = status == "Hoàn tất" || status == "Đang giao",
                isActive = status == "Đang giao"
            ))
            // Step 3: Đơn hàng đã giao cho đơn vị vận chuyển
            steps.add(TrackingStep(
                title = "Đơn hàng đã giao cho đơn vị vận chuyển",
                description = "Đơn vị vận chuyển đã tiếp nhận đơn hàng và đang vận chuyển.",
                dateTimeStr = formatDateTime(cal3),
                isCompleted = status == "Hoàn tất" || status == "Đang giao",
                isActive = false
            ))
            // Step 2: Đơn hàng đã được đóng gói
            steps.add(TrackingStep(
                title = "Đơn hàng đã được đóng gói",
                description = "Nhân viên đã đóng gói sản phẩm hoàn tất.",
                dateTimeStr = formatDateTime(cal2),
                isCompleted = status == "Hoàn tất" || status == "Đang giao" || status == "Đang xử lý",
                isActive = status == "Đang xử lý"
            ))
            // Step 1: Đặt hàng thành công
            steps.add(TrackingStep(
                title = "Đặt hàng thành công",
                description = "Đơn hàng của bạn đã được ghi nhận trên hệ thống.",
                dateTimeStr = formatDateTime(cal1),
                isCompleted = true,
                isActive = status == "Chờ xử lý"
            ))
        }

        // Inflate steps dynamically
        for (i in steps.indices) {
            val step = steps[i]
            val stepBinding = AccountOrderTrackingStepItemBinding.inflate(
                LayoutInflater.from(context),
                binding.layoutTimelineContainer,
                false
            )

            // Bind values
            stepBinding.tvStepTitle.text = step.title
            stepBinding.tvStepDesc.text = step.description

            if (step.isCompleted) {
                stepBinding.tvStepDateTime.visibility = View.VISIBLE
                stepBinding.tvStepDateTime.text = step.dateTimeStr
                
                // Color text as active
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
                stepBinding.tvStepDesc.setTextColor(ContextCompat.getColor(context, R.color.primary))
                stepBinding.tvStepDateTime.setTextColor(ContextCompat.getColor(context, R.color.gray_dark))

                // Color dot as completed
                stepBinding.viewDotInner.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
            } else {
                stepBinding.tvStepDateTime.visibility = View.GONE
                
                // Color text as inactive
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(context, R.color.gray_dark))
                stepBinding.tvStepDesc.setTextColor(ContextCompat.getColor(context, R.color.gray_dark))

                // Color dot as inactive
                stepBinding.viewDotInner.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_light))
            }

            // Glow ring for active step
            if (step.isActive) {
                stepBinding.viewDotOuter.visibility = View.VISIBLE
                stepBinding.viewDotOuter.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                stepBinding.tvStepTitle.setTextColor(ContextCompat.getColor(context, R.color.primary))
                stepBinding.tvStepTitle.textSize = 15f // Slightly larger for emphasis
            } else {
                stepBinding.viewDotOuter.visibility = View.GONE
            }

            // Connector Line styling
            // 1. Top Connector Segment (above dot)
            if (i == 0) {
                stepBinding.viewLineTop.visibility = View.GONE
            } else {
                stepBinding.viewLineTop.visibility = View.VISIBLE
                val prevStep = steps[i - 1]
                if (prevStep.isCompleted) {
                    stepBinding.viewLineTop.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                } else {
                    stepBinding.viewLineTop.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_light))
                }
            }

            // 2. Bottom Connector Segment (below dot)
            if (i == steps.size - 1) {
                stepBinding.viewLineBottom.visibility = View.GONE
            } else {
                stepBinding.viewLineBottom.visibility = View.VISIBLE
                if (step.isCompleted) {
                    stepBinding.viewLineBottom.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary))
                } else {
                    stepBinding.viewLineBottom.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_light))
                }
            }

            binding.layoutTimelineContainer.addView(stepBinding.root)
        }
    }

    private fun parseDateTime(dateStr: String, timeStr: String): java.util.Calendar {
        val cal = java.util.Calendar.getInstance()
        try {
            if (dateStr.contains("/") || dateStr.contains("-")) {
                val separator = if (dateStr.contains("/")) "/" else "-"
                val parts = dateStr.split(separator)
                if (parts.size == 3) {
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = parts[1].toIntOrNull() ?: 1
                    val year = parts[2].toIntOrNull() ?: 2026
                    val timeParts = timeStr.split(":")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                    cal.set(year, month - 1, day, hour, minute, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal
                }
            }
            if (dateStr.contains(" Thg ")) {
                val dateClean = dateStr.replace(",", "").trim()
                val parts = dateClean.split(" ")
                if (parts.size >= 4) {
                    val day = parts[0].toIntOrNull() ?: 1
                    val month = parts[2].toIntOrNull() ?: 1
                    val year = parts[3].toIntOrNull() ?: 2026
                    val timeParts = timeStr.split(":")
                    val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 0
                    val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                    cal.set(year, month - 1, day, hour, minute, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cal
    }

    private fun formatDateTime(cal: java.util.Calendar): String {
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        return String.format("%02d:%02d, %02d/%02d/%d", hour, minute, day, month, year)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDER_ID = "arg_order_id"

        fun newInstance(orderId: String): AccountOrderTrackingFragment {
            val fragment = AccountOrderTrackingFragment()
            val args = Bundle()
            args.putString(ARG_ORDER_ID, orderId)
            fragment.arguments = args
            return fragment
        }
    }

    data class TrackingStep(
        val title: String,
        val description: String,
        val dateTimeStr: String,
        val isCompleted: Boolean,
        val isActive: Boolean
    )
}
