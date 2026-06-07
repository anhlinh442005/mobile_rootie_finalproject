package com.veganbeauty.app.features.account.reward

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.AccountRewardDetailFragmentBinding
import kotlinx.coroutines.launch

class AccountRewardDetailFragment : RootieFragment() {

    private var _binding: AccountRewardDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: OrderRepository

    private var giftId = ""
    private var giftTitle = ""
    private var giftDescription = ""
    private var giftCost = 0
    private var giftExpiryDate = ""
    private var giftCode = ""
    private var giftType = ""
    private var isOwned = false
    private var dbId = -1
    private var rankRequired = "Đồng"

    private var currentPoints = 8500

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountRewardDetailFragmentBinding.inflate(inflater, container, false)
        
        arguments?.let {
            giftId = it.getString(ARG_GIFT_ID) ?: ""
            giftTitle = it.getString(ARG_TITLE) ?: ""
            giftDescription = it.getString(ARG_DESCRIPTION) ?: ""
            giftCost = it.getInt(ARG_COST)
            giftExpiryDate = it.getString(ARG_EXPIRY) ?: ""
            giftCode = it.getString(ARG_CODE) ?: ""
            giftType = it.getString(ARG_TYPE) ?: ""
            isOwned = it.getBoolean(ARG_IS_OWNED)
            dbId = it.getInt(ARG_DB_ID)
            rankRequired = it.getString(ARG_RANK_REQUIRED) ?: "Đồng"
        }

        setupRepository()
        return binding.root
    }

    private fun setupRepository() {
        val db = RootieDatabase.getDatabase(requireContext())
        repository = OrderRepository(db.orderDao(), db.rewardPointDao(), db.userGiftDao(), LocalJsonReader(requireContext()))
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnShare.setOnClickListener {
            Toast.makeText(requireContext(), "Đã copy link chia sẻ quà tặng!", Toast.LENGTH_SHORT).show()
        }

        // Configure ticket view details
        binding.tvGiftTitle.text = giftTitle
        binding.tvGiftSubtitle.text = giftDescription
        binding.tvGiftCost.text = String.format("%,d xu", giftCost).replace(',', '.')
        binding.tvExpiryDate.text = giftExpiryDate
        
        // Bind the code chip
        binding.tvUsageCodeChip.text = giftCode
        
        val days = when (giftExpiryDate) {
            "Hôm nay" -> "Hôm nay"
            "15/12/2026" -> "15 ngày"
            "30/11/2026" -> "Hết hạn"
            "31/12/2026" -> "27 ngày"
            "31/12/2027" -> "Còn 1 năm"
            else -> "Còn hiệu lực"
        }
        binding.tvRemainingDays.text = days

        if (giftExpiryDate == "Hôm nay") {
            binding.tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text))
            binding.tvStatus.text = "Gấp"
        } else if (days == "Hết hạn" || giftExpiryDate == "30/11/2026") {
            binding.tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
            binding.tvRemainingDays.text = "Hết hạn"
            binding.tvStatus.text = "Hết hiệu lực"
        } else {
            binding.tvRemainingDays.setTextColor(Color.parseColor("#7A9161")) // Custom green color from screenshot
            binding.tvStatus.text = "Có hiệu lực"
        }

        binding.tvUsageLimit.text = if (giftType == "voucher") "0/1" else "Chỉ nhận 1 lần"

        // Setup owned details vs exchange details
        if (isOwned) {
            binding.btnRedeemNow.text = "Đã sở hữu"
            binding.btnRedeemNow.isEnabled = false
            binding.btnRedeemNow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_light)
            binding.btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
            
            val removeAction = View.OnClickListener {
                showRemoveConfirmationDialog()
            }
            binding.btnRemove.setOnClickListener(removeAction)
            binding.btnDelete.setOnClickListener(removeAction)
            
            binding.btnRemove.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.VISIBLE
        } else {
            binding.btnRemove.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
            
            binding.btnRedeemNow.setOnClickListener {
                handleRedeemAction()
            }
        }
    }

    override fun observeViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            db.rewardPointDao().getTotalPointsFlow().collect { points ->
                val pointsVal = points ?: 0
                currentPoints = pointsVal
                binding.tvUserBalance.text = "Bạn hiện có: ${String.format("%,d xu", pointsVal).replace(',', '.')}"
 
                val isLockedByRank = (rankRequired == "Vàng" && pointsVal < 10000) || (rankRequired == "Kim Cương" && pointsVal < 20000)
                val isNotEnoughPoints = pointsVal < giftCost
 
                // Progress Bar styling (100% if enough points)
                if (pointsVal >= giftCost) {
                    binding.pbRedeemProgress.progress = 100
                    binding.tvQualifyStatus.text = "✓ Đủ điều kiện"
                    binding.tvQualifyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                } else {
                    binding.pbRedeemProgress.progress = (pointsVal * 100 / giftCost)
                    binding.tvQualifyStatus.text = "✗ Chưa đủ xu"
                    binding.tvQualifyStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text))
                }
 
                if (!isOwned) {
                    if (isLockedByRank) {
                        binding.btnRedeemNow.isEnabled = false
                        binding.btnRedeemNow.text = "Chưa đủ hạng"
                        binding.btnRedeemNow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_light)
                        binding.btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                    } else if (isNotEnoughPoints) {
                        binding.btnRedeemNow.isEnabled = false
                        binding.btnRedeemNow.text = "Không đủ xu"
                        binding.btnRedeemNow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.gray_light)
                        binding.btnRedeemNow.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
                    } else {
                        binding.btnRedeemNow.isEnabled = true
                        binding.btnRedeemNow.text = "Đổi quà"
                        binding.btnRedeemNow.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary)
                        binding.btnRedeemNow.setTextColor(Color.WHITE)
                    }
                }
            }
        }
    }

    private fun handleRedeemAction() {
        if (currentPoints < giftCost) {
            Toast.makeText(requireContext(), "Bạn không đủ xu để đổi quà tặng này!", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val success = repository.redeemGift(
                giftId = giftId,
                title = giftTitle,
                description = giftDescription,
                cost = giftCost,
                expiryDate = giftExpiryDate,
                code = giftCode,
                giftType = giftType
            )

            if (success) {
                selectRewardTabOnResume = 1 // Redirect to My Gifts tab

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Đổi quà thành công!")
                    .setMessage("Chúc mừng! Bạn đã đổi thành công $giftTitle. Quà tặng đã được lưu vào mục 'Quà của tôi'.")
                    .setPositiveButton("Xem danh sách quà") { _, _ ->
                        parentFragmentManager.popBackStack()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                Toast.makeText(requireContext(), "Đổi quà thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xoá quà tặng")
            .setMessage("Bạn có chắc chắn muốn xoá $giftTitle khỏi danh sách quà của tôi không?")
            .setPositiveButton("Xác nhận") { _, _ ->
                lifecycleScope.launch {
                    val deleted = repository.deleteUserGiftById(dbId)
                    if (deleted) {
                        Toast.makeText(requireContext(), "Đã xoá quà tặng khỏi danh sách!", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), "Xoá quà tặng thất bại!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_GIFT_ID = "arg_gift_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESCRIPTION = "arg_description"
        private const val ARG_COST = "arg_cost"
        private const val ARG_EXPIRY = "arg_expiry"
        private const val ARG_CODE = "arg_code"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_IS_OWNED = "arg_is_owned"
        private const val ARG_DB_ID = "arg_db_id"
        private const val ARG_RANK_REQUIRED = "arg_rank_required"

        // Global flag to redirect back to target tab
        var selectRewardTabOnResume = 0

        fun newInstance(
            giftId: String,
            title: String,
            description: String,
            cost: Int,
            expiryDate: String,
            code: String,
            giftType: String,
            isOwned: Boolean,
            dbId: Int,
            rankRequired: String
        ): AccountRewardDetailFragment {
            val fragment = AccountRewardDetailFragment()
            val args = Bundle()
            args.putString(ARG_GIFT_ID, giftId)
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESCRIPTION, description)
            args.putInt(ARG_COST, cost)
            args.putString(ARG_EXPIRY, expiryDate)
            args.putString(ARG_CODE, code)
            args.putString(ARG_TYPE, giftType)
            args.putBoolean(ARG_IS_OWNED, isOwned)
            args.putInt(ARG_DB_ID, dbId)
            args.putString(ARG_RANK_REQUIRED, rankRequired)
            fragment.arguments = args
            return fragment
        }
    }
}

