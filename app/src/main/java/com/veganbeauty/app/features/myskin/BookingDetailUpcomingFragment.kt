package com.veganbeauty.app.features.myskin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.veganbeauty.app.core.base.RootieFragment
import coil.load
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity
import com.veganbeauty.app.databinding.SkinFragmentBookingDetailUpcomingBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BookingDetailUpcomingFragment : RootieFragment() {

    private var _binding: SkinFragmentBookingDetailUpcomingBinding? = null
    private val binding get() = _binding!!

    // We will pass the data as a string (JSON) or pass arguments individually.
    // For simplicity, we can pass fields individually or use a static variable if we don't have Parcelable.
    // Let's create a companion object pattern.
    
    companion object {
        private var bookingData: BookingHistoryEntity? = null
        
        fun newInstance(data: BookingHistoryEntity): BookingDetailUpcomingFragment {
            val fragment = BookingDetailUpcomingFragment()
            bookingData = data
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SkinFragmentBookingDetailUpcomingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.skinDetailBtnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val data = bookingData ?: run {
            Toast.makeText(context, "Lỗi dữ liệu", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        populateUI(data)
        
        binding.skinDetailBtnCopy.setOnClickListener {
            // Copy to clipboard
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Mã đặt lịch", data.id)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Đã sao chép mã đặt lịch", Toast.LENGTH_SHORT).show()
        }
        
        binding.skinDetailBtnPrimary.setOnClickListener {
            Toast.makeText(context, "Xem chi tiết clicked", Toast.LENGTH_SHORT).show()
        }
        
        binding.skinDetailBtnCancel.setOnClickListener {
            showCancelDialog(data)
        }
    }

    private fun showCancelDialog(data: BookingHistoryEntity) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.skin_dialog_cancel_booking, null)
        val dialogBuilder = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            
        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        
        // Setup Dialog Data
        dialogView.findViewById<android.widget.TextView>(R.id.tv_date).text = data.dateDisplay
        dialogView.findViewById<android.widget.TextView>(R.id.tv_month_day).text = data.dayOfWeek
        dialogView.findViewById<android.widget.TextView>(R.id.tv_service_name).text = data.serviceName
        dialogView.findViewById<android.widget.TextView>(R.id.tv_time).text = "${data.time} (${data.duration})"
        dialogView.findViewById<android.widget.TextView>(R.id.tv_store_name).text = data.storeName
        dialogView.findViewById<android.widget.TextView>(R.id.tv_store_address).text = data.storeAddress
        
        // Setup Spinner
        val spReason = dialogView.findViewById<android.widget.Spinner>(R.id.sp_reason)
        val reasons = listOf("Thay đổi lịch trình", "Tìm được địa điểm khác", "Lý do sức khoẻ", "Đã đặt nhầm dịch vụ", "Lý do khác")
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasons)
        spReason.adapter = adapter
        
        val etOtherReason = dialogView.findViewById<android.widget.EditText>(R.id.et_other_reason)
        
        dialogView.findViewById<View>(R.id.btn_close).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_back).setOnClickListener { dialog.dismiss() }
        
        dialogView.findViewById<View>(R.id.btn_confirm_cancel).setOnClickListener {
            val selectedReason = spReason.selectedItem.toString()
            val otherReason = etOtherReason.text.toString().trim()
            val finalReason = if (selectedReason == "Lý do khác" && otherReason.isNotEmpty()) otherReason else selectedReason
            
            // Update in memory
            com.veganbeauty.app.data.local.LocalJsonReader(requireContext()).updateBookingStatus(data.id, "Đã huỷ", finalReason)
            
            // Update UI locally
            val updatedData = data.copy(status = "Đã huỷ", cancelReason = finalReason)
            bookingData = updatedData
            populateUI(updatedData)

            // Update on Firestore
            viewLifecycleOwner.lifecycleScope.launch {
                com.veganbeauty.app.data.remote.FirestoreService().updateBookingStatus(data.id, "Đã huỷ", finalReason)
            }
            
            Toast.makeText(requireContext(), "Hủy lịch thành công", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun populateUI(data: BookingHistoryEntity) {
        binding.skinDetailServiceName.text = data.serviceName
        binding.skinDetailOrderId.text = "Mã đặt lịch: ${data.id}"
        binding.skinDetailDate.text = "${data.dateDisplay}\n${data.dayOfWeek}"
        binding.skinDetailTime.text = "${data.time} (${data.duration})"
        
        // Status Tag
        binding.skinDetailStatusTag.text = data.status
        when (data.status) {
            "Sắp diễn ra", "Chờ xác nhận", "pending" -> {
                if (data.status.equals("Chờ xác nhận", ignoreCase = true) || data.status.equals("pending", ignoreCase = true)) {
                    binding.skinDetailStatusTag.text = "Chờ xác nhận"
                    binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#FF9800")) // Orange
                } else {
                    binding.skinDetailStatusTag.setBackgroundResource(R.drawable.skin_bg_btn_book) // Blue/Green
                }
                binding.skinDetailStatusTag.setTextColor(Color.WHITE)
                binding.skinDetailBottomActions.visibility = View.VISIBLE
            }
            "Đã hoàn thành" -> {
                binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#CD853F")) // Orange/Brown
                binding.skinDetailStatusTag.setTextColor(Color.WHITE)
                binding.skinDetailBottomActions.visibility = View.GONE
            }
            "Đã huỷ" -> {
                binding.skinDetailStatusTag.setBackgroundColor(Color.parseColor("#CD5C5C")) // Red
                binding.skinDetailStatusTag.setTextColor(Color.WHITE)
                binding.skinDetailBottomActions.visibility = View.GONE
            }
            else -> {
                binding.skinDetailStatusTag.setBackgroundColor(Color.GRAY)
                binding.skinDetailStatusTag.setTextColor(Color.WHITE)
                binding.skinDetailBottomActions.visibility = View.GONE
            }
        }
        
        // Store Image
        if (data.storeImage.isNotEmpty()) {
            binding.skinDetailStoreImage.load(data.storeImage) {
                placeholder(R.drawable.imv_logo)
                error(R.drawable.imv_logo)
                crossfade(true)
            }
        } else {
            binding.skinDetailStoreImage.setImageResource(R.drawable.imv_logo)
        }

        // Store Info
        binding.skinDetailStoreName.text = data.storeName
        binding.skinDetailStoreAddress.text = data.storeAddress
        binding.skinDetailStorePhone.text = if (data.storePhone.isNotEmpty()) data.storePhone else "(027) 7100 2020"

        // User Info
        binding.skinDetailUserName.text = data.userName
        binding.skinDetailUserPhone.text = data.userPhone
        binding.skinDetailUserEmail.text = data.userEmail

        // Note Info
        if (data.note.isNotEmpty()) {
            binding.skinDetailNote.text = data.note
            binding.skinDetailNoteContainer.visibility = View.VISIBLE
        } else {
            binding.skinDetailNoteContainer.visibility = View.GONE
        }

        // Policy Info
        if (data.policy.isNotEmpty()) {
            binding.skinDetailPolicy.text = data.policy
            binding.skinDetailPolicyContainer.visibility = View.VISIBLE
        } else {
            binding.skinDetailPolicyContainer.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
