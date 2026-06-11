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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.ShopFragmentVoucherBinding

class ShopVoucherFragment : RootieFragment() {

    private var _binding: ShopFragmentVoucherBinding? = null
    private val binding get() = _binding!!

    private var selectedVoucherCode: String? = null
    private lateinit var adapter: ShopVoucherAdapter

    // List of mock vouchers
    private val voucherList = listOf(
        VoucherUiModel(
            code = "FIRST50K",
            title = "Giảm 50k cho lần đầu tiên mua hàng",
            minSpendText = "Đơn tối thiểu: 70.000đ",
            expiryText = "Hết hạn trong: 2 ngày",
            discountAmount = 50000L,
            minSpendAmount = 70000L
        ),
        VoucherUiModel(
            code = "WELCOME20K",
            title = "Giảm 20k cho đơn hàng từ 50.000đ",
            minSpendText = "Đơn tối thiểu: 50.000đ",
            expiryText = "Hết hạn trong: 5 ngày",
            discountAmount = 20000L,
            minSpendAmount = 50000L
        ),
        VoucherUiModel(
            code = "ROOTIE100K",
            title = "Giảm 100k cho đơn hàng từ 300.000đ",
            minSpendText = "Đơn tối thiểu: 300.000đ",
            expiryText = "Hết hạn trong: 10 ngày",
            discountAmount = 100000L,
            minSpendAmount = 300000L
        )
    )

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
