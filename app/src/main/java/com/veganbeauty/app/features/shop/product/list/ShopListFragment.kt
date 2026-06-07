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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
                    com.veganbeauty.app.features.shop.product.CartHelper.addToCart(requireContext(), lifecycleScope, p, quantity)
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
                    val checkoutFragment = com.veganbeauty.app.features.shop.product.ShopCheckoutFragment.newInstance(arrayListOf(checkoutItem))
                    parentFragmentManager.beginTransaction()
                        .replace(com.veganbeauty.app.R.id.main_container, checkoutFragment)
                        .addToBackStack(null)
                        .commit()
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
        val db = RootieDatabase.getDatabase(requireContext())
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

        binding.btnSearch.setOnClickListener {
            val searchFragment = com.veganbeauty.app.features.shop.search.ShopSearchFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnFilterAdvanced.setOnClickListener {
            val filterSheet = AdvancedFilterBottomSheet()
            filterSheet.show(childFragmentManager, AdvancedFilterBottomSheet.TAG)
        }

        binding.btnFilterSkinType.setOnClickListener {
            val skinTypeSheet = SkinTypeFilterBottomSheet()
            skinTypeSheet.show(childFragmentManager, SkinTypeFilterBottomSheet.TAG)
        }

        binding.btnFilterPrice.setOnClickListener {
            val priceSheet = PriceFilterBottomSheet()
            priceSheet.show(childFragmentManager, PriceFilterBottomSheet.TAG)
        }

        binding.btnCart.setOnClickListener {
            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment()
            cartSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG)
        }

        binding.btnSortToggle.setOnClickListener {
            if (binding.layoutSortOptions.visibility == View.VISIBLE) {
                binding.layoutSortOptions.visibility = View.GONE
            } else {
                binding.layoutSortOptions.visibility = View.VISIBLE
            }
        }
        
        fun selectSortOption(selectedView: android.widget.TextView, sortOrder: String) {
            val medTypeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), com.veganbeauty.app.R.font.be_vietnam_pro_medium)
            val regTypeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), com.veganbeauty.app.R.font.be_vietnam_pro_regular)
            
            val options = listOf(
                binding.btnSortBestSelling to "BEST_SELLING",
                binding.btnSortNewest to "NEWEST",
                binding.btnSortPriceLow to "PRICE_LOW",
                binding.btnSortPriceHigh to "PRICE_HIGH"
            )
            
            for ((view, order) in options) {
                if (view == selectedView) {
                    view.setTextColor(android.graphics.Color.parseColor("#3E4D44"))
                    view.typeface = medTypeface
                } else {
                    view.setTextColor(android.graphics.Color.parseColor("#888888"))
                    view.typeface = regTypeface
                }
            }
            viewModel.setSortOrder(sortOrder)
        }
        
        binding.btnSortBestSelling.setOnClickListener { selectSortOption(binding.btnSortBestSelling, "BEST_SELLING") }
        binding.btnSortNewest.setOnClickListener { selectSortOption(binding.btnSortNewest, "NEWEST") }
        binding.btnSortPriceLow.setOnClickListener { selectSortOption(binding.btnSortPriceLow, "PRICE_LOW") }
        binding.btnSortPriceHigh.setOnClickListener { selectSortOption(binding.btnSortPriceHigh, "PRICE_HIGH") }
        
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
            binding.rvProducts.scrollToPosition(0)
        }
        
        viewModel.subcategories.observe(viewLifecycleOwner) { subcategories ->
            subcategoryAdapter.submitList(subcategories)
            // Reset to "Tất cả" whenever category changes
            subcategoryAdapter.selectedSubcategory = "Tất cả"
        }

        lifecycleScope.launch {
            val db = RootieDatabase.getDatabase(requireContext())
            db.cartDao().getAllCartItems().collect { items ->
                val totalQty = items.sumOf { it.quantity }
                if (totalQty > 0) {
                    binding.tvCartBadge.visibility = View.VISIBLE
                    binding.tvCartBadge.text = totalQty.toString()
                } else {
                    binding.tvCartBadge.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
