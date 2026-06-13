package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.features.home.HomeFragment

class BookingSuccessFragment : RootieFragment() {

    private lateinit var storeName: String
    private lateinit var dateTime: String
    private lateinit var specialist: String
    private lateinit var serviceName: String

    companion object {
        fun newInstance(storeName: String, dateTime: String, specialist: String, serviceName: String): BookingSuccessFragment {
            val args = Bundle()
            args.putString("STORE_NAME", storeName)
            args.putString("DATE_TIME", dateTime)
            args.putString("SPECIALIST", specialist)
            args.putString("SERVICE_NAME", serviceName)
            val fragment = BookingSuccessFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.skin_fragment_booking_success, container, false)
    }

    override fun setupUI(view: View) {
        storeName = arguments?.getString("STORE_NAME") ?: "Rootie Gò Vấp"
        dateTime = arguments?.getString("DATE_TIME") ?: ""
        specialist = arguments?.getString("SPECIALIST") ?: "Nguyễn Khánh Xuân"
        serviceName = arguments?.getString("SERVICE_NAME") ?: ""

        val tvStoreName: TextView = view.findViewById(R.id.info_store_name)
        val tvDateTime: TextView = view.findViewById(R.id.info_date_time)
        val tvSpecialist: TextView = view.findViewById(R.id.info_specialist)
        val tvService: TextView = view.findViewById(R.id.info_service)
        
        val btnBack: ImageView = view.findViewById(R.id.btn_back)
        val btnHome: TextView = view.findViewById(R.id.btn_home)

        tvStoreName.text = storeName
        tvDateTime.text = dateTime
        tvSpecialist.text = specialist
        tvService.text = serviceName

        btnBack.setOnClickListener { 
            // Về màn hình trước đó
            parentFragmentManager.popBackStack() 
        }

        btnHome.setOnClickListener {
            // Về trang chủ của app
            // Cần pop toàn bộ backstack và chuyển về HomeFragment
            val fm = parentFragmentManager
            for (i in 0 until fm.backStackEntryCount) {
                fm.popBackStack()
            }
            fm.beginTransaction()
                .replace(R.id.main_container, HomeFragment())
                .commit()
        }
    }

    override fun observeViewModel() {
        // Not used
    }
}
