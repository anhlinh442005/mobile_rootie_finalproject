package com.veganbeauty.app.features.shop.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopSearchResultsBinding
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.AdvancedFilterBottomSheet
import com.veganbeauty.app.features.shop.product.list.PriceFilterBottomSheet
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter
import com.veganbeauty.app.features.shop.product.list.ShopListFragment
import com.veganbeauty.app.features.shop.product.list.SkinTypeFilterBottomSheet
import androidx.lifecycle.lifecycleScope

class ShopSearchResultsFragment : RootieFragment() {

    private var _binding: ShopSearchResultsBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: ShopSearchViewModel
    private lateinit var shopViewModel: ShopViewModel

    private var keyword: String = ""
    private var showingProducts = true
    private var isSuggestionMode = false

    private lateinit var keywordSuggestionAdapter: SearchSuggestionAdapter
    private lateinit var contentSuggestionAdapter: SearchContentSuggestionAdapter
    private lateinit var previewProductsAdapter: HotDealsAdapter
    private lateinit var videoAdapter: SearchVideoAdapter

    private val productAdapter = ShopListAdapter(
        onItemClick = { product -> navigateToDetail(product) },
        onAddToCartClick = { product ->
            val bottomSheet = com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                product = product,
                onAddToCartClick = { p, quantity ->
                    com.veganbeauty.app.features.shop.product.CartHelper.addToCart(
                        requireContext(), lifecycleScope, p, quantity
                    )
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
                    val checkoutFragment = com.veganbeauty.app.features.shop.product.ShopCheckoutFragment
                        .newInstance(arrayListOf(checkoutItem))
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, checkoutFragment)
                        .addToBackStack(null)
                        .commit()
                }
            )
            bottomSheet.show(parentFragmentManager, com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.TAG)
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopSearchResultsBinding.inflate(inflater, container, false)
        keyword = arguments?.getString(ARG_KEYWORD).orEmpty()
        setupViewModels()
        return binding.root
    }

    private fun setupViewModels() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return when {
                    modelClass.isAssignableFrom(ShopSearchViewModel::class.java) ->
                        ShopSearchViewModel(repository, db.communityDao()) as T
                    modelClass.isAssignableFrom(ShopViewModel::class.java) ->
                        ShopViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel")
                }
            }
        }
        searchViewModel = ViewModelProvider(this, factory)[ShopSearchViewModel::class.java]
        shopViewModel = ViewModelProvider(this, factory)[ShopViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.etSearch.setText(keyword)
        binding.btnBack.setOnClickListener { handleBack() }
        binding.btnClear.setOnClickListener { binding.etSearch.text.clear() }

        binding.rvProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvProducts.adapter = productAdapter

        videoAdapter = SearchVideoAdapter()
        binding.rvVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideos.adapter = videoAdapter

        setupSuggestionAdapters()
        setupSearchInput()
        setupFilters()
        setupSortButtons()
        setupTabs()

        shopViewModel.setCategoryFilter("Tất cả")
        selectTab(true)
    }

    private fun setupSuggestionAdapters() {
        keywordSuggestionAdapter = SearchSuggestionAdapter(R.drawable.ic_search) { name ->
            binding.etSearch.setText(name)
            binding.etSearch.setSelection(name.length)
            performSearch()
        }
        binding.rvKeywordSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKeywordSuggestions.adapter = keywordSuggestionAdapter

        contentSuggestionAdapter = SearchContentSuggestionAdapter { item ->
            handleContentSuggestionClick(item)
        }
        binding.rvCategorySuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategorySuggestions.adapter = contentSuggestionAdapter

        previewProductsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvPreviewProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPreviewProducts.adapter = previewProductsAdapter
    }

    private fun setupSearchInput() {
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            isSuggestionMode = hasFocus
            updateSuggestionUi()
        }
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClear.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
                if (isSuggestionMode) {
                    searchViewModel.updateSuggestions(s?.toString()?.trim().orEmpty())
                }
            }
        })
    }

    private fun setupFilters() {
        binding.btnFilterAdvanced.setOnClickListener {
            AdvancedFilterBottomSheet().show(childFragmentManager, AdvancedFilterBottomSheet.TAG)
        }
        binding.btnFilterSkinType.setOnClickListener {
            SkinTypeFilterBottomSheet().show(childFragmentManager, SkinTypeFilterBottomSheet.TAG)
        }
        binding.btnFilterPrice.setOnClickListener {
            PriceFilterBottomSheet().show(childFragmentManager, PriceFilterBottomSheet.TAG)
        }
        binding.btnSortToggle.setOnClickListener {
            binding.layoutSortOptions.visibility =
                if (binding.layoutSortOptions.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }

    private fun setupSortButtons() {
        fun selectSort(selected: View, order: String) {
            val med = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_medium)
            val reg = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular)
            val options = listOf(
                binding.btnSortBestSelling to "BEST_SELLING",
                binding.btnSortNewest to "NEWEST",
                binding.btnSortPriceLow to "PRICE_LOW",
                binding.btnSortPriceHigh to "PRICE_HIGH"
            )
            for ((btn, value) in options) {
                if (btn == selected) {
                    btn.setTextColor(resources.getColor(R.color.primary, null))
                    btn.typeface = med
                } else {
                    btn.setTextColor(resources.getColor(R.color.gray_dark, null))
                    btn.typeface = reg
                }
            }
            shopViewModel.setSortOrder(order)
        }
        binding.btnSortBestSelling.setOnClickListener { selectSort(it, "BEST_SELLING") }
        binding.btnSortNewest.setOnClickListener { selectSort(it, "NEWEST") }
        binding.btnSortPriceLow.setOnClickListener { selectSort(it, "PRICE_LOW") }
        binding.btnSortPriceHigh.setOnClickListener { selectSort(it, "PRICE_HIGH") }
    }

    private fun setupTabs() {
        binding.tabProducts.setOnClickListener { selectTab(true) }
        binding.tabHandbook.setOnClickListener { selectTab(false) }
    }

    private fun selectTab(products: Boolean) {
        showingProducts = products
        val bold = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_bold)
        val regular = ResourcesCompat.getFont(requireContext(), R.font.be_vietnam_pro_regular)
        if (products) {
            binding.tabProducts.setTextColor(resources.getColor(R.color.primary, null))
            binding.tabProducts.typeface = bold
            binding.tabHandbook.setTextColor(resources.getColor(R.color.gray_dark, null))
            binding.tabHandbook.typeface = regular
            binding.layoutProductFilters.visibility = View.VISIBLE
            binding.rvProducts.visibility = View.VISIBLE
            binding.rvVideos.visibility = View.GONE
        } else {
            binding.tabHandbook.setTextColor(resources.getColor(R.color.primary, null))
            binding.tabHandbook.typeface = bold
            binding.tabProducts.setTextColor(resources.getColor(R.color.gray_dark, null))
            binding.tabProducts.typeface = regular
            binding.layoutProductFilters.visibility = View.GONE
            binding.rvProducts.visibility = View.GONE
            binding.rvVideos.visibility = View.VISIBLE
            refreshVideos()
        }
        refreshProducts()
    }

    private fun updateSuggestionUi() {
        if (isSuggestionMode) {
            binding.llSuggestions.visibility = View.VISIBLE
            binding.layoutMainContent.visibility = View.GONE
            val kw = binding.etSearch.text?.toString()?.trim().orEmpty()
            searchViewModel.updateSuggestions(kw)
        } else {
            binding.llSuggestions.visibility = View.GONE
            binding.layoutMainContent.visibility = View.VISIBLE
            binding.etSearch.clearFocus()
        }
    }

    private fun handleBack() {
        if (isSuggestionMode) {
            exitSuggestionMode()
        } else {
            navigateToSearchRoot()
        }
    }

    private fun exitSuggestionMode() {
        isSuggestionMode = false
        binding.etSearch.clearFocus()
        hideKeyboard()
        updateSuggestionUi()
    }

    private fun navigateToSearchRoot() {
        parentFragmentManager.popBackStack()
    }

    private fun performSearch() {
        keyword = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (keyword.isEmpty()) return
        ShopSearchHistoryHelper.add(requireContext(), keyword)
        exitSuggestionMode()
        refreshProducts()
        refreshVideos()
    }

    private fun handleContentSuggestionClick(item: ContentSuggestion) {
        when (item.type) {
            ContentSuggestionType.CATEGORY -> {
                val category = item.categoryName ?: return
                navigateToCategoryList(category, item.subcategoryName)
            }
            ContentSuggestionType.VIDEO -> {
                item.videoUrl?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
            }
            ContentSuggestionType.BLOG, ContentSuggestionType.POST -> performSearch()
        }
    }

    private fun navigateToCategoryList(category: String, subcategory: String? = null) {
        val listFragment = ShopListFragment().apply {
            arguments = Bundle().apply {
                putString("CATEGORY_NAME", category)
                subcategory?.let { putString("SUBCATEGORY_NAME", it) }
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, listFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun refreshProducts() {
        if (!showingProducts || keyword.isBlank()) return
        val products = shopViewModel.products.value
            ?.filter { searchViewModel.matchesKeyword(it, keyword) }
            .orEmpty()
        productAdapter.submitList(products)
        binding.tvEmpty.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun refreshVideos() {
        if (showingProducts || keyword.isBlank()) return
        val videos = searchViewModel.searchVideos(keyword)
        videoAdapter.submitList(videos)
        binding.tvEmpty.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun navigateToDetail(product: ProductEntity) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ShopDetailFragment().apply { setProduct(product) })
            .addToBackStack(null)
            .commit()
    }

    override fun observeViewModel() {
        shopViewModel.products.observe(viewLifecycleOwner) {
            if (showingProducts) refreshProducts()
        }

        searchViewModel.dataReady.observe(viewLifecycleOwner) {
            if (it == true) {
                refreshProducts()
                refreshVideos()
            }
        }

        searchViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            if (!isSuggestionMode) return@observe
            keywordSuggestionAdapter.submitList(suggestions.productNames)
            contentSuggestionAdapter.submitList(suggestions.contentItems)
            previewProductsAdapter.submitList(suggestions.previewProducts)

            binding.rvKeywordSuggestions.visibility =
                if (suggestions.productNames.isEmpty()) View.GONE else View.VISIBLE
            binding.dividerContent.visibility =
                if (suggestions.productNames.isEmpty() || suggestions.contentItems.isEmpty()) View.GONE else View.VISIBLE
            binding.rvCategorySuggestions.visibility =
                if (suggestions.contentItems.isEmpty()) View.GONE else View.VISIBLE
            binding.dividerProducts.visibility =
                if (suggestions.previewProducts.isEmpty()) View.GONE else View.VISIBLE
            binding.rvPreviewProducts.visibility =
                if (suggestions.previewProducts.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_KEYWORD = "arg_keyword"

        fun newInstance(keyword: String): ShopSearchResultsFragment {
            return ShopSearchResultsFragment().apply {
                arguments = Bundle().apply { putString(ARG_KEYWORD, keyword) }
            }
        }
    }
}
