package com.veganbeauty.app.features.shop.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.databinding.ShopBottomSheetCartBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CartBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: RootieDatabase
    private lateinit var adapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetCartBinding.inflate(inflater, container, false)
        database = RootieDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeCartItems()
    }

    private fun setupRecyclerView() {
        adapter = CartAdapter(
            onQuantityChanged = { item, newQuantity ->
                lifecycleScope.launch {
                    if (newQuantity <= 0) {
                        database.cartDao().deleteCartItem(item)
                        Toast.makeText(requireContext(), "Đã xóa ${item.name} khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
                    } else {
                        database.cartDao().updateCartItem(item.copy(quantity = newQuantity))
                    }
                }
            },
            onSelectionToggled = { item, isSelected ->
                lifecycleScope.launch {
                    database.cartDao().updateCartItem(item.copy(isSelected = isSelected))
                }
            }
        )

        binding.rvCartItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCartItems.adapter = adapter
    }

    private fun setupListeners() {
        // Toggle Select All clicked
        binding.llSelectAll.setOnClickListener {
            val currentList = adapter.currentList
            val allSelected = currentList.all { it.isSelected }
            lifecycleScope.launch {
                currentList.forEach { item ->
                    database.cartDao().updateCartItem(item.copy(isSelected = !allSelected))
                }
            }
        }

        // Points Switch Toggle
        binding.switchPoints.setOnCheckedChangeListener { _, _ ->
            updateUI(adapter.currentList)
        }

        // Voucher Row Click
        binding.llVoucherRow.setOnClickListener {
            Toast.makeText(requireContext(), "Áp dụng mã giảm giá thành công!", Toast.LENGTH_SHORT).show()
        }

        // Toggle Price details click
        binding.llPrices.setOnClickListener {
            val isVisible = binding.llPriceDetails.visibility == View.VISIBLE
            if (isVisible) {
                binding.llPriceDetails.visibility = View.GONE
                binding.ivPriceToggleArrow.rotation = 270f
            } else {
                binding.llPriceDetails.visibility = View.VISIBLE
                binding.ivPriceToggleArrow.rotation = 90f
                
                // Force bottom sheet behavior to expand fully so footer remains visible
                (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.let { bottomSheetDialog ->
                    val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                    if (bottomSheet != null) {
                        val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            }
        }

        // Checkout button
        binding.btnCheckout.setOnClickListener {
            val selectedItems = adapter.currentList.filter { it.isSelected }
            if (selectedItems.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 sản phẩm để thanh toán", Toast.LENGTH_SHORT).show()
            } else {
                val checkoutItems = ArrayList(selectedItems)
                val checkoutFragment = ShopCheckoutFragment.newInstance(checkoutItems)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, checkoutFragment)
                    .addToBackStack(null)
                    .commit()
                dismiss()
            }
        }
    }

    private fun observeCartItems() {
        lifecycleScope.launch {
            database.cartDao().getAllCartItems().collectLatest { items ->
                adapter.submitList(items)
                updateUI(items)
            }
        }
    }

    private fun updateUI(items: List<CartItemEntity>) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        // Calculate count of selected items
        val selectedItems = items.filter { it.isSelected }
        val totalSelectedQty = selectedItems.sumOf { it.quantity }
        
        val originalPriceSum = selectedItems.sumOf { (it.price * 1.2).toLong() * it.quantity }
        var finalPriceSum = selectedItems.sumOf { it.price * it.quantity }

        // Points deduction
        if (binding.switchPoints.isChecked && finalPriceSum > 2400) {
            finalPriceSum -= 2400
        }

        val totalSavings = originalPriceSum - finalPriceSum

        // Update details values
        binding.tvDetailSubtotal.text = formatter.format(originalPriceSum)
        val directDiscount = originalPriceSum - finalPriceSum
        binding.tvDetailDirectDiscount.text = if (directDiscount > 0) {
            "-${formatter.format(directDiscount)}"
        } else {
            "0đ"
        }

        // Update total values
        binding.tvOriginalTotalPrice.text = formatter.format(originalPriceSum)
        binding.tvOriginalTotalPrice.paintFlags = binding.tvOriginalTotalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        
        binding.tvTotalValue.text = formatter.format(finalPriceSum)
        binding.tvSavingsValue.text = formatter.format(totalSavings)

        binding.btnCheckout.text = "Mua hàng"

        // Update select all text and icon state
        binding.tvSelectAllLabel.text = "Chọn tất cả (${items.size})"
        val allSelected = items.isNotEmpty() && items.all { it.isSelected }
        if (allSelected) {
            binding.ivSelectAll.setImageResource(R.drawable.ic_cart_checked)
        } else {
            binding.ivSelectAll.setImageResource(R.drawable.ic_cart_unchecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "CartBottomSheetFragment"
    }
}
