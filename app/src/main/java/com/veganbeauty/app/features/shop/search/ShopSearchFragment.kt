package com.veganbeauty.app.features.shop.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import com.veganbeauty.app.databinding.ShopSearchBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListFragment

class ShopSearchFragment : RootieFragment() {

    private var _binding: ShopSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: ShopSearchViewModel
    private var isSuggestionMode = false

    private lateinit var hotDealsAdapter: HotDealsAdapter
    private lateinit var previewProductsAdapter: HotDealsAdapter
    private lateinit var topSearchAdapter: SearchChipAdapter
    private lateinit var keywordSuggestionAdapter: SearchSuggestionAdapter
    private lateinit var contentSuggestionAdapter: SearchContentSuggestionAdapter
    private lateinit var historyAdapter: ShopSearchHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))

        searchViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopSearchViewModel(repository, db.communityDao()) as T
            }
        })[ShopSearchViewModel::class.java]

        binding.btnBack.setOnClickListener { handleBack() }
        binding.btnClear.setOnClickListener { binding.etSearch.text.clear() }
        binding.btnClearHistory.setOnClickListener {
            ShopSearchHistoryHelper.clear(requireContext())
            refreshSearchHistory()
        }

        setupRecyclerViews()
        setupSearch()
        refreshSearchHistory()
        updateSearchUi()
    }

    private fun setupRecyclerViews() {
        hotDealsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvHotDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHotDeals.adapter = hotDealsAdapter

        previewProductsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = previewProductsAdapter

        topSearchAdapter = SearchChipAdapter { term -> navigateToCategory(term) }
        binding.rvTopSearch.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvTopSearch.adapter = topSearchAdapter

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

        historyAdapter = ShopSearchHistoryAdapter(
            onItemClick = { query ->
                binding.etSearch.setText(query)
                binding.etSearch.setSelection(query.length)
                performSearch()
            },
            onDeleteClick = { query ->
                ShopSearchHistoryHelper.remove(requireContext(), query)
                refreshSearchHistory()
            }
        )
        binding.rvSearchHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchHistory.adapter = historyAdapter
    }

    private fun setupSearch() {
        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            isSuggestionMode = hasFocus
            updateSearchUi()
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
                    updateSearchUi()
                }
            }
        })
    }

    private fun updateSearchUi() {
        val keyword = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (!isSuggestionMode) {
            binding.llDefaultContent.visibility = View.VISIBLE
            binding.llSuggestions.visibility = View.GONE
            binding.rvSearchResults.visibility = View.GONE
            return
        }

        if (keyword.isEmpty()) {
            binding.llDefaultContent.visibility = View.VISIBLE
            binding.llSuggestions.visibility = View.GONE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.llDefaultContent.visibility = View.GONE
            binding.llSuggestions.visibility = View.VISIBLE
            searchViewModel.updateSuggestions(keyword)
        }
    }

    private fun handleBack() {
        if (isSuggestionMode) {
            exitSuggestionMode()
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun exitSuggestionMode() {
        isSuggestionMode = false
        binding.etSearch.text?.clear()
        binding.etSearch.clearFocus()
        hideKeyboard()
        updateSearchUi()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
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

    private fun performSearch() {
        val keyword = binding.etSearch.text?.toString()?.trim().orEmpty()
        if (keyword.isEmpty()) return
        ShopSearchHistoryHelper.add(requireContext(), keyword)
        refreshSearchHistory()
        exitSuggestionMode()
        navigateToSearchResults(keyword)
    }

    private fun navigateToSearchResults(keyword: String) {
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, ShopSearchResultsFragment.newInstance(keyword))
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCategory(term: String) {
        val (category, subcategory) = searchViewModel.getNavigationForTerm(term)
        navigateToCategoryList(category, subcategory)
    }

    private fun navigateToCategoryList(category: String, subcategory: String? = null) {
        val listFragment = ShopListFragment().apply {
            arguments = Bundle().apply {
                putString("CATEGORY_NAME", category)
                subcategory?.let { putString("SUBCATEGORY_NAME", it) }
            }
        }
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.main_container, listFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDetail(product: ProductEntity) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ShopDetailFragment().apply { setProduct(product) })
            .addToBackStack(null)
            .commit()
    }

    private fun refreshSearchHistory() {
        val history = ShopSearchHistoryHelper.getHistory(requireContext())
        historyAdapter.submitList(history)
        binding.llSearchHistory.visibility = if (history.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun observeViewModel() {
        searchViewModel.hotDeals.observe(viewLifecycleOwner) { deals ->
            hotDealsAdapter.submitList(deals)
        }

        searchViewModel.topSearchTerms.observe(viewLifecycleOwner) { terms ->
            topSearchAdapter.submitList(terms)
        }

        searchViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            if (!isSuggestionMode) return@observe
            keywordSuggestionAdapter.submitList(suggestions.productNames)
            contentSuggestionAdapter.submitList(suggestions.contentItems)
            previewProductsAdapter.submitList(suggestions.previewProducts)

            val keyword = binding.etSearch.text?.toString()?.trim().orEmpty()
            if (keyword.isNotEmpty()) {
                binding.llDefaultContent.visibility = View.GONE
                binding.llSuggestions.visibility = View.VISIBLE
            }

            binding.rvKeywordSuggestions.visibility =
                if (suggestions.productNames.isEmpty()) View.GONE else View.VISIBLE
            binding.rvCategorySuggestions.visibility =
                if (suggestions.contentItems.isEmpty()) View.GONE else View.VISIBLE
            binding.rvSearchResults.visibility =
                if (suggestions.previewProducts.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSearchHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
