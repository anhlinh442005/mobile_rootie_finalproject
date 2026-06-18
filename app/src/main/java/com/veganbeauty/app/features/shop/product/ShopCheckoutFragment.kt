package com.veganbeauty.app.features.shop.product

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.ProfileSession
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.OrderItem
import com.veganbeauty.app.databinding.ShopFragmentCheckoutBinding
import com.veganbeauty.app.features.shop.store.ShopStoreSelectionFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class ShopCheckoutFragment : RootieFragment() {

    private var _binding: ShopFragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var checkoutItems: ArrayList<CartItemEntity> = arrayListOf()
    private lateinit var adapter: ShopCheckoutProductAdapter
    private lateinit var database: RootieDatabase

    // Whether the current buyer is a logged-in member or a guest.
    // Set in onCreate from ProfileSession.isLoggedIn and re-checked in
    // setupUI to keep the form visibility in sync with the session.
    private var isLoggedIn: Boolean = false

    // Store the last validated buyer info for guest checkout (used when payment dialog confirms)
    private var lastGuestBuyerInfo: BuyerInfo? = null

    // Order properties
    private var deliveryType = "Giao hàng tận nơi"
    
    // Address list variables
    private data class CheckoutAddress(
        var name: String,
        var phone: String,
        var details: String,
        var isDefault: Boolean
    )
    private var selectedAddressIndex = 0
    private var memberAddresses = ArrayList<CheckoutAddress>()

    private fun loadSavedAddresses(): ArrayList<CheckoutAddress> {
        val prefs = requireContext().getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("saved_addresses_list_json", null)
        val list = ArrayList<CheckoutAddress>()
        
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(CheckoutAddress(
                        name = obj.getString("name"),
                        phone = obj.getString("phone"),
                        details = obj.getString("address"),
                        isDefault = obj.optBoolean("isDefault", false)
                    ))
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        
        if (list.isEmpty()) {
            val homeName = prefs.getString("addr_home_name", "Ánh Linh") ?: "Ánh Linh"
            val homePhone = prefs.getString("addr_home_phone", "0999 999 999") ?: "0999 999 999"
            val homeAddr = prefs.getString("addr_home_addr", "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "123 Đường Bến Nghé, Phường Bến Nghé, TP.Hồ Chí Minh"
            val defaultType = prefs.getString("addr_default_type", "HOME") ?: "HOME"
            
            list.add(CheckoutAddress(
                name = homeName,
                phone = homePhone,
                details = homeAddr,
                isDefault = defaultType == "HOME"
            ))

            val officeName = prefs.getString("addr_office_name", "Khánh Xuân") ?: "Khánh Xuân"
            val officePhone = prefs.getString("addr_office_phone", "0868 888 888") ?: "0868 888 888"
            val officeAddr = prefs.getString("addr_office_addr", "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh") ?: "Bitexco Financial Tower, 2 Hải Triều, Phường Bến Nghé, TP.Hồ Chí Minh"
            
            list.add(CheckoutAddress(
                name = officeName,
                phone = officePhone,
                details = officeAddr,
                isDefault = defaultType == "OFFICE"
            ))
            saveAddressesList(list)
        }
        return list
    }

    private fun saveAddressesList(list: List<CheckoutAddress>) {
        val prefs = requireContext().getSharedPreferences("rootie_profile_prefs", android.content.Context.MODE_PRIVATE)
        try {
            val array = org.json.JSONArray()
            list.forEach { addr ->
                val obj = org.json.JSONObject().apply {
                    put("name", addr.name)
                    put("phone", addr.phone)
                    put("address", addr.details)
                    put("isDefault", addr.isDefault)
                }
                array.put(obj)
            }
            prefs.edit().putString("saved_addresses_list_json", array.toString()).apply()
            
            val defaultAddr = list.firstOrNull { it.isDefault } ?: list.firstOrNull()
            if (defaultAddr != null) {
                ProfileSession.setAddress(requireContext(), defaultAddr.details)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    // Store list variables
    private var selectedStoreId = "CH001"
    private var selectedStoreName = "Cửa hàng mỹ phẩm Rootie - Cơ sở 1"
    private var selectedStoreAddress = "235 Nguyễn Thị Minh Khai, P. Nguyễn Cư Trinh, Q.1, TP.HCM"

    private var recipientName = "Bảo Nguyên"
    private var recipientPhone = "0123456789"
    private var recipientAddress = "123 Lê Lợi, phường 5, quận 1, Hồ Chí Minh"
    private var paymentMethod = "Thanh toán tiền mặt khi nhận hàng"
    private var isVoucherApplied = false
    private var voucherDiscountAmount = 0L
    private var selectedVoucherCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        isLoggedIn = ProfileSession.isLoggedIn(requireContext())
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            checkoutItems = it.getSerializable(ARG_CHECKOUT_ITEMS) as? ArrayList<CartItemEntity> ?: arrayListOf()
            selectedVoucherCode = it.getString(ARG_INITIAL_VOUCHER_CODE)
            voucherDiscountAmount = it.getLong(ARG_INITIAL_VOUCHER_DISCOUNT, 0L)
            isVoucherApplied = !selectedVoucherCode.isNullOrEmpty()
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
        applyCheckoutMode()
        if (isLoggedIn) {
            syncMemberAddressIntoSelectionSheet()
        } else {
            updateDeliveryUI()
        }
        updatePaymentMethodUI()

        // Initial voucher UI state
        if (isVoucherApplied) {
            val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
            binding.tvVoucherDesc.text = "Đã áp dụng mã: $selectedVoucherCode (-${formatter.format(voucherDiscountAmount)})"
            binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5F2FF")))
        } else {
            binding.tvVoucherDesc.text = "Áp dụng ưu đãi để được giảm giá"
            binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#EDF3ED")))
        }

        calculatePrices()
    }

    /**
     * Wires up the checkout screen for either the logged-in or guest path:
     *  - Logged in: hide the guest form, pre-fill name/phone/address from
     *    ProfileSession, keep the address-change row.
     *  - Guest: show the guest form with only phone/email (no address required),
     *    hide the saved-address card. Both member and guest can see/change store
     *    when "Nhận tại cửa hàng" is selected.
     */
    private fun applyCheckoutMode() {
        if (isLoggedIn) {
            // Member checkout: hide guest form and guest store pickup, show member address
            binding.llGuestForm.visibility = View.GONE
            binding.llGuestStorePickup.visibility = View.GONE
            binding.llMemberAddress.visibility = View.VISIBLE
            // Show change address/store button for members
            binding.btnChangeAddress.visibility = View.VISIBLE
            // Pre-fill from ProfileSession so the user does not have to type
            // anything except maybe a different shipping address.
            recipientName = ProfileSession.getFullName(requireContext()).ifBlank { recipientName }
            recipientPhone = ProfileSession.getPhone(requireContext()).ifBlank { recipientPhone }
            val savedAddress = ProfileSession.getAddress(requireContext())
            if (savedAddress.isNotBlank()) {
                recipientAddress = savedAddress
            }
        } else {
            // Guest checkout: form is always visible, but hide address for store pickup
            binding.llGuestForm.visibility = View.VISIBLE
            if (deliveryType == "Nhận tại cửa hàng") {
                binding.tvGuestAddressLabel.visibility = View.GONE
                binding.etGuestAddress.visibility = View.GONE
            } else {
                binding.tvGuestAddressLabel.visibility = View.VISIBLE
                binding.etGuestAddress.visibility = View.VISIBLE
            }
            binding.llMemberAddress.visibility = View.GONE
            binding.btnChangeAddress.visibility = View.GONE
            // Show/hide store pickup card based on delivery type
            updateGuestStorePickupUI()
            // Show/hide store pickup notice based on delivery type
            updateGuestStoreNotice()
        }
    }

    /**
     * Populate the two-row address selection sheet with the active
     * member's saved profile data. The primary row mirrors what the
     * member has stored in [ProfileSession] and the secondary row is
     * the same address marked as the "alternate" pick so the user
     * still gets a choice when opening the sheet.
     *
     * Previously both rows were hard-coded to "Bảo Nguyên"/"Nguyên
     * Bảo" mock data, which is what caused the "Bảo Nguyên" leak when
     * the test user opened the "Thay đổi" sheet.
     */
    private fun syncMemberAddressIntoSelectionSheet() {
        memberAddresses = loadSavedAddresses()
        val defaultIdx = memberAddresses.indexOfFirst { it.isDefault }.coerceAtLeast(0)
        selectedAddressIndex = defaultIdx
        
        val active = memberAddresses.getOrNull(selectedAddressIndex)
        if (active != null) {
            recipientName = active.name
            recipientPhone = active.phone
            recipientAddress = active.details
        }
        updateDeliveryUI()
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

        // 2. Address Change (for logged-in members)
        binding.btnChangeAddress.setOnClickListener {
            if (deliveryType == "Nhận tại cửa hàng") {
                val storeFragment = ShopStoreSelectionFragment.newInstance(
                    isSelectionMode = true,
                    selectedStoreId = selectedStoreId
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, storeFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                showAddressSelectionBottomSheet()
            }
        }

        // 2b. Change Store (for guest checkout when store pickup is selected)
        binding.btnChangeStore.setOnClickListener {
            val storeFragment = ShopStoreSelectionFragment.newInstance(
                isSelectionMode = true,
                selectedStoreId = selectedStoreId
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, storeFragment)
                .addToBackStack(null)
                .commit()
        }

        // Store Result Listener
        parentFragmentManager.setFragmentResultListener(
            ShopStoreSelectionFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val storeId = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_ID)
            val storeName = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_NAME)
            val storeAddress = bundle.getString(ShopStoreSelectionFragment.RESULT_STORE_ADDRESS)
            if (storeId != null && storeName != null && storeAddress != null) {
                selectedStoreId = storeId
                selectedStoreName = storeName
                selectedStoreAddress = storeAddress
                updateDeliveryUI()
                // Also update guest store pickup UI if visible
                if (!isLoggedIn && deliveryType == "Nhận tại cửa hàng") {
                    updateGuestStorePickupUI()
                }
            }
        }

        // 3. Payment Method Change
        binding.btnChangePaymentMethod.setOnClickListener {
            showPaymentMethodBottomSheet()
        }

        // 4. Voucher Row click
        binding.llVoucherRow.setOnClickListener {
            val voucherFragment = ShopVoucherFragment.newInstance(selectedVoucherCode)
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, voucherFragment)
                .addToBackStack(null)
                .commit()
        }

        // Voucher Result Listener
        parentFragmentManager.setFragmentResultListener(
            ShopVoucherFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val code = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE)
            val discount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L)
            
            selectedVoucherCode = code
            voucherDiscountAmount = discount
            isVoucherApplied = !code.isNullOrEmpty()

            if (isVoucherApplied) {
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                binding.tvVoucherDesc.text = "Đã áp dụng mã: $code (-${formatter.format(discount)})"
                binding.llVoucherRow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E5F2FF")))
            } else {
                binding.tvVoucherDesc.text = "Áp dụng ưu đãi để được giảm giá"
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
        if (deliveryType == "Nhận tại cửa hàng") {
            binding.tvAddressTitle.text = "Nhận hàng tại"
            binding.tvDefaultBadge.visibility = View.GONE
            binding.tvRecipientNamePhone.text = selectedStoreName
            binding.tvRecipientAddress.text = selectedStoreAddress
        } else {
            binding.tvAddressTitle.text = "Giao hàng tới"
            if (isLoggedIn) {
                val active = memberAddresses.getOrNull(selectedAddressIndex)
                binding.tvDefaultBadge.visibility = if (active?.isDefault == true) View.VISIBLE else View.GONE
                binding.tvRecipientNamePhone.text = if (active != null) "${active.name} - ${active.phone}" else "$recipientName - $recipientPhone"
                binding.tvRecipientAddress.text = active?.details ?: recipientAddress
            } else {
                binding.tvDefaultBadge.visibility = View.GONE
                binding.tvRecipientNamePhone.text = "$recipientName - $recipientPhone"
                binding.tvRecipientAddress.text = recipientAddress
            }
        }
        // Re-apply checkout mode to update UI based on delivery type
        applyCheckoutMode()
        // Update guest notice visibility
        updateGuestStoreNotice()
    }

    /**
     * Show/hide the store pickup notice for guest checkout
     */
    private fun updateGuestStoreNotice() {
        if (!isLoggedIn && deliveryType == "Nhận tại cửa hàng") {
            binding.tvGuestStorePickupNotice.visibility = View.VISIBLE
            binding.tvGuestStorePickupNotice.text = "Bạn đã chọn nhận hàng tại cửa hàng. Vui lòng chọn cửa hàng bên dưới."
        } else {
            binding.tvGuestStorePickupNotice.visibility = View.GONE
        }
    }

    /**
     * Show/hide the guest store pickup card and update its content
     */
    private fun updateGuestStorePickupUI() {
        if (!isLoggedIn && deliveryType == "Nhận tại cửa hàng") {
            binding.llGuestStorePickup.visibility = View.VISIBLE
            binding.tvGuestStoreName.text = selectedStoreName
            binding.tvGuestStoreAddress.text = selectedStoreAddress
        } else {
            binding.llGuestStorePickup.visibility = View.GONE
        }
    }

    private fun updatePaymentMethodUI() {
        binding.tvPaymentMethod.text = paymentMethod
        when (paymentMethod) {
            "Thanh toán tiền mặt khi nhận hàng" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_cash)
                binding.ivPaymentMethodIcon.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
            }

            "Thẻ ATM nội địa/Internet Banking" -> {
                binding.ivPaymentMethodIcon.setImageResource(R.drawable.ic_credit_card)
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

        val llAddressContainer = sheetView.findViewById<LinearLayout>(R.id.llAddressContainer)
        val btnConfirmAddress = sheetView.findViewById<View>(R.id.btnConfirmAddress)
        val btnAddAddress = sheetView.findViewById<View>(R.id.btnAddAddress)

        var tempSelectedIndex = selectedAddressIndex

        fun populateAddressViews() {
            llAddressContainer.removeAllViews()
            memberAddresses.forEachIndexed { index, addr ->
                val itemView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_item_select_address, llAddressContainer, false)
                
                val tvNamePhone = itemView.findViewById<TextView>(R.id.tvNamePhone)
                val tvAddressDetails = itemView.findViewById<TextView>(R.id.tvAddressDetails)
                val tvTagDefault = itemView.findViewById<TextView>(R.id.tvTagDefault)
                val ivRadioAddress = itemView.findViewById<ImageView>(R.id.ivRadioAddress)
                val tvEditAddress = itemView.findViewById<TextView>(R.id.tvEditAddress)

                tvNamePhone.text = "${addr.name} | ${addr.phone}"
                tvAddressDetails.text = addr.details
                tvTagDefault.visibility = if (addr.isDefault) View.VISIBLE else View.GONE

                if (index == tempSelectedIndex) {
                    ivRadioAddress.setImageResource(R.drawable.ic_cart_checked)
                    ivRadioAddress.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3E4D44"))
                } else {
                    ivRadioAddress.setImageResource(R.drawable.ic_cart_unchecked)
                    ivRadioAddress.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#807F7F"))
                }

                itemView.setOnClickListener {
                    tempSelectedIndex = index
                    populateAddressViews()
                }

                tvEditAddress.setOnClickListener {
                    showEditAddressForm(index) {
                        populateAddressViews()
                    }
                }

                llAddressContainer.addView(itemView)

                if (index < memberAddresses.size - 1) {
                    val divider = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            1
                        )
                        setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                    }
                    llAddressContainer.addView(divider)
                }
            }
        }

        populateAddressViews()

        btnAddAddress.setOnClickListener {
            showAddAddressForm {
                populateAddressViews()
            }
        }

        btnConfirmAddress.setOnClickListener {
            selectedAddressIndex = tempSelectedIndex
            val active = memberAddresses.getOrNull(selectedAddressIndex)
            if (active != null) {
                recipientName = active.name
                recipientPhone = active.phone
                recipientAddress = active.details
            }
            updateDeliveryUI()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun showEditAddressForm(index: Int, onUpdated: () -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_dialog_edit_address, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_dialog_phone)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_dialog_address)
        val switchDefault = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_dialog_default)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<View>(R.id.btn_dialog_save)

        val addrItem = memberAddresses.getOrNull(index) ?: return

        tvTitle.text = "Chỉnh sửa địa chỉ"
        etName.setText(addrItem.name)
        etPhone.setText(addrItem.phone)
        etAddress.setText(addrItem.details)
        switchDefault.isChecked = addrItem.isDefault

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val addr = etAddress.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty() && addr.isNotEmpty()) {
                addrItem.name = name
                addrItem.phone = phone
                addrItem.details = addr
                
                if (switchDefault.isChecked) {
                    memberAddresses.forEachIndexed { i, a ->
                        a.isDefault = (i == index)
                    }
                } else if (addrItem.isDefault) {
                    addrItem.isDefault = false
                }

                saveAddressesList(memberAddresses)
                onUpdated()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showAddAddressForm(onUpdated: () -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.shop_dialog_edit_address, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val etName = dialogView.findViewById<EditText>(R.id.et_dialog_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_dialog_phone)
        val etAddress = dialogView.findViewById<EditText>(R.id.et_dialog_address)
        val switchDefault = dialogView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.switch_dialog_default)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<View>(R.id.btn_dialog_save)

        tvTitle.text = "Thêm địa chỉ mới"
        etName.setText("")
        etPhone.setText("")
        etAddress.setText("")
        switchDefault.isChecked = false

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val addr = etAddress.text.toString().trim()
            if (name.isNotEmpty() && phone.isNotEmpty() && addr.isNotEmpty()) {
                val newAddr = CheckoutAddress(
                    name = name,
                    phone = phone,
                    details = addr,
                    isDefault = switchDefault.isChecked
                )
                
                if (switchDefault.isChecked) {
                    memberAddresses.forEach { it.isDefault = false }
                }
                
                memberAddresses.add(newAddr)
                saveAddressesList(memberAddresses)
                onUpdated()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
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
            ivRadioCash.setImageResource(if (cash) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
            ivRadioCash.imageTintList = if (cash) null else android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BDBDBD"))

            ivRadioATM.setImageResource(if (atm) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
            ivRadioATM.imageTintList = if (atm) null else android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BDBDBD"))

            ivRadioMoMo.setImageResource(if (momo) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
            ivRadioMoMo.imageTintList = if (momo) null else android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BDBDBD"))

            ivRadioVNPay.setImageResource(if (vnpay) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
            ivRadioVNPay.imageTintList = if (vnpay) null else android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#BDBDBD"))
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
        // Note + hide-product-info flags are reserved for the future order API;
        // for now we only consume the payment method to choose a flow.
        @Suppress("UNUSED_VARIABLE")
        val note = binding.etNote.text.toString().trim()
        @Suppress("UNUSED_VARIABLE")
        val hideProductInfo = binding.switchHideProductInfo.isChecked

        // Validate buyer info up front so the OTP step only runs when the
        // form is in a valid state.
        val buyerInfo = collectAndValidateBuyerInfo() ?: return

        // Recalculate the final total so we can pass it to the success screen.
        val finalTotal = calculateFinalTotal()

        // Build a quick mock order code based on current timestamp.
        val mockOrderCode = "RDH" + java.text.SimpleDateFormat(
            "ddMMyyyyHHmmss",
            java.util.Locale("vi", "VN")
        ).format(java.util.Date())

        // Dispatch the right checkout branch. Order persistence happens
        // inside [navigateToOrderSuccess] so we can guarantee the row
        // is in Room by the time the user navigates to the tracking
        // screen.
        when {
            isLoggedIn -> handleMemberCheckout(mockOrderCode, finalTotal)
            else -> handleGuestCheckout(mockOrderCode, finalTotal, buyerInfo)
        }
    }

    /**
     * Validates the buyer information, returning a populated [BuyerInfo] on
     * success or `null` if validation failed (in which case the user has
     * already been shown a toast explaining what's wrong).
     *
     * Guest checkout only requires:
     * - Phone number (required)
     * - Email (optional)
     * - No address needed (removed from guest form)
     * - Store must be selected when "Nhận tại cửa hàng" is chosen
     */
    private fun collectAndValidateBuyerInfo(): BuyerInfo? {
        if (isLoggedIn) {
            // Members do not need to type anything new — we just use the
            // values populated from the saved address list / ProfileSession.
            return BuyerInfo(
                name = recipientName,
                phone = recipientPhone,
                email = ProfileSession.getEmail(requireContext()).ifBlank { null },
                address = recipientAddress
            )
        } else {
            // Guest checkout
            val guestName = binding.etGuestName.text.toString().trim()
            val guestPhone = binding.etGuestPhone.text.toString().trim()
            val guestEmail = binding.etGuestEmail.text.toString().trim()

            // Validation: name is required
            if (guestName.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Vui lòng nhập họ và tên người nhận.", Toast.LENGTH_SHORT).show()
                return null
            }

            // Validation: phone is required
            if (guestPhone.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Vui lòng nhập số điện thoại.", Toast.LENGTH_SHORT).show()
                return null
            }
            if (guestPhone.length < 9) {
                Toast.makeText(requireContext(),
                    "Số điện thoại không hợp lệ.", Toast.LENGTH_SHORT).show()
                return null
            }

            // Validation: email is optional but must be valid if provided
            if (guestEmail.isNotEmpty() &&
                !android.util.Patterns.EMAIL_ADDRESS.matcher(guestEmail).matches()
            ) {
                Toast.makeText(requireContext(),
                    "Email không đúng định dạng.", Toast.LENGTH_SHORT).show()
                return null
            }

            if (deliveryType == "Nhận tại cửa hàng") {
                // For store pickup, must select a store first
                if (selectedStoreId.isBlank() || selectedStoreName == "Chưa chọn cửa hàng") {
                    Toast.makeText(requireContext(),
                        "Vui lòng chọn cửa hàng để nhận hàng.", Toast.LENGTH_SHORT).show()
                    return null
                }
                recipientName = guestName
                recipientPhone = guestPhone
                recipientAddress = selectedStoreAddress
                return BuyerInfo(
                    name = guestName,
                    phone = guestPhone,
                    email = guestEmail.ifBlank { null },
                    address = selectedStoreAddress
                )
            } else {
                // For home delivery, guest must also fill in address
                val guestAddress = binding.etGuestAddress.text.toString().trim()

                // Validation: address is required
                if (guestAddress.isEmpty()) {
                    Toast.makeText(requireContext(),
                        "Vui lòng nhập địa chỉ giao hàng.", Toast.LENGTH_SHORT).show()
                    return null
                }

                recipientName = guestName
                recipientPhone = guestPhone
                recipientAddress = guestAddress
                return BuyerInfo(
                    name = guestName,
                    phone = guestPhone,
                    email = guestEmail.ifBlank { null },
                    address = guestAddress
                )
            }
        }
    }

    /**
     * Member checkout: same behaviour as before the hybrid rollout — COD
     * goes straight to the success screen, the rest show their respective
     * payment dialogs.
     */
    private fun handleMemberCheckout(mockOrderCode: String, finalTotal: Long) {
        // 1) Clear items from the cart database so the next visit starts clean.
        lifecycleScope.launch {
            checkoutItems.forEach { item -> database.cartDao().deleteCartItem(item) }
        }
        when (paymentMethod) {
            "Thanh toán tiền mặt khi nhận hàng" ->
                navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, isGuest = false)
            "Thẻ ATM nội địa/Internet Banking" ->
                ShopPaymentDialogs.showAtmVnpayQrDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal,
                    isVnpay = false
                )
            "Thanh toán trực tuyến VNPay" ->
                ShopPaymentDialogs.showAtmVnpayQrDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal,
                    isVnpay = true
                )
            "Thanh toán trực tuyến MoMo" ->
                ShopPaymentDialogs.showMomoRedirectDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal
                )
            else -> navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, isGuest = false)
        }
    }


    fun showSuccessBannerAndNavigate(
        orderCode: String,
        isEmailNotification: Boolean,
        onFinished: () -> Unit
    ) {
        val b = _binding ?: run {
            onFinished()
            return
        }
        val greeting = if (!recipientName.isNullOrBlank()) "$recipientName ơi, " else ""
        val storePickup = isStorePickup()
        
        val titleText: String
        val messageText: String

        if (isEmailNotification) {
            if (storePickup) {
                titleText = "Xác nhận nhận tại cửa hàng qua Email"
                messageText = "${greeting}chúng tôi đã nhận được đơn hàng #$orderCode. Xác nhận nhận tại cửa hàng đã được gửi qua email của bạn."
            } else {
                titleText = "Xác nhận đặt hàng qua Email"
                messageText = "${greeting}chúng tôi đã nhận được đơn hàng #$orderCode. Xác nhận đã được gửi qua email của bạn."
            }
        } else {
            if (storePickup) {
                titleText = "Xác nhận nhận tại cửa hàng qua SMS"
                messageText = "${greeting}cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #$orderCode. Vui lòng đến cửa hàng đã chọn để nhận hàng."
            } else {
                titleText = "Xác nhận đặt hàng qua SMS"
                messageText = "${greeting}cảm ơn bạn đã đặt hàng tại Rootie. Mã đơn hàng: #$orderCode. Đơn hàng đang được chuẩn bị."
            }
        }

        b.tvNotiTitle.text = titleText
        b.tvNotiMessage.text = messageText

        b.cvNotificationBanner.visibility = View.VISIBLE
        b.cvNotificationBanner.translationY = -300f

        b.cvNotificationBanner.animate()
            .translationY(0f)
            .setDuration(1200) // Slow slide-in
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    _binding?.let { binding ->
                        binding.cvNotificationBanner.animate()
                            .translationY(-300f)
                            .setDuration(800)
                            .withEndAction {
                                binding.cvNotificationBanner.visibility = View.GONE
                                onFinished()
                            }
                            .start()
                    } ?: onFinished()
                }, 2500)
            }
            .start()
    }

    /**
     * Guest checkout: after order is placed, show mock push notification preview.
     * If guest provided email -> show as email notification
     * If no email provided -> show as SMS notification
     *
     * The local guest cart is cleared on success so a fresh visit starts empty.
     */
    private fun handleGuestCheckout(
        mockOrderCode: String,
        finalTotal: Long,
        buyerInfo: BuyerInfo
    ) {
        // Store buyer info for use when payment dialog confirms
        lastGuestBuyerInfo = buyerInfo

        // Save guest phone for order tracking (guests can track orders without login)
        ProfileSession.setGuestPhone(requireContext(), buyerInfo.phone)

        val hasEmail = !buyerInfo.email.isNullOrBlank()

        when (paymentMethod) {
            "Thanh toán tiền mặt khi nhận hàng" ->
                navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, isGuest = true, isEmailNotification = hasEmail)
            "Thẻ ATM nội địa/Internet Banking" ->
                ShopPaymentDialogs.showAtmVnpayQrDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal,
                    isVnpay = false
                )
            "Thanh toán trực tuyến VNPay" ->
                ShopPaymentDialogs.showAtmVnpayQrDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal,
                    isVnpay = true
                )
            "Thanh toán trực tuyến MoMo" ->
                ShopPaymentDialogs.showMomoRedirectDialog(
                    fragment = this,
                    orderCode = mockOrderCode,
                    totalAmount = finalTotal
                )
            else -> navigateToOrderSuccess(mockOrderCode, finalTotal, paymentMethod, isGuest = true, isEmailNotification = hasEmail)
        }
    }

    private fun navigateToOrderSuccess(
        orderCode: String,
        totalAmount: Long,
        method: String,
        isGuest: Boolean,
        isEmailNotification: Boolean = false
    ) {
        // Build the [OrderEntity] snapshot from the current checkout
        // state and persist it before navigating. Persisting here (and
        // not as a fire-and-forget) guarantees the row is in Room by
        // the time the user opens the order-tracking screen.
        val persistedOrder = buildOrderEntitySnapshot(orderCode, totalAmount, method, isGuest)
        persistOrderSync(persistedOrder)

        val notificationType = if (isEmailNotification) "email" else "sms"

        // For logged-in users, navigate directly to success screen
        // For guests, show the mock push-notification preview based on email/SMS
        if (isGuest) {
            PushNotiDialog.show(
                context = requireContext(),
                orderCode = orderCode,
                recipientName = recipientName,
                isEmailNotification = isEmailNotification,
                onContinue = {
                    // Navigate to the success screen.
                    commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
                },
                onDismiss = {
                    // User dismissed the push preview — still navigate so
                    // the order is visible from the success screen; the
                    // order is already persisted.
                    commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
                },
                fragment = this
            )
        } else {
            // Member: navigate directly to success screen
            commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
        }
    }

    /**
     * Build a fully-populated [OrderEntity] from the current checkout
     * state. Used by both [navigateToOrderSuccess] (member COD) and
     * by the payment dialogs so the row written to Room is the same
     * regardless of which payment branch the user took.
     */
    private fun buildOrderEntitySnapshot(
        orderCode: String,
        totalAmount: Long,
        method: String,
        isGuest: Boolean
    ): OrderEntity {
        val finalUserId = if (isLoggedIn) ProfileSession.getUserId(requireContext()) else null
        val orderItems = checkoutItems.map { ci ->
            OrderItem(
                productId = ci.id,
                productName = ci.name,
                productImage = ci.image,
                quantity = ci.quantity,
                price = ci.price
            )
        }
        val now = System.currentTimeMillis()
        val nowDate = java.text.SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(java.util.Date(now))
        val nowTime = java.text.SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(java.util.Date(now))
        return OrderEntity(
            id = orderCode,
            orderDate = nowDate,
            orderTime = nowTime,
            status = "Chờ xử lý",
            totalAmount = totalAmount,
            subTotal = checkoutItems.sumOf { it.price * it.quantity },
            items = orderItems,
            userId = finalUserId,
            isGuest = isGuest,
            shippingName = recipientName,
            shippingPhone = recipientPhone,
            shippingAddress = recipientAddress,
            shippingCost = 0L,
            voucherDiscount = voucherDiscountAmount,
            paymentMethod = method,
            expectedDeliveryTime = null,
            hasReview = false,
            reviewStars = 0,
            reviewText = null,
            reviewImage = null,
            isAnonymous = false,
            recommendToFriends = false,
            billingName = recipientName,
            billingPhone = recipientPhone,
            billingEmail = if (isGuest) binding.etGuestEmail.text.toString().trim().ifBlank { null } else ProfileSession.getEmail(requireContext()).ifBlank { null }
        )
    }

    /**
     * Persist an [order] to Room. Suspended on the IO dispatcher so
     * the caller can call it from a [lifecycleScope.launch] without
     * blocking the UI thread. Failures are logged but never thrown —
     * a missing row is preferable to crashing the checkout flow.
     */
    private fun persistOrderSync(order: OrderEntity) {
        lifecycleScope.launch {
            try {
                database.orderDao().insertOrder(order)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "persistOrderSync failed: ${e.message}", e)
            }
        }
        
        // --- ADD FIREBASE SYNC FOR ADMIN ---
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            // 1. Push order to Firebase
            val orderMap = com.google.gson.Gson().fromJson(
                com.google.gson.Gson().toJsonTree(order),
                Map::class.java
            ) as Map<String, Any>
            db.collection("orders").document(order.id).set(orderMap)
            
            // 2. Push notification for Admin
            val newNotification = hashMapOf(
                "title" to "Có đơn hàng mới! \uD83D\uDED2",
                "message" to "Khách hàng ${order.shippingName} vừa đặt đơn hàng trị giá ${String.format("%,d", order.totalAmount)}đ.",
                "type" to "NEW_ORDER",
                "isRead" to false,
                "createdAt" to System.currentTimeMillis(),
                "orderId" to order.id
            )
            db.collection("notification_admin").add(newNotification)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Firebase sync failed: ${e.message}", e)
        }
    }

    /**
     * Public hook for [ShopPaymentDialogs] (or any other flow that
     * has the dialogs on top of the checkout screen) so the order is
     * persisted before the success screen is pushed.
     */
    fun persistOrderFromDialog(
        orderCode: String,
        totalAmount: Long,
        method: String,
        isGuest: Boolean,
        isEmailNotification: Boolean = false
    ) {
        val snapshot = buildOrderEntitySnapshot(orderCode, totalAmount, method, isGuest)
        persistOrderSync(snapshot)

        val notificationType = if (isEmailNotification) "email" else "sms"

        // If guest, also show the appropriate notification (email vs SMS)
        if (isGuest) {
            PushNotiDialog.show(
                context = requireContext(),
                orderCode = orderCode,
                recipientName = recipientName,
                isEmailNotification = isEmailNotification,
                onContinue = {
                    commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
                },
                onDismiss = {
                    commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
                },
                fragment = this
            )
        } else {
            commitSuccessNavigation(orderCode, totalAmount, method, isGuest, notificationType)
        }
    }

    /**
     * Check if guest has provided email for notification purposes.
     * Used by [ShopPaymentDialogs] to determine notification type.
     */
    fun hasGuestEmail(): Boolean {
        if (!isLoggedIn) {
            val email = binding.etGuestEmail.text.toString().trim()
            return email.isNotEmpty()
        }
        return false
    }

    /**
     * Perform the actual fragment swap to the order-success screen.
     *
     * The previous implementation called [parentFragmentManager.popBackStack]
     * and then immediately committed a new transaction, which on some
     * devices caused the checkout screen to be popped off the back stack
     * *after* the new success transaction was queued, so the success
     * fragment ended up briefly attached to a half-detached view and the
     * process was killed. We now defer the pop until after the commit so
     * the FragmentManager processes them in the correct order.
     */
    private fun commitSuccessNavigation(
        orderCode: String,
        totalAmount: Long,
        method: String,
        isGuest: Boolean,
        notificationType: String = "sms"
    ) {
        val successFragment = ShopOrderSuccessFragment.newInstance(
            orderCode = orderCode,
            totalAmount = totalAmount,
            paymentMethod = method,
            isGuest = isGuest,
            notificationType = notificationType,
            isStorePickup = (deliveryType == "Nhận tại cửa hàng"),
            storeName = selectedStoreName,
            storeAddress = selectedStoreAddress,
            recipientName = recipientName
        )
        try {
            parentFragmentManager.popBackStack()
        } catch (_: Exception) {}
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, successFragment)
            .commit()
    }

    fun isStorePickup() = deliveryType == "Nhận tại cửa hàng"
    fun getSelectedStoreName() = selectedStoreName
    fun getSelectedStoreAddress() = selectedStoreAddress


    /**
     * Snapshot of the buyer info collected from the checkout form. For
     * members the address fields come from the saved address list; for
     * guests they come from the inline form.
     */
    private data class BuyerInfo(
        val name: String,
        val phone: String,
        val email: String?,
        val address: String
    )

    private fun calculateFinalTotal(): Long {
        var finalPriceSum = checkoutItems.sumOf { it.price * it.quantity }

        val pointsDiscount = if (binding.switchPoints.isChecked && finalPriceSum > 2400) 2400L else 0L
        finalPriceSum -= pointsDiscount

        if (isVoucherApplied && finalPriceSum > voucherDiscountAmount) {
            finalPriceSum -= voucherDiscountAmount
        }
        return finalPriceSum
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ShopCheckoutFragment"
        const val ARG_CHECKOUT_ITEMS = "checkout_items"
        const val ARG_INITIAL_VOUCHER_CODE = "initial_voucher_code"
        const val ARG_INITIAL_VOUCHER_DISCOUNT = "initial_voucher_discount"
        /**
         * Back-stack name used when pushing the order-success screen on
         * top of the checkout screen. Reused by
         * [com.veganbeauty.app.features.shop.product.ShopPaymentDialogs]
         * when navigating from the payment-method dialogs to the
         * success screen, so the same pop-then-commit ordering works
         * from both call sites.
         */
        const val SUCCESS_TAG = "shop_checkout_success"

        fun newInstance(
            items: ArrayList<CartItemEntity>,
            initialVoucherCode: String? = null,
            initialVoucherDiscount: Long = 0L
        ): ShopCheckoutFragment {
            return ShopCheckoutFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_CHECKOUT_ITEMS, items)
                    putString(ARG_INITIAL_VOUCHER_CODE, initialVoucherCode)
                    putLong(ARG_INITIAL_VOUCHER_DISCOUNT, initialVoucherDiscount)
                }
            }
        }
    }
}
