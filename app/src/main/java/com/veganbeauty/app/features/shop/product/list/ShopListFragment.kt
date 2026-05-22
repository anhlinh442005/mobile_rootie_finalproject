package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopCategoryBinding
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment

class ShopListFragment : RootieFragment() {

    private var _binding: ShopCategoryBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ShopViewModel
    private lateinit var subcategoryAdapter: SubcategoryAdapter
    private val productAdapter = ShopListAdapter(
        onItemClick = { product ->
            navigateToDetail(product)
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
            .replace(com.veganbeauty.app.R.id.main_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopCategoryBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RootieFragment calls setupUI(view) and observeViewModel() automatically
    }

    private fun setupViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopViewModel(repository) as T
            }
        })[ShopViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.rvProducts.adapter = productAdapter
        
        subcategoryAdapter = SubcategoryAdapter { subcategory ->
            subcategoryAdapter.selectedSubcategory = subcategory
            viewModel.setSubcategoryFilter(subcategory)
        }
        binding.rvSubcategories.adapter = subcategoryAdapter
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        val categoryName = arguments?.getString("CATEGORY_NAME")
        if (categoryName != null) {
            viewModel.setCategoryFilter(categoryName)
            binding.tvTitle.text = categoryName
        } else {
            viewModel.setCategoryFilter("Tất cả")
            binding.tvTitle.text = "Tất cả sản phẩm"
        }
    }

    override fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }
        
        viewModel.subcategories.observe(viewLifecycleOwner) { subcategories ->
            subcategoryAdapter.submitList(subcategories)
            // Reset to "Tất cả" whenever category changes
            subcategoryAdapter.selectedSubcategory = "Tất cả"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
