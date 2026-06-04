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
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopSearchDetailBinding
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter
import com.veganbeauty.app.data.local.entities.ProductEntity

class ShopSearchDetailFragment : RootieFragment() {

    private var _binding: ShopSearchDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ShopViewModel
    
    private val productAdapter = ShopListAdapter(
        onItemClick = { product ->
            val detailFragment = ShopDetailFragment()
            detailFragment.setProduct(product)
            parentFragmentManager.beginTransaction()
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
        _binding = ShopSearchDetailBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db")
            .fallbackToDestructiveMigration()
            .build()
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))
        
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ShopViewModel(repository) as T
            }
        })[ShopViewModel::class.java]
    }

    private var allProductsList: List<ProductEntity> = emptyList()

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchResults.adapter = productAdapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val keyword = s?.toString()?.trim() ?: ""
                filterProducts(keyword)
            }
        })
        
        // Cập nhật hiển thị (nếu có keyword sẵn)
        binding.etSearch.setText("")
    }

    override fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            allProductsList = products
            val keyword = binding.etSearch.text?.toString()?.trim() ?: ""
            filterProducts(keyword)
        }
    }

    private fun filterProducts(keyword: String) {
        if (keyword.isEmpty()) {
            productAdapter.submitList(emptyList())
        } else {
            val filtered = allProductsList.filter { 
                it.name.contains(keyword, ignoreCase = true) || 
                it.description.contains(keyword, ignoreCase = true) 
            }
            productAdapter.submitList(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
