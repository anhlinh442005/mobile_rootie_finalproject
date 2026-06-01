package com.veganbeauty.app.features.shop.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopBottomSheetBuyBinding
import java.text.NumberFormat
import java.util.Locale

class ChooseQuantityBottomSheet(
    private val product: ProductEntity,
    private val onAddToCartClick: (ProductEntity, Int) -> Unit,
    private val onBuyNowClick: (ProductEntity, Int) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetBuyBinding? = null
    private val binding get() = _binding!!

    private var currentQuantity = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetBuyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProductInfo()
        setupListeners()
        updatePriceAndSavings()
    }

    private fun setupProductInfo() {
        binding.tvProductName.text = product.name
        
        binding.ivProduct.load(product.mainImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }

        binding.tvQuantityValue.text = currentQuantity.toString()
    }

    private fun setupListeners() {
        binding.btnMinus.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                binding.tvQuantityValue.text = currentQuantity.toString()
                updatePriceAndSavings()
            }
        }

        binding.btnPlus.setOnClickListener {
            currentQuantity++
            binding.tvQuantityValue.text = currentQuantity.toString()
            updatePriceAndSavings()
        }

        binding.btnAddToCartOutline.setOnClickListener {
            onAddToCartClick(product, currentQuantity)
            dismiss()
        }

        binding.btnBuyNow.setOnClickListener {
            onBuyNowClick(product, currentQuantity)
            dismiss()
        }
    }

    private fun updatePriceAndSavings() {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        
        // Cập nhật giá
        val currentPrice = product.price
        val originalPrice = (product.price * 1.2).toLong() // Giả định giá gốc
        
        binding.tvPrice.text = formatter.format(currentPrice)
        binding.tvOriginalPrice.text = formatter.format(originalPrice)
        binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        // Tạm tính
        val subtotal = currentPrice * currentQuantity
        binding.tvSubtotalValue.text = formatter.format(subtotal)
        
        // Tiết kiệm được
        val savings = (originalPrice - currentPrice) * currentQuantity
        binding.tvSavingsValue.text = formatter.format(savings)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ChooseQuantityBottomSheet"
    }
}
