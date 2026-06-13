package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailCompletedBinding

class BookingDetailCompletedFragment : Fragment() {

    private var _binding: SkinFragmentBookingDetailCompletedBinding? = null
    private val binding get() = _binding!!

    companion object {
        private var bookingData: BookingHistoryEntity? = null
        
        fun newInstance(data: BookingHistoryEntity): BookingDetailCompletedFragment {
            val fragment = BookingDetailCompletedFragment()
            bookingData = data
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentBookingDetailCompletedBinding.inflate(inflater, container, false)
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
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Mã đặt lịch", data?.id ?: "")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show()
        }
        
        binding.skinDetailBtnRebook.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, BookingFragment())
                .addToBackStack(null)
                .commit()
        }
        
        binding.skinDetailBtnBookOther.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, BookingFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun populateUI(data: BookingHistoryEntity) {
        binding.skinDetailServiceName.text = data.serviceName
        binding.skinDetailOrderId.text = "Mã đặt lịch: ${data.id}"
        binding.skinDetailDate.text = "${data.dateDisplay} • ${data.dayOfWeek}"
        binding.skinDetailTime.text = "${data.time} (${data.duration})"
        
        if (data.completedAt.isNotEmpty()) {
            binding.skinDetailCompletedAt.text = "Hoàn thành lúc: ${data.completedAt}"
        }

        // Store Image
        if (data.storeImage.isNotEmpty()) {
            binding.skinDetailStoreImage.load(data.storeImage) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        }

        // Skin Results
        binding.skinDetailResultsList.removeAllViews()
        for (result in data.skinResults) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, 8)
            }
            val icon = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_check_circle)
                setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                layoutParams = LinearLayout.LayoutParams(40, 40)
            }
            val text = TextView(requireContext()).apply {
                text = result
                setTextColor(ContextCompat.getColor(requireContext(), R.color.content))
                textSize = 10f
                setPadding(12, 0, 0, 0)
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            row.addView(icon)
            row.addView(text)
            binding.skinDetailResultsList.addView(row)
        }

        // Store Info
        binding.skinDetailStoreName.text = data.storeName
        binding.skinDetailStoreAddress.text = data.storeAddress
        binding.skinDetailStorePhone.text = data.storePhone

        // Consultant Info
        binding.skinDetailConsultantName.text = data.consultantName
        if (data.consultantAvatar.isNotEmpty()) {
            binding.skinDetailConsultantAvatar.load(data.consultantAvatar) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        }

        // Review Info
        binding.skinDetailUserRatingNum.text = String.format("%.1f", data.userRating)
        binding.skinDetailUserReviewText.text = "“${data.userReview}”"
        binding.skinDetailReviewDate.text = data.reviewDate

        // Images Before/After
        if (data.beforeImage.isNotEmpty()) {
            binding.skinDetailBeforeImg.load(data.beforeImage) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
            }
        }
        if (data.afterImage.isNotEmpty()) {
            binding.skinDetailAfterImg.load(data.afterImage) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
            }
        }

        // Points
        binding.skinDetailEarnedPoints.text = "+${data.earnedPoints}"
        binding.skinDetailTotalPoints.text = "Tổng điểm hiện tại: ${data.totalPoints} điểm"

        // Next Appointment
        binding.skinDetailNextApptText.text = data.nextAppointmentText
        binding.skinDetailNextApptDate.text = data.nextAppointmentDate
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
