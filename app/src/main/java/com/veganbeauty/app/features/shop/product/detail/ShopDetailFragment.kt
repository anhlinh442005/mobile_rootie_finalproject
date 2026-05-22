package com.veganbeauty.app.features.shop.product.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopProductDetailBinding
import java.text.NumberFormat
import java.util.Locale

class ShopDetailFragment : RootieFragment() {

    private var _binding: ShopProductDetailBinding? = null
    private val binding get() = _binding!!

    private var product: ProductEntity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Trong thực tế nên dùng SafeArgs hoặc Bundle
        // Tạm thời lấy dữ liệu từ arguments
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup mock adapters to show dummy UI while testing
        binding.rvHandbook.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_handbook, 3)
        binding.rvRelatedProducts.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_card, 2)
        binding.rvRecentlyViewed.adapter = MockAdapter(com.veganbeauty.app.R.layout.shop_product_card, 2)

        binding.btnBuyOnline.setOnClickListener {
            product?.let { p ->
                val bottomSheet = com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                    product = p,
                    onAddToCartClick = { prod, quantity ->
                        android.widget.Toast.makeText(requireContext(), "Đã thêm $quantity ${prod.name} vào giỏ", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onBuyNowClick = { prod, quantity ->
                        android.widget.Toast.makeText(requireContext(), "Mua ngay $quantity ${prod.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                bottomSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.TAG)
            }
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
        
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        binding.tvPrice.text = formatter.format(product.price)
        
        // Tạm thời giả định giá gốc cao hơn 20%
        val originalPrice = (product.price * 1.2).toLong()
        binding.tvOriginalPrice.text = formatter.format(originalPrice)
        binding.tvOriginalPrice.paintFlags = binding.tvOriginalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

        binding.ivProductLarge.load(product.mainImage) {
            crossfade(true)
            placeholder(android.R.color.darker_gray)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class MockAdapter(private val layoutId: Int, private val itemCountToReturn: Int) : RecyclerView.Adapter<MockAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}
        override fun getItemCount() = itemCountToReturn
    }
}
