package com.veganbeauty.app.features.shop.product.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.ShopFragmentBinding
import com.veganbeauty.app.features.shop.ShopViewModel
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment

class ShopListFragment : RootieFragment() {

    private var _binding: ShopFragmentBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ShopViewModel
    private val productAdapter = ShopListAdapter { product ->
        navigateToDetail(product)
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
            .replace(R.id.main_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = ShopFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // RootieFragment calls setupUI(view) and observeViewModel() automatically
    }

    private fun setupViewModel() {
        val db = Room.databaseBuilder(requireContext(), RootieDatabase::class.java, "rootie-db").build()
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
    }

    override fun observeViewModel() {
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
