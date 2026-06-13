package com.veganbeauty.app.features.myskin

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookingFragment : RootieFragment() {

    private lateinit var storeNameStr: String
    private lateinit var storeAddressStr: String
    private lateinit var storeImageUrlStr: String

    private var selectedService: BookingService? = null
    private var selectedDate: BookingDate? = null
    private var selectedTime: BookingTime? = null
    
    private lateinit var timeAdapter: BookingTimeAdapter
    private lateinit var dateAdapter: BookingDateAdapter
    
    private val baseTimes = listOf("09:00", "10:00", "11:00", "14:00", "15:00", "16:00", "17:00", "18:00")

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
        val btnCalendar: ImageView = view.findViewById(R.id.booking_btn_calendar)

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

        // Initialize Times Adapter
        timeAdapter = BookingTimeAdapter(emptyList()) { time ->
            selectedTime = time
        }
        rvTimes.layoutManager = GridLayoutManager(context, 4)
        rvTimes.adapter = timeAdapter

        // Setup Dates
        val startCal = Calendar.getInstance()
        setupDateList(startCal, rvDates)

        // Setup Calendar Picker
        btnCalendar.setOnClickListener {
            val currentCal = Calendar.getInstance()
            val maxCal = Calendar.getInstance().apply { add(Calendar.MONTH, 2) }

            val picker = DatePickerDialog(requireContext(), R.style.Theme_DeviceDefault_Light_Dialog_Alert, { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance()
                chosenCal.set(year, month, dayOfMonth)
                setupDateList(chosenCal, rvDates)
            }, currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal.get(Calendar.DAY_OF_MONTH))
            
            picker.datePicker.minDate = currentCal.timeInMillis
            picker.datePicker.maxDate = maxCal.timeInMillis
            picker.show()
            
            // Customize colors if necessary
            picker.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.primary, null))
            picker.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.content, null))
        }

        // Click listeners
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        
        btnChangeStore.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnConfirm.setOnClickListener {
            if (selectedService == null || selectedDate == null || selectedTime == null) {
                Toast.makeText(context, "Vui lòng chọn đầy đủ Dịch vụ, Ngày và Giờ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Hiện Dialog xác nhận
            showConfirmationDialog()
        }
    }
    
    private fun setupDateList(startCal: Calendar, rvDates: RecyclerView) {
        val dates = mutableListOf<BookingDate>()
        val dayFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        
        val tempCal = startCal.clone() as Calendar
        for (i in 0 until 5) {
            val dayOfWeek = when (tempCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Thứ 2"
                Calendar.TUESDAY -> "Thứ 3"
                Calendar.WEDNESDAY -> "Thứ 4"
                Calendar.THURSDAY -> "Thứ 5"
                Calendar.FRIDAY -> "Thứ 6"
                Calendar.SATURDAY -> "Thứ 7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }
            
            val clonedCal = tempCal.clone() as Calendar
            dates.add(BookingDate((i + 1).toString(), dayOfWeek, dayFormat.format(tempCal.time), clonedCal))
            
            tempCal.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        selectedDate = null
        selectedTime = null
        updateTimeSlots(null)
        
        dateAdapter = BookingDateAdapter(dates) { date ->
            selectedDate = date
            selectedTime = null
            updateTimeSlots(date.fullDate)
        }
        
        if (rvDates.layoutManager == null) {
            rvDates.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
        rvDates.adapter = dateAdapter
    }
    
    private fun updateTimeSlots(selectedCal: Calendar?) {
        if (selectedCal == null) {
            timeAdapter.updateData(baseTimes.mapIndexed { index, t -> BookingTime((index + 1).toString(), t, false) })
            return
        }
        
        val now = Calendar.getInstance()
        val isToday = now.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) && 
                      now.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR)
                      
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        val times = baseTimes.mapIndexed { index, timeStr ->
            val slotHour = timeStr.split(":")[0].toInt()
            val locked = if (isToday) slotHour <= currentHour else false
            BookingTime((index + 1).toString(), timeStr, locked)
        }
        
        timeAdapter.updateData(times)
    }
    
    private fun showConfirmationDialog() {
        val sDate = selectedDate ?: return
        val sTime = selectedTime ?: return
        val sService = selectedService ?: return
        
        val year = sDate.fullDate?.get(Calendar.YEAR) ?: 2026
        val msg = "Bạn có chắc chắn muốn đặt lịch:\n\n" +
                "Dịch vụ: ${sService.name}\n" +
                "Thời gian: ${sTime.time} - ${sDate.dayOfWeek}, ${sDate.date}/$year\n" +
                "Chi nhánh: $storeNameStr"

        AlertDialog.Builder(requireContext(), R.style.Theme_DeviceDefault_Light_Dialog_Alert)
            .setTitle("Xác nhận đặt lịch")
            .setMessage(msg)
            .setPositiveButton("Đồng ý") { _, _ ->
                completeBooking()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
    
    private fun completeBooking() {
        val sDate = selectedDate ?: return
        val sTime = selectedTime ?: return
        val year = sDate.fullDate?.get(Calendar.YEAR) ?: 2026
        
        val dateTime = "${sDate.date}/$year - ${sTime.time}"
        val service = selectedService?.name ?: ""
        val specialist = "Nguyễn Khánh Xuân"

        // Format tháng cho entity
        val monthFormat = SimpleDateFormat("MM", Locale.getDefault())
        val monthDisplay = "Tháng ${monthFormat.format(sDate.fullDate?.time ?: Calendar.getInstance().time)}"

        val newBooking = com.veganbeauty.app.data.local.entities.BookingHistoryEntity(
            id = "RS" + System.currentTimeMillis().toString().takeLast(8),
            userId = "1",
            userName = com.veganbeauty.app.data.local.ProfileSession.getFullName(requireContext()),
            userPhone = com.veganbeauty.app.data.local.ProfileSession.getPhone(requireContext()),
            userEmail = com.veganbeauty.app.data.local.ProfileSession.getEmail(requireContext()),
            serviceName = service,
            dateDisplay = "${sDate.date}/$year",
            monthDisplay = monthDisplay,
            dayOfWeek = sDate.dayOfWeek,
            time = sTime.time,
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

    override fun observeViewModel() {
        // Not used
    }
}
