package com.veganbeauty.app.features.shop.product.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.ShopProductDetailBinding

class ShopDetailFragment : RootieFragment() {
    private var _binding: ShopProductDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Implementation
    }

    private var product: com.veganbeauty.app.data.local.entities.ProductEntity? = null
    fun setProduct(product: com.veganbeauty.app.data.local.entities.ProductEntity) {
        this.product = product
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(productId: String): ShopDetailFragment {
            return ShopDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("product_id", productId)
                }
            }
        }
    }
}
