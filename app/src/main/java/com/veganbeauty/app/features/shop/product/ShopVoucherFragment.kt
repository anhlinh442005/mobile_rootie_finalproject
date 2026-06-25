package com.veganbeauty.app.features.shop.product

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.OrderRepository
import com.veganbeauty.app.databinding.ShopFragmentVoucherBinding
import com.veganbeauty.app.features.profile.AccountVoucherFragment
import com.veganbeauty.app.features.profile.VoucherItem
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShopVoucherFragment : RootieFragment() {

    private var _binding: ShopFragmentVoucherBinding? = null
    private val binding get() = _binding!!

    private var selectedVoucherCode: String? = null
    private lateinit var adapter: ShopVoucherAdapter
    private lateinit var repository: OrderRepository

    // List of active vouchers loaded dynamically
    private val voucherList = mutableListOf<VoucherUiModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedVoucherCode = it.getString(ARG_SELECTED_VOUCHER_CODE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentVoucherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupRecyclerView()
        setupListeners()
        loadVouchers()
    }

    private fun setupRecyclerView() {
        adapter = ShopVoucherAdapter(voucherList, selectedVoucherCode) { voucher ->
            selectedVoucherCode = voucher?.code
            binding.etCouponCode.setText(voucher?.code ?: "")
        }
        binding.rvVouchers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVouchers.adapter = adapter

        // Set initial text if a voucher is already selected
        selectedVoucherCode?.let { code ->
            binding.etCouponCode.setText(code)
        }
    }

    private fun setupListeners() {
        // Handle input code enter
        binding.etCouponCode.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyCodeInput()
                true
            } else {
                false
            }
        }

        // Realtime input search matching code
        binding.etCouponCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString().trim().uppercase()
                val matched = voucherList.find { it.code == input }
                if (matched != null && selectedVoucherCode != matched.code) {
                    selectedVoucherCode = matched.code
                    adapter.setSelectedCode(matched.code)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Confirm Selection
        binding.btnSelectVoucher.setOnClickListener {
            val result = Bundle().apply {
                putString(RESULT_VOUCHER_CODE, selectedVoucherCode)
                val selectedVoucher = voucherList.find { it.code == selectedVoucherCode }
                if (selectedVoucher != null) {
                    putLong(RESULT_VOUCHER_DISCOUNT, selectedVoucher.discountAmount)
                    putString(RESULT_VOUCHER_TITLE, selectedVoucher.title)
                } else {
                    putLong(RESULT_VOUCHER_DISCOUNT, 0L)
                    putString(RESULT_VOUCHER_TITLE, "")
                }
            }
            parentFragmentManager.setFragmentResult(REQUEST_KEY, result)
            parentFragmentManager.popBackStack()
        }
    }

    private fun applyCodeInput() {
        val code = binding.etCouponCode.text.toString().trim().uppercase()
        val matched = voucherList.find { it.code == code }
        if (matched != null) {
            selectedVoucherCode = matched.code
            adapter.setSelectedCode(matched.code)
            Toast.makeText(requireContext(), "Đã áp dụng mã: ${matched.code}", Toast.LENGTH_SHORT).show()
        } else if (code.isNotEmpty()) {
            Toast.makeText(requireContext(), "Mã giảm giá không hợp lệ", Toast.LENGTH_SHORT).show()
        }
        
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCouponCode.windowToken, 0)
    }

    private fun loadVouchers() {
        val context = requireContext()
        val db = RootieDatabase.getDatabase(context)
        repository = OrderRepository(
            db.orderDao(),
            db.rewardPointDao(),
            db.userGiftDao(),
            LocalJsonReader(context)
        )

        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllUserGifts().collect { dbGifts ->
                val systemVouchers = loadVouchersFromAssets(context)
                val mappedDbVouchers = dbGifts
                    .filter { it.giftType == "voucher_discount" || it.giftType == "voucher_freeship" }
                    .map { gift ->
                        val statusVal = computeStatusFromExpiry(gift.expiryDate)
                        VoucherItem(
                            id = "db_${gift.id}",
                            title = gift.title,
                            description = gift.description,
                            code = gift.code,
                            status = statusVal,
                            hsd = gift.expiryDate,
                            type = if (gift.giftType == "voucher_freeship") "free ship" else "discount",
                            fromGift = true,
                            quantity = null,
                            minOrderValue = gift.minOrderValue,
                            applicableProducts = gift.applicableProducts,
                            offerType = gift.offerType,
                            discountValue = gift.discountValue
                        )
                    }

                val activeSystem = systemVouchers.filter {
                    it.id !in AccountVoucherFragment.deletedSystemVoucherIdsStatic
                }
                
                val allVouchers = activeSystem + mappedDbVouchers
                // Filter only valid or expiring vouchers
                val activeVouchers = allVouchers.filter { it.status == "valid" || it.status == "expiring" }
                
                voucherList.clear()
                activeVouchers.forEach { item ->
                    val minSpendText = "Đơn tối thiểu: ${formatCurrency(item.minOrderValue)}đ"
                    val expiryText = "Hết hạn: ${formatHsd(item.hsd)}"
                    voucherList.add(
                        VoucherUiModel(
                            code = item.code,
                            title = item.title,
                            minSpendText = minSpendText,
                            expiryText = expiryText,
                            discountAmount = item.discountValue.toLong(),
                            minSpendAmount = item.minOrderValue.toLong()
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                
                // Select first matching selected code if set
                selectedVoucherCode?.let { code ->
                    val matched = voucherList.find { it.code == code }
                    if (matched != null) {
                        adapter.setSelectedCode(code)
                    }
                }
            }
        }
    }

    private fun computeStatusFromExpiry(expiryStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val expiryDate = sdf.parse(expiryStr) ?: return "valid"
            
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val expiry = Calendar.getInstance().apply {
                time = expiryDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            if (expiry.before(today)) {
                "expired"
            } else if (expiry.equals(today)) {
                "expiring"
            } else {
                "valid"
            }
        } catch (e: Exception) {
            "valid"
        }
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

    private fun loadVouchersFromAssets(ctx: Context): List<VoucherItem> {
        return try {
            val jsonString = ctx.assets.open("vouchers.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<VoucherItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val hsdStr = obj.optString("hsd", "")
                val statusVal = computeStatusFromExpiry(hsdStr)
                
                list.add(
                    VoucherItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        code = obj.optString("code", ""),
                        status = statusVal,
                        hsd = hsdStr,
                        type = obj.optString("type", "discount"),
                        fromGift = obj.optBoolean("from-gift", false),
                        quantity = if (obj.has("quantity")) obj.getInt("quantity") else null,
                        minOrderValue = obj.optInt("minOrderValue", 0),
                        applicableProducts = obj.optString("applicableProducts", "Tất cả sản phẩm"),
                        offerType = obj.optString("offerType", "fixed_amount"),
                        discountValue = obj.optInt("discountValue", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ShopVoucherFragment"
        const val ARG_SELECTED_VOUCHER_CODE = "selected_voucher_code"

        // Result Keys
        const val REQUEST_KEY = "voucher_selection_request"
        const val RESULT_VOUCHER_CODE = "result_voucher_code"
        const val RESULT_VOUCHER_DISCOUNT = "result_voucher_discount"
        const val RESULT_VOUCHER_TITLE = "result_voucher_title"

        fun newInstance(selectedCode: String?): ShopVoucherFragment {
            return ShopVoucherFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_VOUCHER_CODE, selectedCode)
                }
            }
        }
    }
}

// UI Model for voucher item
data class VoucherUiModel(
    val code: String,
    val title: String,
    val minSpendText: String,
    val expiryText: String,
    val discountAmount: Long,
    val minSpendAmount: Long
)

// RecyclerView Adapter
class ShopVoucherAdapter(
    private val items: List<VoucherUiModel>,
    initialSelectedCode: String?,
    private val onSelectionChanged: (VoucherUiModel?) -> Unit
) : RecyclerView.Adapter<ShopVoucherAdapter.ViewHolder>() {

    private var selectedCode: String? = initialSelectedCode

    fun setSelectedCode(code: String?) {
        selectedCode = code
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.shop_item_voucher_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, selectedCode == item.code) {
            if (selectedCode == item.code) {
                // Toggle off
                selectedCode = null
                onSelectionChanged(null)
            } else {
                selectedCode = item.code
                onSelectionChanged(item)
            }
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvVoucherTitle)
        private val minSpend = view.findViewById<TextView>(R.id.tvVoucherMinSpend)
        private val expiry = view.findViewById<TextView>(R.id.tvVoucherExpiry)
        private val selectIndicator = view.findViewById<ImageView>(R.id.ivSelectIndicator)

        fun bind(item: VoucherUiModel, isSelected: Boolean, onClick: () -> Unit) {
            title.text = item.title
            minSpend.text = item.minSpendText
            expiry.text = item.expiryText

            if (isSelected) {
                selectIndicator.setImageResource(R.drawable.ic_cart_checked)
                selectIndicator.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#3E4D44")
                )
            } else {
                selectIndicator.setImageResource(R.drawable.ic_cart_unchecked)
                selectIndicator.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#807F7F")
                )
            }

            itemView.setOnClickListener { onClick() }
        }
    }
}
