package com.veganbeauty.app.features.myskin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCancelledBinding

class BookingDetailCancelledFragment : Fragment() {

    private var _binding: SkinFragmentBookingDetailCancelledBinding? = null
    private val binding get() = _binding!!

    companion object {
        private var bookingData: BookingHistoryEntity? = null
        
        fun newInstance(data: BookingHistoryEntity): BookingDetailCancelledFragment {
            val fragment = BookingDetailCancelledFragment()
            bookingData = data
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentBookingDetailCancelledBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.skinDetailBtnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val data = bookingData
        if (data != null) {
            populateUI(data)
        } else {
            Toast.makeText(context, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
        
        binding.skinDetailBtnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Mã đặt lịch", data?.id ?: "")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show()
        }
        
        binding.skinDetailBtnContactStore.setOnClickListener {
            Toast.makeText(context, "Liên hệ chi nhánh clicked", Toast.LENGTH_SHORT).show()
        }

        binding.skinDetailBtnRebookNow.setOnClickListener {
            Toast.makeText(context, "Đặt lại ngay clicked", Toast.LENGTH_SHORT).show()
        }

        binding.skinDetailBtnRebookBottom.setOnClickListener {
            Toast.makeText(context, "Đặt lại lịch hẹn clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.skinDetailBtnBookOtherBottom.setOnClickListener {
            Toast.makeText(context, "Đặt dịch vụ khác clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateUI(data: BookingHistoryEntity) {
        binding.skinDetailServiceName.text = data.serviceName
        binding.skinDetailOrderId.text = "Mã đặt lịch: ${data.id}"
        binding.skinDetailDate.text = "${data.dateDisplay} • ${data.dayOfWeek}"
        binding.skinDetailTime.text = "${data.time} (${data.duration})"
        
        if (data.cancelledAt.isNotEmpty()) {
            binding.skinDetailCancelledAt.text = "Đã huỷ vào: ${data.cancelledAt}"
        }

        // Store Image
        if (data.storeImage.isNotEmpty()) {
            binding.skinDetailStoreImage.load(data.storeImage) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        }

        // Cancel Reason
        if (data.cancelReason.isNotEmpty()) {
            binding.skinDetailCancelReason.text = data.cancelReason
        }

        // Store Info
        binding.skinDetailStoreName.text = data.storeName
        binding.skinDetailStoreAddress.text = data.storeAddress
        binding.skinDetailStorePhone.text = data.storePhone

        // User Info
        binding.skinDetailUserName.text = data.userName
        binding.skinDetailUserPhone.text = data.userPhone
        binding.skinDetailUserEmail.text = data.userEmail
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
