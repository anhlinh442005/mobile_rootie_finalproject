package com.veganbeauty.app.features.shop.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopSearchBinding
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment

class ShopSearchFragment : RootieFragment() {

    private var _binding: ShopSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var searchViewModel: ShopSearchViewModel
    private lateinit var shopViewModel: ShopViewModel
    
    private lateinit var hotDealsAdapter: HotDealsAdapter
    private lateinit var searchResultsAdapter: HotDealsAdapter
    
    private lateinit var keywordSuggestionAdapter: SearchSuggestionAdapter
    private lateinit var categorySuggestionAdapter: SearchSuggestionAdapter

    private var allProductsList: List<ProductEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        searchViewModel = ViewModelProvider(this)[ShopSearchViewModel::class.java]
        
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
        shopViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopViewModel(repository) as T
            }
        })[ShopViewModel::class.java]
        
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }

        setupRecyclerViews()
        setupSearch()
    }

    private fun setupRecyclerViews() {
        hotDealsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvHotDeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHotDeals.adapter = hotDealsAdapter

        searchResultsAdapter = HotDealsAdapter(onItemClick = { navigateToDetail(it) })
        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = searchResultsAdapter
        
        keywordSuggestionAdapter = SearchSuggestionAdapter(android.R.drawable.ic_menu_search) { keyword ->
            binding.etSearch.setText(keyword)
            binding.etSearch.setSelection(keyword.length)
        }
        binding.rvKeywordSuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKeywordSuggestions.adapter = keywordSuggestionAdapter

        categorySuggestionAdapter = SearchSuggestionAdapter(android.R.drawable.ic_menu_send) { category ->
            binding.etSearch.setText(category)
            binding.etSearch.setSelection(category.length)
        }
        binding.rvCategorySuggestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategorySuggestions.adapter = categorySuggestionAdapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                
                if (keyword.isEmpty()) {
                    binding.btnClear.visibility = View.GONE
                    binding.llDefaultContent.visibility = View.VISIBLE
                    binding.llSuggestions.visibility = View.GONE
                    binding.rvSearchResults.visibility = View.GONE
                    searchResultsAdapter.submitList(emptyList())
                } else {
                    binding.btnClear.visibility = View.VISIBLE
                    binding.llDefaultContent.visibility = View.GONE
                    binding.llSuggestions.visibility = View.VISIBLE
                    binding.rvSearchResults.visibility = View.VISIBLE
                    filterProducts(keyword)
                    generateSuggestions(keyword)
                }
            }
        })
    }

    private fun generateSuggestions(keyword: String) {
        val keywords = listOf(
            "$keyword bí đao",
            "Nước dưỡng $keyword",
            "Combo $keyword"
        )
        val categories = listOf(
            "Danh mục Chăm sóc $keyword",
            "Cẩm nang chăm $keyword"
        )
        keywordSuggestionAdapter.submitList(keywords)
        categorySuggestionAdapter.submitList(categories)
    }

    private fun filterProducts(keyword: String) {
        val filtered = allProductsList.filter { 
            it.name.contains(keyword, ignoreCase = true) || 
            it.description.contains(keyword, ignoreCase = true) 
        }
        searchResultsAdapter.submitList(filtered)
    }

    private fun navigateToDetail(product: ProductEntity) {
        val fragment = ShopDetailFragment().apply {
            setProduct(product)
        }
        parentFragmentManager.beginTransaction()
            .replace(com.veganbeauty.app.R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun observeViewModel() {
        searchViewModel.hotDeals.observe(viewLifecycleOwner) { deals ->
            hotDealsAdapter.submitList(deals)
        }

        shopViewModel.products.observe(viewLifecycleOwner) { products ->
            allProductsList = products
            val keyword = binding.etSearch.text?.toString()?.trim() ?: ""
            if (keyword.isNotEmpty()) filterProducts(keyword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
