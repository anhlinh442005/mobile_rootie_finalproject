package com.veganbeauty.app.features.shop.product

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.databinding.ShopBottomSheetSuggestedProductsBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter
import kotlinx.coroutines.launch

class SuggestedProductsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: ShopBottomSheetSuggestedProductsBinding? = null
    private val binding get() = _binding!!

    private val productAdapter = ShopListAdapter(
        onItemClick = { product ->
            navigateToDetail(product)
            dismiss()
        },
        onAddToCartClick = { product ->
            showChooseQuantityBottomSheet(product)
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopBottomSheetSuggestedProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = productAdapter
        binding.rvProducts.setHasFixedSize(false)

        loadSuggestedProducts()
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.let { bottomSheet ->
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
    }

    private fun loadSuggestedProducts() {
        val db = RootieDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            db.productDao().getAllProducts().collect { products ->
                val suggested = products.filter {
                    it.isNew || it.price >= 500000 || it.category.contains("Combo", ignoreCase = true)
                }.take(10)
                productAdapter.submitList(suggested)
            }
        }
    }

    private fun navigateToDetail(product: ProductEntity) {
        val detailFragment = ShopDetailFragment()
        detailFragment.setProduct(product)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showChooseQuantityBottomSheet(product: ProductEntity) {
        val bottomSheet = ChooseQuantityBottomSheet(
            product = product,
            onAddToCartClick = { p, quantity ->
                CartHelper.addToCart(requireContext(), lifecycleScope, p, quantity)
            },
            onBuyNowClick = { p, quantity ->
                val checkoutItem = com.veganbeauty.app.data.local.entities.CartItemEntity(
                    id = p.id,
                    name = p.name,
                    image = p.mainImage,
                    price = p.price,
                    quantity = quantity,
                    isSelected = true
                )
                val checkoutFragment = ShopCheckoutFragment.newInstance(arrayListOf(checkoutItem))
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, checkoutFragment)
                    .addToBackStack(null)
                    .commit()
                dismiss() // Close the suggested products bottom sheet as well
            }
        )
        bottomSheet.show(parentFragmentManager, ChooseQuantityBottomSheet.TAG)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SuggestedProductsBottomSheet"
        
        fun newInstance(): SuggestedProductsBottomSheet {
            return SuggestedProductsBottomSheet()
        }
    }
}
