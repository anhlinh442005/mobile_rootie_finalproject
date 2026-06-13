package com.veganbeauty.app.features.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.veganbeauty.app.databinding.AccountVoucherDetailFragmentBinding
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AccountVoucherDetailFragment : RootieFragment() {

    private var _binding: AccountVoucherDetailFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: OrderRepository

    private var voucherId = ""
    private var voucherTitle = ""
    private var voucherDescription = ""
    private var voucherCode = ""
    private var voucherStatus = ""
    private var voucherHsd = ""
    private var voucherType = ""
    private var fromGift = false
    private var minOrderValue = 0
    private var applicableProducts = "Tất cả sản phẩm"
    private var offerType = "fixed_amount"
    private var discountValue = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountVoucherDetailFragmentBinding.inflate(inflater, container, false)

        arguments?.let {
            voucherId = it.getString(ARG_ID) ?: ""
            voucherTitle = it.getString(ARG_TITLE) ?: ""
            voucherDescription = it.getString(ARG_DESCRIPTION) ?: ""
            voucherCode = it.getString(ARG_CODE) ?: ""
            voucherStatus = it.getString(ARG_STATUS) ?: ""
            voucherHsd = it.getString(ARG_HSD) ?: ""
            voucherType = it.getString(ARG_TYPE) ?: ""
            fromGift = it.getBoolean(ARG_FROM_GIFT)
            minOrderValue = it.getInt(ARG_MIN_ORDER_VALUE)
            applicableProducts = it.getString(ARG_APPLICABLE_PRODUCTS) ?: "Tất cả sản phẩm"
            offerType = it.getString(ARG_OFFER_TYPE) ?: "fixed_amount"
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
        val context = requireContext()

        // 1. Back navigation
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 2. Share actions
        val shareAction = View.OnClickListener {
            copyToClipboard(context, voucherCode)
            Toast.makeText(context, "Đã sao chép và chia sẻ mã voucher $voucherCode!", Toast.LENGTH_SHORT).show()
        }
        binding.btnShare.setOnClickListener(shareAction)
        binding.btnShareVoucher.setOnClickListener(shareAction)

        // 3. Delete action
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        // 4. Ticket Card Bindings
        // Set Icons depending on type
        when (voucherType.lowercase().replace(" ", "").replace("_", "")) {
            "freeship", "voucherfreeship" -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_truck)
            }
            "gift", "productgift", "product", "product_gift" -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_gift)
            }
            else -> {
                binding.ivVoucherIcon.setImageResource(R.drawable.ic_voucher)
            }
        }

        // Format Big Title and Subtitle for Ticket
        val formattedTitle = when {
            voucherType.lowercase().contains("freeship") || voucherType.lowercase().contains("free ship") -> "Free Ship"
            offerType == "percentage" -> "Giảm $discountValue%"
            offerType == "fixed_amount" -> "Giảm ${discountValue / 1000}K"
            offerType == "product_gift" -> "Quà tặng"
            else -> voucherTitle
        }
        binding.tvVoucherTitle.text = formattedTitle

        val formattedSubtitle = "Tối đa ${minOrderValue / 1000}k"
        binding.tvVoucherSubtitle.text = formattedSubtitle

        // 5. Description Block
        binding.tvDetailTitle.text = voucherTitle
        binding.tvDetailDesc.text = voucherDescription

        // 6. Voucher Code Block
        binding.tvVoucherCode.text = voucherCode
        binding.btnCopyCode.setOnClickListener {
            copyToClipboard(context, voucherCode)
            Toast.makeText(context, "Đã sao chép mã voucher $voucherCode thành công!", Toast.LENGTH_SHORT).show()
        }

        // 7. Conditions Card
        binding.tvCondMinOrder.text = "Áp dụng cho đơn hàng từ ${formatCurrency(minOrderValue)} VNĐ."

        // 8. Info Table Card
        val displayHsd = formatHsd(voucherHsd)
        binding.tvInfoExpiry.text = displayHsd

        val remainingText = computeRemainingDays(voucherHsd)
        binding.tvInfoRemaining.text = remainingText

        if (remainingText == "Hết hạn" || voucherStatus == "expired") {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#C62828"))
            binding.tvInfoStatus.text = "Hết hiệu lực"
            binding.tvInfoStatus.setTextColor(Color.parseColor("#C62828"))
            
            // Disable Use Now button if expired
            binding.btnUseNow.isEnabled = false
            binding.btnUseNow.text = "Đã hết hạn"
            binding.btnUseNow.backgroundTintList = ContextCompat.getColorStateList(context, R.color.gray_light)
            binding.btnUseNow.setTextColor(ContextCompat.getColor(context, R.color.gray_dark))
        } else if (remainingText == "Hôm nay" || voucherStatus == "expiring") {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#C62828"))
            binding.tvInfoStatus.text = "Có hiệu lực"
        } else {
            binding.tvInfoRemaining.setTextColor(Color.parseColor("#7A9161"))
            binding.tvInfoStatus.text = "Có hiệu lực"
        }

        binding.tvInfoUsage.text = "0/1"

        // 9. Use now button
        binding.btnUseNow.setOnClickListener {
            deleteVoucherFromList { success ->
                if (success) {
                    Toast.makeText(context, "Mã voucher $voucherCode đã được áp dụng cho đơn hàng của bạn!", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                } else {
                    Toast.makeText(context, "Áp dụng voucher thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteVoucherFromList(onComplete: (Boolean) -> Unit) {
        if (voucherId.startsWith("db_")) {
            val dbId = voucherId.substringAfter("db_").toIntOrNull()
            if (dbId != null) {
                lifecycleScope.launch {
                    val success = repository.deleteUserGiftById(dbId)
                    onComplete(success)
                }
            } else {
                onComplete(false)
            }
        } else {
            AccountVoucherFragment.deletedSystemVoucherIdsStatic.add(voucherId)
            onComplete(true)
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Xoá voucher")
            .setMessage("Bạn có chắc chắn muốn xoá $voucherTitle khỏi ví voucher của bạn không?")
            .setPositiveButton("Xác nhận") { _, _ ->
                lifecycleScope.launch {
                    val context = requireContext()
                    if (voucherId.startsWith("db_")) {
                        val dbId = voucherId.substringAfter("db_").toIntOrNull()
                        if (dbId != null) {
                            val success = repository.deleteUserGiftById(dbId)
                            if (success) {
                                Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            } else {
                                Toast.makeText(context, "Xoá voucher thất bại", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // For system vouchers, we communicate the deletion via memory or pop back
                        // In list fragmentation, system deletions are stored inside deletedSystemVoucherIds.
                        // We will trigger pop back. Since the parent fragment will reload and if we let parent know,
                        // we can pass deletion back, but wait: the list deletes locally via list callbacks.
                        // Let's set a shared key or simply send deletion down.
                        // Since system vouchers are static, we can write a preference or notify parent:
                        // For simplicity, we can delete system vouchers on parent side. Let's send result bundle or static state!
                        // Let's create a static deleted system set in AccountVoucherFragment so we can append directly!
                        AccountVoucherFragment.deletedSystemVoucherIdsStatic.add(voucherId)
                        Toast.makeText(context, "Đã xoá voucher thành công", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun formatCurrency(amount: Int): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = '.'
        }
        val df = DecimalFormat("#,###", symbols)
        return df.format(amount)
    }

    private fun formatHsd(hsdStr: String): String {
        return try {
            val inputSdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputSdf.parse(hsdStr) ?: return hsdStr
            val outputSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            outputSdf.format(date)
        } catch (e: Exception) {
            if (hsdStr.contains(" ")) hsdStr.split(" ")[0] else hsdStr
        }
    }

    private fun computeRemainingDays(hsdStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val expiryDate = sdf.parse(hsdStr) ?: return "Còn hiệu lực"
            
            val today = Calendar.getInstance().apply {
                set(Calendar.YEAR, 2026)
                set(Calendar.MONTH, Calendar.JUNE)
                set(Calendar.DAY_OF_MONTH, 11)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val expiry = Calendar.getInstance().apply {
                time = expiryDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val diffMs = expiry.time - today.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            
            if (diffDays < 0) {
                "Hết hạn"
            } else if (diffDays == 0L) {
                "Hôm nay"
            } else {
                "$diffDays ngày"
            }
        } catch (e: Exception) {
            "Còn hiệu lực"
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Voucher Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_TITLE = "arg_title"
        private const val ARG_DESCRIPTION = "arg_description"
        private const val ARG_CODE = "arg_code"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_HSD = "arg_hsd"
        private const val ARG_TYPE = "arg_type"
        private const val ARG_FROM_GIFT = "arg_from_gift"
        private const val ARG_MIN_ORDER_VALUE = "arg_min_order_value"
        private const val ARG_APPLICABLE_PRODUCTS = "arg_applicable_products"
        private const val ARG_OFFER_TYPE = "arg_offer_type"
        private const val ARG_DISCOUNT_VALUE = "arg_discount_value"

        fun newInstance(item: VoucherItem): AccountVoucherDetailFragment {
            val fragment = AccountVoucherDetailFragment()
            val args = Bundle()
            args.putString(ARG_ID, item.id)
            args.putString(ARG_TITLE, item.title)
            args.putString(ARG_DESCRIPTION, item.description)
            args.putString(ARG_CODE, item.code)
            args.putString(ARG_STATUS, item.status)
            args.putString(ARG_HSD, item.hsd)
            args.putString(ARG_TYPE, item.type)
            args.putBoolean(ARG_FROM_GIFT, item.fromGift)
            args.putInt(ARG_MIN_ORDER_VALUE, item.minOrderValue)
            args.putString(ARG_APPLICABLE_PRODUCTS, item.applicableProducts)
            args.putString(ARG_OFFER_TYPE, item.offerType)
            args.putInt(ARG_DISCOUNT_VALUE, item.discountValue)
            fragment.arguments = args
            return fragment
        }
    }
}
