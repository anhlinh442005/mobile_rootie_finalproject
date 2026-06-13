package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment

class BookingFragment : RootieFragment() {

    private lateinit var storeNameStr: String
    private lateinit var storeAddressStr: String
    private lateinit var storeImageUrlStr: String

    private var selectedService: BookingService? = null
    private var selectedDate: BookingDate? = null
    private var selectedTime: BookingTime? = null

    companion object {
        fun newInstance(storeName: String, storeAddress: String, storeImageUrl: String): BookingFragment {
            val args = Bundle()
            args.putString("STORE_NAME", storeName)
            args.putString("STORE_ADDRESS", storeAddress)
            args.putString("STORE_IMAGE_URL", storeImageUrl)
            val fragment = BookingFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.skin_fragment_booking, container, false)
    }

    override fun setupUI(view: View) {
        storeNameStr = arguments?.getString("STORE_NAME") ?: "Rootie Gò Vấp"
        storeAddressStr = arguments?.getString("STORE_ADDRESS") ?: "27 Quang Trung, P.10, Gò Vấp, Tp. Hồ Chí Minh"
        storeImageUrlStr = arguments?.getString("STORE_IMAGE_URL") ?: ""

        val storeName: TextView = view.findViewById(R.id.booking_store_name)
        val storeAddress: TextView = view.findViewById(R.id.booking_store_address)
        val storeImage: ImageView = view.findViewById(R.id.booking_store_image)
        val btnChangeStore: TextView = view.findViewById(R.id.btn_change_store)
        val btnBack: ImageView = view.findViewById(R.id.btn_back)
        val btnConfirm: TextView = view.findViewById(R.id.btn_confirm_booking)

        val rvServices: RecyclerView = view.findViewById(R.id.rv_services)
        val rvDates: RecyclerView = view.findViewById(R.id.rv_dates)
        val rvTimes: RecyclerView = view.findViewById(R.id.rv_times)

        // Set store info
        storeName.text = storeNameStr
        storeAddress.text = storeAddressStr
        if (storeImageUrlStr.isNotEmpty()) {
            storeImage.load(storeImageUrlStr) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        } else {
            storeImage.setImageResource(R.drawable.imv_logo)
        }

        // Setup Services
        val mockServices = listOf(
            BookingService("1", "Soi da cơ bản", "30 phút * Miễn phí", "30 phút"),
            BookingService("2", "Soi da chuyên sâu", "45 phút * 199.000 đ", "45 phút"),
            BookingService("3", "Soi da & tư vấn 1:1", "60 phút * 299.000 đ", "60 phút")
        )
        rvServices.layoutManager = LinearLayoutManager(context)
        rvServices.adapter = BookingServiceAdapter(mockServices) { service ->
            selectedService = service
        }

        // Setup Dates
        val mockDates = listOf(
            BookingDate("1", "Thứ 6", "24/05"),
            BookingDate("2", "Thứ 7", "25/05"),
            BookingDate("3", "CN", "26/05"),
            BookingDate("4", "Thứ 2", "27/05"),
            BookingDate("5", "Thứ 3", "28/05")
        )
        rvDates.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvDates.adapter = BookingDateAdapter(mockDates) { date ->
            selectedDate = date
        }

        // Setup Times
        val mockTimes = listOf(
            BookingTime("1", "09:00"), BookingTime("2", "10:00"), BookingTime("3", "11:00"), BookingTime("4", "14:00"),
            BookingTime("5", "15:00"), BookingTime("6", "16:00"), BookingTime("7", "17:00"), BookingTime("8", "18:00")
        )
        rvTimes.layoutManager = GridLayoutManager(context, 4)
        rvTimes.adapter = BookingTimeAdapter(mockTimes) { time ->
            selectedTime = time
        }

        // Click listeners
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        btnChangeStore.setOnClickListener {
            // Thay đổi chi nhánh -> pop back to choose branch
            parentFragmentManager.popBackStack()
        }

        btnConfirm.setOnClickListener {
            if (selectedService == null || selectedDate == null || selectedTime == null) {
                Toast.makeText(context, "Vui lòng chọn đầy đủ Dịch vụ, Ngày và Giờ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Đặt lịch thành công
            val dateTime = "${selectedDate?.date}/2026 - ${selectedTime?.time}"
            val service = selectedService?.name ?: ""
            val specialist = "Nguyễn Khánh Xuân" // Mặc định hoặc lấy từ ProfileSession

            // Lưu lịch hẹn vào memory để hiển thị ở Lịch sử
            val newBooking = com.veganbeauty.app.data.local.entities.BookingHistoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                userId = "1",
                userName = com.veganbeauty.app.data.local.ProfileSession.getFullName(requireContext()),
                userPhone = com.veganbeauty.app.data.local.ProfileSession.getPhone(requireContext()),
                userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext()),
                serviceName = service,
                dateDisplay = selectedDate?.date ?: "",
                monthDisplay = "Tháng 05",
                dayOfWeek = selectedDate?.dayOfWeek ?: "",
                time = selectedTime?.time ?: "",
                duration = selectedService?.duration ?: "",
                storeName = storeNameStr,
                storeAddress = storeAddressStr,
                storePhone = "1900 1234",
                storeImage = storeImageUrlStr,
                status = "upcoming",
                createdAt = "Vừa xong",
                consultantName = specialist,
                consultantAvatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
                consultantRating = 5.0f
            )
            com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).addBooking(newBooking)

            val successFragment = BookingSuccessFragment.newInstance(
                storeName = storeNameStr,
                dateTime = dateTime,
                specialist = specialist,
                serviceName = service
            )

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    android.R.anim.slide_in_left,
                    android.R.anim.fade_out,
                    android.R.anim.fade_in,
                    android.R.anim.slide_out_right
                )
                .replace(R.id.main_container, successFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun observeViewModel() {
        // Not used
    }
}
