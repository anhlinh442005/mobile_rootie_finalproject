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
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import kotlinx.coroutines.launch

class ShopHomeFragment : RootieFragment() {

    private var _binding: ShopHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopHomeViewModel
    
    private val sliderHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        if (_binding != null) {
            val current = binding.vpBanner.currentItem
            val total = binding.vpBanner.adapter?.itemCount ?: 0
            if (total > 0) {
                binding.vpBanner.currentItem = (current + 1) % total
            }
        }
    }
    
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

    private var cartVoucherCode: String? = null
    private var cartVoucherDiscount = 0L

    override fun setupUI(view: View) {
        // Thiết lập Adapter cho ViewPager Banner
        binding.vpBanner.adapter = bannerAdapter
        
        binding.vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateBannerDots(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 4000)
            }
        })

        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvCategories.adapter = categoryAdapter

        // Thiết lập Adapter cho Sản phẩm gợi ý (2 cột)
        binding.rvSuggestedProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSuggestedProducts.adapter = productAdapter

        // Set click listener for View All button to show SuggestedProductsBottomSheet
        binding.btnViewAll.setOnClickListener {
            val suggestedBottomSheet = com.veganbeauty.app.features.shop.product.SuggestedProductsBottomSheet.newInstance()
            suggestedBottomSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.SuggestedProductsBottomSheet.TAG)
        }

        // Open Cart when Cart Container is clicked
        binding.flCartContainer.setOnClickListener {
            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                cartVoucherCode,
                cartVoucherDiscount
            )
            cartSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.TAG)
        }

        // Voucher result listener to re-open cart
        parentFragmentManager.setFragmentResultListener(
            com.veganbeauty.app.features.shop.product.ShopVoucherFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val code = bundle.getString(com.veganbeauty.app.features.shop.product.ShopVoucherFragment.RESULT_VOUCHER_CODE)
            val discount = bundle.getLong(com.veganbeauty.app.features.shop.product.ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L)
            cartVoucherCode = code
            cartVoucherDiscount = discount

            val cartSheet = com.veganbeauty.app.features.shop.product.CartBottomSheetFragment.newInstance(
                cartVoucherCode,
                cartVoucherDiscount
            )
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
                setupBannerDots(banners.size)
            }
        }

        // Quan sát dữ liệu Category
        lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoryAdapter.submitList(categories)
            }
        }

        // Quan sát dữ liệu Sản phẩm gợi ý (Lọc sản phẩm Hot hoặc Mới)
        viewModel.suggestedProducts.observe(viewLifecycleOwner) { products ->
            val filtered = products.filter {
                it.isNew || it.price >= 500000 || it.category.contains("Combo", ignoreCase = true)
            }.take(4)
            productAdapter.submitList(filtered)
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

    private fun setupBannerDots(count: Int) {
        if (_binding == null) return
        binding.llBannerDots.removeAllViews()
        repeat(count) {
            val dot = View(requireContext())
            val size = resources.getDimensionPixelSize(com.veganbeauty.app.R.dimen.home_banner_dot_size)
            val params = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = resources.getDimensionPixelSize(com.veganbeauty.app.R.dimen.home_banner_dot_margin)
            }
            dot.layoutParams = params
            dot.background = createDotDrawable(false)
            binding.llBannerDots.addView(dot)
        }
        updateBannerDots(0)
    }

    private fun updateBannerDots(activeIndex: Int) {
        if (_binding == null) return
        for (i in 0 until binding.llBannerDots.childCount) {
            binding.llBannerDots.getChildAt(i).background = createDotDrawable(i == activeIndex)
        }
    }

    private fun createDotDrawable(active: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (active) 0xFFFFFFFF.toInt() else 0x88FFFFFF.toInt())
        }
    }

    override fun onResume() {
        super.onResume()
        if (bannerAdapter.itemCount > 0) {
            sliderHandler.removeCallbacks(sliderRunnable)
            sliderHandler.postDelayed(sliderRunnable, 4000)
        }
    }

    override fun onPause() {
        sliderHandler.removeCallbacks(sliderRunnable)
        super.onPause()
    }

    override fun onDestroyView() {
        sliderHandler.removeCallbacks(sliderRunnable)
        super.onDestroyView()
        _binding = null
    }
}
