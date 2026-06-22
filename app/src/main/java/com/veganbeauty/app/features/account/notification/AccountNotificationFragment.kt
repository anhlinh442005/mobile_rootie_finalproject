package com.veganbeauty.app.features.account.notification

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.entities.NotificationItem
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.databinding.AccountNotificationFragmentBinding
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.features.profile.AccountVoucherDetailFragment
import com.veganbeauty.app.features.profile.VoucherItem
import com.veganbeauty.app.features.account.order.AccountOrderDetailFragment
import com.veganbeauty.app.features.account.checkin.AccountCheckinFragment
import com.veganbeauty.app.features.account.expiry.AccountProductExpiryFragment
import com.veganbeauty.app.features.myskin.BookingHistoryFragment
import com.veganbeauty.app.features.myskin.BookingDetailUpcomingFragment
import com.veganbeauty.app.features.myskin.BookingDetailCompletedFragment
import com.veganbeauty.app.features.myskin.BookingDetailCancelledFragment
import com.veganbeauty.app.features.routine.SkinReminderFragment

class AccountNotificationFragment : RootieFragment() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Đã cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Bạn chưa cấp quyền nhận thông báo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private var _binding: AccountNotificationFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationViewModel

    private val listAdapter by lazy {
        NotificationListAdapter(
            onItemClick = { item ->
                viewModel.markAsRead(item.id)
                handleNotificationNavigation(item)
            },
            onActionClick = { item ->
                viewModel.markAsRead(item.id)
                if (item.actionText == "COPY MÃ" && !item.voucherCode.isNullOrEmpty()) {
                    context?.let { ctx ->
                        copyToClipboard(ctx, item.voucherCode)
                        Toast.makeText(
                            ctx,
                            "Đã sao chép mã voucher ${item.voucherCode} thành công!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                handleNotificationNavigation(item)
            },
            onMarkReadClick = { item ->
                viewModel.markAsRead(item.id)
            },
            onDeleteClick = { item ->
                viewModel.deleteNotification(item.id)
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountNotificationFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val repository = NotificationRepository.getInstance(requireContext())
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NotificationViewModel(repository) as T
            }
        })[NotificationViewModel::class.java]
    }

    override fun setupUI(view: View) {
        checkNotificationPermission()
        // Back Navigation
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        // Bind RecyclerView
        binding.rvNotifications.adapter = listAdapter

        // Bind Chips/Tabs Clicks
        binding.tabAll.setOnClickListener { viewModel.selectTab("Tất cả") }
        binding.tabPromo.setOnClickListener { viewModel.selectTab("Khuyến mãi") }
        binding.tabOrder.setOnClickListener { viewModel.selectTab("Đơn hàng") }
        binding.tabOther.setOnClickListener { viewModel.selectTab("Khác") }

        // Bind Search Input Bar
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })

        // Bind Header Bulk Actions
        binding.btnMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(context, "Đã đánh dấu đọc tất cả", Toast.LENGTH_SHORT).show()
        }

        binding.btnDeleteAll.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xoá tất cả thông báo")
                .setMessage("Bạn có chắc chắn muốn xoá toàn bộ thông báo không?")
                .setPositiveButton("Xoá") { _, _ ->
                    viewModel.deleteAllNotifications()
                    Toast.makeText(context, "Đã xoá toàn bộ thông báo", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    override fun observeViewModel() {
        // Observe search and tab filters to render lists
        viewModel.notificationItems.observe(viewLifecycleOwner) { items ->
            listAdapter.submitList(items)
            val isEmpty = items.isEmpty()
            binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvNotifications.visibility = if (isEmpty) View.GONE else View.VISIBLE
            binding.layoutHeaderActions.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        // Observe active tab to update visual styles dynamically
        viewModel.selectedTab.observe(viewLifecycleOwner) { activeTab ->
            updateTabStyles(activeTab)
        }
    }

    private fun updateTabStyles(activeTab: String) {
        val tabs = mapOf(
            "Tất cả" to binding.tabAll,
            "Khuyến mãi" to binding.tabPromo,
            "Đơn hàng" to binding.tabOrder,
            "Khác" to binding.tabOther
        )

        for ((tab, textView) in tabs) {
            if (tab.equals(activeTab, ignoreCase = true)) {
                textView.setBackgroundResource(R.drawable.tab_active_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                textView.setBackgroundResource(R.drawable.tab_inactive_bg)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
        }
    }

    private fun handleNotificationNavigation(item: NotificationItem) {
        val type = (item.notificationType ?: "").lowercase()
        val category = (item.category ?: "").lowercase()
        val title = (item.title ?: "").lowercase()
        val content = (item.content ?: "").lowercase()

        when {
            type == "voucher" || category.contains("khuyến mãi") || title.contains("voucher") || content.contains("voucher") -> {
                val code = item.voucherCode ?: "RT50KDEC"
                val voucher = NotificationIntentHandler.findVoucherByCode(requireContext(), code) ?: VoucherItem(
                    id = item.id,
                    title = item.title,
                    description = item.content,
                    code = code,
                    status = "valid",
                    hsd = "2026-12-31 23:59:59",
                    type = "discount",
                    fromGift = false,
                    quantity = 1,
                    minOrderValue = 300000,
                    applicableProducts = "Tất cả sản phẩm",
                    offerType = "fixed_amount",
                    discountValue = 50000
                )
                val fragment = AccountVoucherDetailFragment.newInstance(voucher)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            type == "order" || category.contains("đơn hàng") || title.contains("đơn hàng") || content.contains("đơn hàng") -> {
                val orderId = item.orderId ?: "RT8829"
                val fragment = AccountOrderDetailFragment.newInstance(orderId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            type == "checkin" || title.contains("điểm danh") || content.contains("điểm danh") -> {
                val fragment = AccountCheckinFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            type == "product expired" || title.contains("hết hạn") || content.contains("hết hạn") -> {
                val fragment = AccountProductExpiryFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            type == "schedule date" || title.contains("lịch hẹn") || content.contains("lịch hẹn") -> {
                val scheduleId = item.scheduleId ?: "BK_NOTI_101"
                val userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
                val bookings = LocalJsonReader(requireContext()).getUserBookingHistory(userEmail)
                val realBooking = bookings.find { it.id == scheduleId }
                val fragment = if (realBooking != null) {
                    when (realBooking.status) {
                        "Đã hoàn thành" -> BookingDetailCompletedFragment.newInstance(realBooking)
                        "Đã huỷ" -> BookingDetailCancelledFragment.newInstance(realBooking)
                        else -> BookingDetailUpcomingFragment.newInstance(realBooking)
                    }
                } else {
                    val mockBooking = BookingHistoryEntity(
                        id = scheduleId,
                        userId = "user_1",
                        userName = "Nguyễn Khánh Xuân",
                        userPhone = "0901234567",
                        userEmail = userEmail,
                        serviceName = "Chăm sóc da chuyên sâu Acne Free",
                        dateDisplay = "15 Tháng 6, 2026",
                        dayOfWeek = "Thứ Hai",
                        time = "14:00 - 15:30",
                        duration = "90 phút",
                        storeName = "Rootie Quận 1",
                        storeAddress = "123 Nguyễn Thị Minh Khai, Quận 1, TP. HCM",
                        status = "Sắp diễn ra"
                    )
                    BookingDetailUpcomingFragment.newInstance(mockBooking)
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            type == "skin care" || title.contains("dưỡng da") || title.contains("chăm sóc da") || content.contains("dưỡng da") || content.contains("chăm sóc da") -> {
                val fragment = SkinReminderFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Rootie Voucher Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
