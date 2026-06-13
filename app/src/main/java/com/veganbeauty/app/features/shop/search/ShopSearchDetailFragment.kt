package com.veganbeauty.app.features.shop.search

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopSearchDetailBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListFragment

class ShopSearchDetailFragment : RootieFragment() {

    private var _binding: ShopSearchDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: ShopSearchViewModel
    private lateinit var keywordSuggestionAdapter: SearchSuggestionAdapter
    private lateinit var contentSuggestionAdapter: SearchContentSuggestionAdapter
    private lateinit var previewProductsAdapter: HotDealsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopSearchDetailBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
        searchViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopSearchViewModel(repository, db.communityDao()) as T
            }
        })[ShopSearchViewModel::class.java]
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnClear.setOnClickListener { binding.etSearch.text.clear() }

        keywordSuggestionAdapter = SearchSuggestionAdapter(R.drawable.ic_search) { name ->
            binding.etSearch.setText(name)
            binding.etSearch.setSelection(name.length)
            performSearch()
        }
        binding.rvKeywordSuggestions.adapter = keywordSuggestionAdapter
        binding.rvKeywordSuggestions.layoutManager = LinearLayoutManager(requireContext())

        contentSuggestionAdapter = SearchContentSuggestionAdapter { item ->
            handleContentSuggestionClick(item)
        }
        binding.rvCategorySuggestions.adapter = contentSuggestionAdapter
        binding.rvCategorySuggestions.layoutManager = LinearLayoutManager(requireContext())

        previewProductsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvSearchResults.adapter = previewProductsAdapter
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())

        val initialKeyword = arguments?.getString(ARG_KEYWORD).orEmpty()
        if (initialKeyword.isNotEmpty()) {
            binding.etSearch.setText(initialKeyword)
            binding.etSearch.setSelection(initialKeyword.length)
            searchViewModel.updateSuggestions(initialKeyword)
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
                val keyword = s?.toString()?.trim().orEmpty()
                binding.btnClear.visibility = if (keyword.isEmpty()) View.GONE else View.VISIBLE
                searchViewModel.updateSuggestions(keyword)
            }
        })
    }

    override fun observeViewModel() {
        searchViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
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
                if (suggestions.contentItems.isEmpty() && suggestions.productNames.isEmpty()) View.GONE
                else if (suggestions.previewProducts.isEmpty()) View.GONE else View.VISIBLE
            binding.rvSearchResults.visibility =
                if (suggestions.previewProducts.isEmpty()) View.GONE else View.VISIBLE
        }
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
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ShopSearchResultsFragment.newInstance(keyword))
            .addToBackStack(null)
            .commit()
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

    private fun navigateToDetail(product: ProductEntity) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ShopDetailFragment().apply { setProduct(product) })
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_KEYWORD = "arg_keyword"

        fun newInstance(keyword: String = ""): ShopSearchDetailFragment {
            return ShopSearchDetailFragment().apply {
                arguments = Bundle().apply { putString(ARG_KEYWORD, keyword) }
            }
        }
    }
}
