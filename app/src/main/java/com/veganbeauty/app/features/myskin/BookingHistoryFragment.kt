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
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
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

        // Setup filter clicks
        filterAll.setOnClickListener { setFilter("Tất Cả", filterAll) }
        filterUpcoming.setOnClickListener { setFilter("Sắp diễn ra", filterUpcoming) }
        filterCompleted.setOnClickListener { setFilter("Đã hoàn thành", filterCompleted) }
        filterCancelled.setOnClickListener { setFilter("Đã huỷ", filterCancelled) }

        // Load data
        val jsonReader = LocalJsonReader(requireContext())
        val userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext())
        allHistory = jsonReader.getUserBookingHistory(userEmail)

        // Init adapter
        rvHistory.layoutManager = LinearLayoutManager(context)
        historyAdapter = BookingHistoryAdapter(emptyList()) { selectedBooking ->
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
        }
        rvHistory.adapter = historyAdapter

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
