package com.veganbeauty.app.features.shop.product.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopFragmentDetailBinding
import java.text.NumberFormat
import java.util.Locale

class ShopDetailFragment : RootieFragment() {

    private var _binding: ShopFragmentDetailBinding? = null
    private val binding get() = _binding!!

    private var product: ProductEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Trong thực tế nên dùng SafeArgs hoặc Bundle
        // Tạm thời lấy dữ liệu từ arguments
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopFragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Hiển thị dữ liệu sản phẩm nếu có
        product?.let { displayProduct(it) }
    }

    fun setProduct(product: ProductEntity) {
        this.product = product
        if (_binding != null) {
            displayProduct(product)
        }
    }

    private fun displayProduct(product: ProductEntity) {
        binding.tvProductName.text = product.name
        binding.tvDescription.text = product.description
        binding.tvCategory.text = product.category
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        binding.tvPrice.text = formatter.format(product.price)

        binding.ivProductLarge.load(product.mainImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
