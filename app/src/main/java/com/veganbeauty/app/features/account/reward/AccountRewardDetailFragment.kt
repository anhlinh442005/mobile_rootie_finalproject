package com.veganbeauty.app.features.account.reward

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
    private var minOrderValue = 0
    private var applicableProducts = "Tất cả sản phẩm"
    private var offerType = "fixed_amount"
    private var productId: String? = null
    private var discountValue = 0

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
            minOrderValue = it.getInt(ARG_MIN_ORDER_VALUE)
            applicableProducts = it.getString(ARG_APPLICABLE_PRODUCTS) ?: "Tất cả sản phẩm"
            offerType = it.getString(ARG_OFFER_TYPE) ?: "fixed_amount"
            productId = it.getString(ARG_PRODUCT_ID)
            discountValue = it.getInt(ARG_DISCOUNT_VALUE)
        }

        setupRepository()
        return binding.root
    }

    private fun setupRepository() {
        val db = RootieDatabase.getDatabase(requireContext())
        repository = OrderRepository(
            db.orderDao(),
            db.rewardPointDao(),
            db.userGiftDao(),
            LocalJsonReader(requireContext())
        )
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
        
        // Expiry date format split
        val displayDate = if (giftExpiryDate.contains(" ")) giftExpiryDate.split(" ")[0] else giftExpiryDate
        binding.tvExpiryDate.text = displayDate
        
        // Bind the code chip (Mask code if not owned!)
        if (isOwned) {
            binding.tvUsageCodeChip.text = giftCode
        } else {
            binding.tvUsageCodeChip.text = "******"
        }
        
        val days = when (displayDate) {
            "Hôm nay", "2026-06-11" -> "Hôm nay"
            "2026-12-15" -> "Còn 6 tháng"
            "2026-12-31" -> "Còn 6 tháng"
            "2027-01-15" -> "Còn 7 tháng"
            else -> "Còn hiệu lực"
        }
        binding.tvRemainingDays.text = days

        if (days == "Hôm nay") {
            binding.tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_cancelled_text))
            binding.tvStatus.text = "Gấp"
        } else if (days == "Hết hạn") {
            binding.tvRemainingDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_dark))
            binding.tvRemainingDays.text = "Hết hạn"
            binding.tvStatus.text = "Hết hiệu lực"
        } else {
            binding.tvRemainingDays.setTextColor(Color.parseColor("#7A9161"))
            binding.tvStatus.text = "Có hiệu lực"
        }

        binding.tvUsageLimit.text = if (giftType.startsWith("voucher")) "0/1" else "Chỉ nhận 1 lần"

        // Setup dynamic applicability checkmark rules
        binding.tvRuleApplicableProducts.text = "Áp dụng cho: $applicableProducts"
        binding.tvRuleMinOrderValue.text = if (minOrderValue > 0) {
            "Hóa đơn tối thiểu " + String.format("%,d đ", minOrderValue).replace(',', '.')
        } else {
            "Áp dụng cho mọi hóa đơn"
        }
        
        binding.tvRuleOfferType.text = when (offerType) {
            "percentage" -> "Ưu đãi: Giảm $discountValue%"
            "fixed_amount" -> "Ưu đãi: Giảm " + String.format("%,d đ", discountValue).replace(',', '.')
            "product_gift" -> "Ưu đãi: Tặng kèm sản phẩm"
            else -> "Ưu đãi đặc quyền từ Rootie"
        }

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
 
                val isLockedByRank = (rankRequired == "Vàng" && pointsVal < 10000) || 
                                     (rankRequired == "VIP" && pointsVal < 20000) ||
                                     (rankRequired == "Kim Cương" && pointsVal < 20000)
                val isNotEnoughPoints = pointsVal < giftCost
 
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
                giftType = giftType,
                minOrderValue = minOrderValue,
                applicableProducts = applicableProducts,
                offerType = offerType,
                productId = productId,
                discountValue = discountValue
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
        private const val ARG_MIN_ORDER_VALUE = "arg_min_order_value"
        private const val ARG_APPLICABLE_PRODUCTS = "arg_applicable_products"
        private const val ARG_OFFER_TYPE = "arg_offer_type"
        private const val ARG_PRODUCT_ID = "arg_product_id"
        private const val ARG_DISCOUNT_VALUE = "arg_discount_value"

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
            rankRequired: String,
            minOrderValue: Int,
            applicableProducts: String,
            offerType: String,
            productId: String?,
            discountValue: Int
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
            args.putInt(ARG_MIN_ORDER_VALUE, minOrderValue)
            args.putString(ARG_APPLICABLE_PRODUCTS, applicableProducts)
            args.putString(ARG_OFFER_TYPE, offerType)
            args.putString(ARG_PRODUCT_ID, productId)
            args.putInt(ARG_DISCOUNT_VALUE, discountValue)
            fragment.arguments = args
            return fragment
        }
    }
}
