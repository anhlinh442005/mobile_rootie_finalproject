package com.veganbeauty.app.features.shop.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopHomeBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter
import com.veganbeauty.app.features.shop.product.list.ShopListFragment
import kotlinx.coroutines.launch

class ShopHomeFragment : RootieFragment() {

    private var _binding: ShopHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopHomeViewModel
    
    private val bannerAdapter = ShopHomeBannerAdapter()
    private val categoryAdapter = ShopHomeCategoryAdapter { category ->
        navigateToCategoryList(category)
    }
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopHomeBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopHomeViewModel(repository) as T
            }
        })[ShopHomeViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Thiết lập Adapter cho ViewPager Banner
        binding.vpBanner.adapter = bannerAdapter

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = categoryAdapter

        // Thiết lập Adapter cho Sản phẩm gợi ý (2 cột)
        binding.rvSuggestedProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSuggestedProducts.adapter = productAdapter

        // Open Cart when Cart Container is clicked
        binding.flCartContainer.setOnClickListener {
            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment()
            cartSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG)
        }

        // Xử lý click thanh tìm kiếm
        val searchClickListener = View.OnClickListener {
            val searchFragment = com.veganbeauty.app.features.shop.search.ShopSearchFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.veganbeauty.app.R.id.main_container, searchFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.llSearchBar.setOnClickListener(searchClickListener)
        // Make EditText behave like a button
        binding.etSearch.isFocusable = false
        binding.etSearch.isClickable = true
        binding.etSearch.setOnClickListener(searchClickListener)

        // Navigation
        com.veganbeauty.app.features.home.BottomNavHelper.setup(
            fragment = this,
            root = binding.root,
            activeTabId = com.veganbeauty.app.R.id.nav_shop
        ) { tabId -> com.veganbeauty.app.features.home.BottomNavHelper.navigate(this, tabId) }
    }

    override fun observeViewModel() {
        // Quan sát dữ liệu Banner
        lifecycleScope.launch {
            viewModel.banners.collect { banners ->
                bannerAdapter.submitList(banners)
            }
        }

        // Quan sát dữ liệu Category
        lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.submitList(categories)
            }
        }

        // Quan sát dữ liệu Sản phẩm gợi ý
        viewModel.suggestedProducts.observe(viewLifecycleOwner) { products ->
            // Chỉ hiển thị 4 sản phẩm đầu tiên cho phần "Gợi ý" theo thiết kế
            productAdapter.submitList(products.take(4))
        }

        // Observe cart items to update badge count
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

    private fun navigateToCategoryList(category: com.veganbeauty.app.features.shop.home.models.CategoryUiModel) {
        val listFragment = ShopListFragment()
        listFragment.arguments = Bundle().apply { putString("CATEGORY_NAME", category.name) }
        
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(com.veganbeauty.app.R.id.main_container, listFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
