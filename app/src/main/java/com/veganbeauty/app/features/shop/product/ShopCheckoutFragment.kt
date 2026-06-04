package com.veganbeauty.app.features.shop.product

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.databinding.ShopFragmentCheckoutBinding
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShopCheckoutFragment : RootieFragment() {

    private var _binding: ShopFragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var checkoutItems: ArrayList<CartItemEntity> = arrayListOf()
    private lateinit var adapter: ShopCheckoutProductAdapter
    private lateinit var database: RootieDatabase

    // Order properties
    private var deliveryType = "Giao hàng tận nơi"
    
    // Address list variables
    private var selectedAddressIndex = 1
    private var address1Name = "Bảo Nguyên"
    private var address1Phone = "0123456789"
    private var address1Details = "123 Lê Lợi, phường 5, quận 1, Hồ Chí Minh"

    private var address2Name = "Nguyên Bảo"
    private var address2Phone = "0987654321"
    private var address2Details = "321 Lê Lợi, phường 5, quận 1, Hồ Chí Minh"

    private var recipientName = "Bảo Nguyên"
    private var recipientPhone = "0123456789"
    private var recipientAddress = "123 Lê Lợi, phường 5, quận 1, Hồ Chí Minh"
    private var paymentMethod = "Thanh toán tiền mặt khi nhận hàng"
    private var isVoucherApplied = false
    private var voucherDiscountAmount = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            checkoutItems = it.getSerializable(ARG_CHECKOUT_ITEMS) as? ArrayList<CartItemEntity> ?: arrayListOf()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        setupRecyclerView()
        setupListeners()
        updateDeliveryUI()
        updatePaymentMethodUI()
        calculatePrices()
    }

    private fun setupRecyclerView() {
        adapter = ShopCheckoutProductAdapter()
        binding.rvCheckoutItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCheckoutItems.adapter = adapter
        adapter.submitList(checkoutItems)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 1. Delivery Type Change
        binding.llDeliveryType.setOnClickListener {
            showDeliveryTypeBottomSheet()
        }

        // 2. Address Change
        binding.btnChangeAddress.setOnClickListener {
            showAddressSelectionBottomSheet()
        }

        // 3. Payment Method Change
        binding.btnChangePaymentMethod.setOnClickListener {
            showPaymentMethodBottomSheet()
        }

        // 4. Voucher Row click
        binding.llVoucherRow.setOnClickListener {
            if (!isVoucherApplied) {
                isVoucherApplied = true
                voucherDiscountAmount = 20000L // mock 20k voucher discount
                Toast.makeText(requireContext(), "Đã áp dụng mã giảm giá 20.000đ thành công!", Toast.LENGTH_SHORT).show()
                binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5F2FF")))
            } else {
                isVoucherApplied = false
                voucherDiscountAmount = 0L
                Toast.makeText(requireContext(), "Đã hủy áp dụng mã giảm giá", Toast.LENGTH_SHORT).show()
                binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EDF3ED")))
            }
            calculatePrices()
        }

        // 5. Points Switch changed
        binding.switchPoints.setOnCheckedChangeListener { _, _ ->
            calculatePrices()
        }

        // 6. Checkout button
        binding.btnCheckout.setOnClickListener {
            performCheckout()
        }
    }

    private fun updateDeliveryUI() {
        binding.tvDeliveryTypeValue.text = deliveryType
        binding.tvRecipientNamePhone.text = "$recipientName - $recipientPhone"
        binding.tvRecipientAddress.text = recipientAddress
    }

    private fun updatePaymentMethodUI() {
        binding.tvPaymentMethod.text = paymentMethod
        when (paymentMethod) {
            "Thanh toán tiền mặt khi nhận hàng" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_cash)
                binding.ivPaymentMethodIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            }
            "Thẻ ATM nội địa/Internet Banking" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_card)
                binding.ivPaymentMethodIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            }
            "Thanh toán trực tuyến MoMo" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_momo)
                binding.ivPaymentMethodIcon.imageTintList = null
            }
            "Thanh toán trực tuyến VNPay" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_vnpay)
                binding.ivPaymentMethodIcon.imageTintList = null
            }
            else -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_cash)
                binding.ivPaymentMethodIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            }
        }
    }

    private fun calculatePrices() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        // Sum up prices
        val originalPriceSum = checkoutItems.sumOf { (it.price * 1.2).toLong() * it.quantity }
        var finalPriceSum = checkoutItems.sumOf { it.price * it.quantity }

        // 1. Direct Discount from standard pricing differences
        var directDiscount = originalPriceSum - finalPriceSum

        // 2. Points discount: 2400đ
        var pointsDiscount = 0L
        if (binding.switchPoints.isChecked && finalPriceSum > 2400) {
            pointsDiscount = 2400L
            finalPriceSum -= pointsDiscount
        }

        // 3. Voucher discount
        if (isVoucherApplied && finalPriceSum > voucherDiscountAmount) {
            finalPriceSum -= voucherDiscountAmount
        }

        val totalSavings = originalPriceSum - finalPriceSum

        // Update Text Views
        binding.tvDetailSubtotal.text = formatter.format(originalPriceSum)
        binding.tvDetailDirectDiscount.text = if (directDiscount + pointsDiscount > 0) {
            "-${formatter.format(directDiscount + pointsDiscount)}"
        } else {
            "0đ"
        }
        binding.tvDetailVoucherDiscount.text = if (voucherDiscountAmount > 0) {
            "-${formatter.format(voucherDiscountAmount)}"
        } else {
            "0đ"
        }

        binding.tvOriginalTotalPrice.text = formatter.format(originalPriceSum)
        binding.tvOriginalTotalPrice.paintFlags = binding.tvOriginalTotalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        binding.tvTotalValue.text = formatter.format(finalPriceSum)
        binding.tvSavingsValue.text = formatter.format(totalSavings)
    }

    private fun showDeliveryTypeBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_delivery_type, null)
        bottomSheetDialog.setContentView(sheetView)

        val ivRadioDelivery = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioDelivery)
        val ivRadioStore = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioStore)
        val clOptionDelivery = sheetView.findViewById<View>(R.id.clOptionDelivery)
        val clOptionStore = sheetView.findViewById<View>(R.id.clOptionStore)

        // Set radio indicator check/uncheck states based on current selection
        if (deliveryType == "Giao hàng tận nơi") {
            ivRadioDelivery.setImageResource(R.drawable.ic_cart_checked)
            ivRadioDelivery.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            ivRadioStore.setImageResource(R.drawable.ic_cart_unchecked)
            ivRadioStore.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#807F7F"))
        } else {
            ivRadioDelivery.setImageResource(R.drawable.ic_cart_unchecked)
            ivRadioDelivery.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#807F7F"))
            ivRadioStore.setImageResource(R.drawable.ic_cart_checked)
            ivRadioStore.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
        }

        clOptionDelivery.setOnClickListener {
            deliveryType = "Giao hàng tận nơi"
            updateDeliveryUI()
            bottomSheetDialog.dismiss()
        }

        clOptionStore.setOnClickListener {
            deliveryType = "Nhận tại cửa hàng"
            updateDeliveryUI()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showAddressSelectionBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_select_address, null)
        bottomSheetDialog.setContentView(sheetView)

        val tvNamePhone1 = sheetView.findViewById<TextView>(R.id.tvNamePhone1)
        val tvAddressDetails1 = sheetView.findViewById<TextView>(R.id.tvAddressDetails1)
        val tvEditAddress1 = sheetView.findViewById<TextView>(R.id.tvEditAddress1)
        val ivRadioAddress1 = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioAddress1)
        val clAddress1 = sheetView.findViewById<View>(R.id.clAddress1)

        val tvNamePhone2 = sheetView.findViewById<TextView>(R.id.tvNamePhone2)
        val tvAddressDetails2 = sheetView.findViewById<TextView>(R.id.tvAddressDetails2)
        val tvEditAddress2 = sheetView.findViewById<TextView>(R.id.tvEditAddress2)
        val ivRadioAddress2 = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioAddress2)
        val clAddress2 = sheetView.findViewById<View>(R.id.clAddress2)

        val btnConfirmAddress = sheetView.findViewById<View>(R.id.btnConfirmAddress)
        val btnAddAddress = sheetView.findViewById<View>(R.id.btnAddAddress)

        var tempSelectedIndex = selectedAddressIndex

        fun updateSheetUI() {
            tvNamePhone1.text = "$address1Name | $address1Phone"
            tvAddressDetails1.text = address1Details

            tvNamePhone2.text = "$address2Name | $address2Phone"
            tvAddressDetails2.text = address2Details

            if (tempSelectedIndex == 1) {
                ivRadioAddress1.setImageResource(R.drawable.ic_cart_checked)
                ivRadioAddress1.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
                ivRadioAddress2.setImageResource(R.drawable.ic_cart_unchecked)
                ivRadioAddress2.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#807F7F"))
            } else {
                ivRadioAddress1.setImageResource(R.drawable.ic_cart_unchecked)
                ivRadioAddress1.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#807F7F"))
                ivRadioAddress2.setImageResource(R.drawable.ic_cart_checked)
                ivRadioAddress2.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            }
        }

        updateSheetUI()

        clAddress1.setOnClickListener {
            tempSelectedIndex = 1
            updateSheetUI()
        }

        clAddress2.setOnClickListener {
            tempSelectedIndex = 2
            updateSheetUI()
        }

        tvEditAddress1.setOnClickListener {
            showEditAddressForm(1) {
                updateSheetUI()
            }
        }

        tvEditAddress2.setOnClickListener {
            showEditAddressForm(2) {
                updateSheetUI()
            }
        }

        btnAddAddress.setOnClickListener {
            showEditAddressForm(2) {
                updateSheetUI()
            }
        }

        btnConfirmAddress.setOnClickListener {
            selectedAddressIndex = tempSelectedIndex
            if (selectedAddressIndex == 1) {
                recipientName = address1Name
                recipientPhone = address1Phone
                recipientAddress = address1Details
            } else {
                recipientName = address2Name
                recipientPhone = address2Phone
                recipientAddress = address2Details
            }
            updateDeliveryUI()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showEditAddressForm(index: Int, onUpdated: () -> Unit) {
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val currentName = if (index == 1) address1Name else address2Name
        val currentPhone = if (index == 1) address1Phone else address2Phone
        val currentAddress = if (index == 1) address1Details else address2Details

        val etName = EditText(requireContext()).apply {
            hint = "Họ tên người nhận"
            setText(currentName)
        }
        val etPhone = EditText(requireContext()).apply {
            hint = "Số điện thoại"
            setText(currentPhone)
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val etAddress = EditText(requireContext()).apply {
            hint = "Địa chỉ chi tiết"
            setText(currentAddress)
        }

        container.addView(etName)
        container.addView(etPhone)
        container.addView(etAddress)

        AlertDialog.Builder(requireContext())
            .setTitle("Sửa địa chỉ")
            .setView(container)
            .setPositiveButton("Cập nhật") { _, _ ->
                val newName = etName.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newAddress = etAddress.text.toString().trim()
                if (newName.isNotEmpty() && newPhone.isNotEmpty() && newAddress.isNotEmpty()) {
                    if (index == 1) {
                        address1Name = newName
                        address1Phone = newPhone
                        address1Details = newAddress
                    } else {
                        address2Name = newName
                        address2Phone = newPhone
                        address2Details = newAddress
                    }
                    onUpdated()
                } else {
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showPaymentMethodBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_bottom_sheet_payment_method, null)
        bottomSheetDialog.setContentView(sheetView)

        val ivRadioCash = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioCash)
        val ivRadioATM = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioATM)
        val ivRadioMoMo = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioMoMo)
        val ivRadioVNPay = sheetView.findViewById<android.widget.ImageView>(R.id.ivRadioVNPay)

        val llOptionCash = sheetView.findViewById<View>(R.id.llOptionCash)
        val llOptionATM = sheetView.findViewById<View>(R.id.llOptionATM)
        val llOptionMoMo = sheetView.findViewById<View>(R.id.llOptionMoMo)
        val llOptionVNPay = sheetView.findViewById<View>(R.id.llOptionVNPay)

        fun updateRadioStates(cash: Boolean, atm: Boolean, momo: Boolean, vnpay: Boolean) {
            ivRadioCash.setImageResource(if (cash) R.drawable.ic_cart_checked else R.drawable.ic_cart_unchecked)
            ivRadioCash.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(if (cash) "#3E4D44" else "#807F7F"))

            ivRadioATM.setImageResource(if (atm) R.drawable.ic_cart_checked else R.drawable.ic_cart_unchecked)
            ivRadioATM.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(if (atm) "#3E4D44" else "#807F7F"))

            ivRadioMoMo.setImageResource(if (momo) R.drawable.ic_cart_checked else R.drawable.ic_cart_unchecked)
            ivRadioMoMo.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(if (momo) "#3E4D44" else "#807F7F"))

            ivRadioVNPay.setImageResource(if (vnpay) R.drawable.ic_cart_checked else R.drawable.ic_cart_unchecked)
            ivRadioVNPay.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(if (vnpay) "#3E4D44" else "#807F7F"))
        }

        // Set initial state
        updateRadioStates(
            cash = paymentMethod == "Thanh toán tiền mặt khi nhận hàng",
            atm = paymentMethod == "Thẻ ATM nội địa/Internet Banking",
            momo = paymentMethod == "Thanh toán trực tuyến MoMo",
            vnpay = paymentMethod == "Thanh toán trực tuyến VNPay"
        )

        llOptionCash.setOnClickListener {
            paymentMethod = "Thanh toán tiền mặt khi nhận hàng"
            updatePaymentMethodUI()
            bottomSheetDialog.dismiss()
        }

        llOptionATM.setOnClickListener {
            paymentMethod = "Thẻ ATM nội địa/Internet Banking"
            updatePaymentMethodUI()
            bottomSheetDialog.dismiss()
        }

        llOptionMoMo.setOnClickListener {
            paymentMethod = "Thanh toán trực tuyến MoMo"
            updatePaymentMethodUI()
            bottomSheetDialog.dismiss()
        }

        llOptionVNPay.setOnClickListener {
            paymentMethod = "Thanh toán trực tuyến VNPay"
            updatePaymentMethodUI()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun performCheckout() {
        val note = binding.etNote.text.toString().trim()
        val hideProductInfo = binding.switchHideProductInfo.isChecked

        lifecycleScope.launch {
            // If checking out from cart, clear these items from the cart database
            checkoutItems.forEach { item ->
                database.cartDao().deleteCartItem(item)
            }

            Toast.makeText(
                requireContext(),
                "Đặt hàng thành công! Cảm ơn bạn đã mua hàng.",
                Toast.LENGTH_LONG
            ).show()

            // Navigate back to the previous screen
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ShopCheckoutFragment"
        const val ARG_CHECKOUT_ITEMS = "checkout_items"

        fun newInstance(items: ArrayList<CartItemEntity>): ShopCheckoutFragment {
            return ShopCheckoutFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CHECKOUT_ITEMS, items)
                }
            }
        }
    }
}
