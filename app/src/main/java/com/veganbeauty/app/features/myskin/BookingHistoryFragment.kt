package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BookingHistoryFragment : RootieFragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var filterAll: TextView
    private lateinit var filterUpcoming: TextView
    private lateinit var filterCompleted: TextView
    private lateinit var filterCancelled: TextView

    private lateinit var historyAdapter: BookingHistoryAdapter
    private var allHistory: List<BookingHistoryEntity> = emptyList()
    private var currentFilter = "Tất Cả"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.skin_fragment_booking_history, container, false)
    }

    override fun setupUI(view: View) {
        rvHistory = view.findViewById(R.id.rv_history)
        filterAll = view.findViewById(R.id.filter_all)
        filterUpcoming = view.findViewById(R.id.filter_upcoming)
        filterCompleted = view.findViewById(R.id.filter_completed)
        filterCancelled = view.findViewById(R.id.filter_cancelled)
        
        val btnBack = view.findViewById<ImageView>(R.id.btn_back)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        if (!ProfileSession.isLoggedIn(requireContext())) {
            com.veganbeauty.app.features.home.BottomNavHelper.showLoginRequiredDialog(requireContext())
            parentFragmentManager.popBackStack()
            return
        }

        val btnNoti = view.findViewById<View>(R.id.btn_notification)
        btnNoti?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, com.veganbeauty.app.features.account.notification.AccountNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup filter clicks
        filterAll.setOnClickListener { setFilter("Tất Cả", filterAll) }
        filterUpcoming.setOnClickListener { setFilter("Sắp diễn ra", filterUpcoming) }
        filterCompleted.setOnClickListener { setFilter("Đã hoàn thành", filterCompleted) }
        filterCancelled.setOnClickListener { setFilter("Đã huỷ", filterCancelled) }

        // Initial setup for empty list
        // Load data
        val jsonReader = LocalJsonReader(requireContext())
        val userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
        allHistory = jsonReader.getUserBookingHistory(userEmail)

        // Init adapter
        rvHistory.layoutManager = LinearLayoutManager(context)
        historyAdapter = BookingHistoryAdapter(
            items = emptyList(),
            onViewDetailClick = { selectedBooking ->
                val detailFragment = when (selectedBooking.status) {
                    "Đã hoàn thành" -> BookingDetailCompletedFragment.newInstance(selectedBooking)
                    "Đã huỷ" -> BookingDetailCancelledFragment.newInstance(selectedBooking)
                    else -> BookingDetailUpcomingFragment.newInstance(selectedBooking)
                }
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onCancelClick = { selectedBooking ->
                showCancelDialog(selectedBooking)
            }
        )
        rvHistory.adapter = historyAdapter

        // Load data from Firestore
        loadDataFromFirestore()
    }

    private fun loadDataFromFirestore() {
        val userEmail = ProfileSession.getEmail(requireContext()).takeIf { it.isNotBlank() } ?: "test@example.com"
        viewLifecycleOwner.lifecycleScope.launch {
            allHistory = FirestoreService().getUserBookingHistory(userEmail)
            applyFilter()
        }
        // Initial filter
        applyFilter()

        // Fetch from Firestore asynchronously
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val remoteBookings = com.veganbeauty.app.data.remote.FirestoreService().fetchBookingsForUser(userEmail)
                if (remoteBookings.isNotEmpty()) {
                    for (remote in remoteBookings) {
                        val existing = allHistory.find { it.id == remote.id }
                        if (existing != null) {
                            jsonReader.updateBookingStatus(remote.id, remote.status, remote.cancelReason)
                        } else {
                            jsonReader.addBooking(remote)
                        }
                    }
                    allHistory = jsonReader.getUserBookingHistory(userEmail)
                    applyFilter()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showCancelDialog(data: BookingHistoryEntity) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_cancel_booking, null)
        val dialogBuilder = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        // Setup Dialog Data
        val (dayNum, monthDayStr) = BookingDateParser.parseDateDisplay(data.dateDisplay, data.monthDisplay, data.dayOfWeek)
        dialogView.findViewById<android.widget.TextView>(R.id.tv_date).text = dayNum
        dialogView.findViewById<android.widget.TextView>(R.id.tv_month_day).text = monthDayStr
        dialogView.findViewById<android.widget.TextView>(R.id.tv_service_name).text = data.serviceName
        dialogView.findViewById<android.widget.TextView>(R.id.tv_time).text = "${data.time} (${data.duration})"
        dialogView.findViewById<android.widget.TextView>(R.id.tv_store_name).text = data.storeName
        dialogView.findViewById<android.widget.TextView>(R.id.tv_store_address).text = data.storeAddress
        
        // Setup Spinner
        val spReason = dialogView.findViewById<android.widget.Spinner>(R.id.sp_reason)
        val reasons = listOf("Chọn lý do hủy lịch", "Thay đổi lịch trình", "Tìm được địa điểm khác", "Lý do sức khoẻ", "Đã đặt nhầm dịch vụ", "Lý do khác")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons)
        spReason.adapter = adapter
        
        val etOtherReason = dialogView.findViewById<android.widget.EditText>(R.id.et_other_reason)
        
        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_back).setOnClickListener { dialog.dismiss() }
        
        dialogView.findViewById<View>(R.id.btn_confirm_cancel).setOnClickListener {
            val selectedReason = spReason.selectedItem.toString()
            val otherReason = etOtherReason.text.toString().trim()
            val finalReason = when {
                selectedReason == "Lý do khác" && otherReason.isNotEmpty() -> otherReason
                selectedReason == "Lý do khác" -> "Lý do khác"
                selectedReason == "Chọn lý do hủy lịch" && otherReason.isNotEmpty() -> otherReason
                selectedReason == "Chọn lý do hủy lịch" -> "Không có lý do cụ thể"
                else -> selectedReason
            }
            
            // Update in memory & local storage
            LocalJsonReader(requireContext()).updateBookingStatus(data.id, "Đã huỷ", finalReason)
            
            // Reload local data
            val userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
            allHistory = LocalJsonReader(requireContext()).getUserBookingHistory(userEmail)
            applyFilter()

            // Update on Firestore
            viewLifecycleOwner.lifecycleScope.launch {
                com.veganbeauty.app.data.remote.FirestoreService().updateBookingStatus(data.id, "Đã huỷ", finalReason)
            }
            
            android.widget.Toast.makeText(requireContext(), "Hủy lịch thành công", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun observeViewModel() {}

    private fun setFilter(filter: String, selectedTextView: TextView) {
        currentFilter = filter

        val unselectedBg = R.drawable.skin_bg_outline
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.primary)

        filterAll.setBackgroundResource(unselectedBg)
        filterAll.setTextColor(unselectedColor)
        
        filterUpcoming.setBackgroundResource(unselectedBg)
        filterUpcoming.setTextColor(unselectedColor)
        
        filterCompleted.setBackgroundResource(unselectedBg)
        filterCompleted.setTextColor(unselectedColor)
        
        filterCancelled.setBackgroundResource(unselectedBg)
        filterCancelled.setTextColor(unselectedColor)

        // Selected state
        selectedTextView.setBackgroundResource(R.drawable.skin_bg_btn_book)
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral))

        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = if (currentFilter == "Tất Cả") {
            allHistory
        } else if (currentFilter == "Sắp diễn ra") {
            allHistory.filter { 
                it.status.equals("Sắp diễn ra", ignoreCase = true) || 
                it.status.equals("Chờ xác nhận", ignoreCase = true) || 
                it.status.equals("pending", ignoreCase = true)
            }
        } else {
            allHistory.filter { it.status.equals(currentFilter, ignoreCase = true) }
        }

        // Group by status
        val grouped = filteredList.groupBy { 
            if (it.status.equals("Chờ xác nhận", ignoreCase = true) || it.status.equals("pending", ignoreCase = true)) {
                "Sắp diễn ra"
            } else {
                it.status
            }
        }
        
        // Define order of groups to match UI design
        val order = listOf("Sắp diễn ra", "Đã hoàn thành", "Đã huỷ")
        val displayItems = mutableListOf<Any>()
        
        for (status in order) {
            val itemsForStatus = grouped[status]
            if (!itemsForStatus.isNullOrEmpty()) {
                // Add header only if "Tất Cả" is selected or we want headers always.
                // The design has headers "Sắp diễn ra", "Đã hoàn thành", "Đã huỷ"
                // Usually filters still show headers if there's only one category. Let's always show header.
                displayItems.add(status)
                displayItems.addAll(itemsForStatus)
            }
        }

        historyAdapter.updateData(displayItems)
    }
}
