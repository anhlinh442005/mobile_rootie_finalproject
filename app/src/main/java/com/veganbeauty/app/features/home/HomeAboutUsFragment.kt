package com.veganbeauty.app.features.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.utils.NavAppUtils

class HomeAboutUsFragment : RootieFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.home_about_us_fragment, container, false)
    }

    override fun setupUI(view: View) {
        // Setup bottom nav — highlight "Trang chủ" tab
        NavAppUtils.setupNavApp(this, view, R.id.nav_home)

        // Setup header actions
        view.findViewById<View>(R.id.home_header_search_bar)?.setOnClickListener {
            Toast.makeText(context, "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.home_header_qr_btn)?.setOnClickListener {
            Toast.makeText(context, "Mở trình quét mã QR", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.home_header_notification_btn)?.setOnClickListener {
            Toast.makeText(context, "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }
    }

    override fun observeViewModel() {
        // Not used
    }
}
