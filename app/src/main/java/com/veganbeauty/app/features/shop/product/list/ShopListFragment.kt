package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.databinding.ShopCategoryBinding

class ShopListFragment : RootieFragment() {
    private var _binding: ShopCategoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val categoryName = arguments?.getString("CATEGORY_NAME") ?: arguments?.getString("category") ?: "Sản phẩm"
        binding.tvTitle.text = categoryName

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val adapter = ShopListAdapter(
            onItemClick = { product ->
                val detailFragment = com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment()
                detailFragment.setProduct(product)
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(com.veganbeauty.app.R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onAddToCartClick = { product ->
                val bottomSheet = com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                    product = product,
                    onAddToCartClick = { p, quantity ->
                        android.widget.Toast.makeText(requireContext(), "Đã thêm $quantity ${p.name} vào giỏ", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onBuyNowClick = { p, quantity ->
                        android.widget.Toast.makeText(requireContext(), "Mua ngay $quantity ${p.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                bottomSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.TAG)
            }
        )

        binding.rvProducts.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = adapter

        val jsonReader = com.veganbeauty.app.data.local.LocalJsonReader(requireContext())
        val allProducts = jsonReader.getAllProducts()
        
        val filtered = allProducts.filter { it.category.contains(categoryName, ignoreCase = true) }
        if (filtered.isNotEmpty()) {
            adapter.submitList(filtered)
        } else {
            adapter.submitList(allProducts) // Fallback to all products if no exact category match
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(category: String): ShopListFragment {
            return ShopListFragment().apply {
                arguments = Bundle().apply {
                    putString("category", category)
                }
            }
        }
    }
}
