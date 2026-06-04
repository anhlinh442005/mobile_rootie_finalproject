package com.veganbeauty.app.features.account.expiry

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.AccountProductExpiryFragmentBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment

class AccountProductExpiryFragment : RootieFragment() {

    private var _binding: AccountProductExpiryFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AccountProductExpiryViewModel

    private val soonAdapter = AccountProductExpiryAdapter(isGrid = false) { uiModel ->
        navigateToDetail(uiModel)
    }

    private val allAdapter = AccountProductExpiryAdapter(isGrid = true) { uiModel ->
        navigateToDetail(uiModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AccountProductExpiryFragmentBinding.inflate(inflater, container, false)
        setupViewModel()
        return binding.root
    }

    private fun setupViewModel() {
        val db = RootieDatabase.getDatabase(requireContext())
        val repository = ProductRepository(db.productDao(), LocalJsonReader(requireContext()))

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AccountProductExpiryViewModel(repository) as T
            }
        })[AccountProductExpiryViewModel::class.java]
    }

    override fun setupUI(view: View) {
        // Back action
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup lists
        binding.rvSoonProducts.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSoonProducts.adapter = soonAdapter

        binding.rvAllProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvAllProducts.adapter = allAdapter

        // Search edit text
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
        })

        // Filter button
        binding.btnFilter.setOnClickListener {
            Toast.makeText(requireContext(), "Bộ lọc đang được phát triển!", Toast.LENGTH_SHORT).show()
        }

        // View all soon button
        binding.btnViewAllSoon.setOnClickListener {
            Toast.makeText(requireContext(), "Hiển thị tất cả sản phẩm sắp hết hạn", Toast.LENGTH_SHORT).show()
        }
    }

    override fun observeViewModel() {
        // Observe soon products list
        viewModel.soonExpiryProducts.observe(viewLifecycleOwner) { products ->
            soonAdapter.submitList(products)
            if (products.isEmpty()) {
                binding.rvSoonProducts.visibility = View.GONE
                binding.tvSoonTitle.visibility = View.GONE
                binding.btnViewAllSoon.visibility = View.GONE
            } else {
                binding.rvSoonProducts.visibility = View.VISIBLE
                binding.tvSoonTitle.visibility = View.VISIBLE
                binding.btnViewAllSoon.visibility = View.VISIBLE
            }
        }

        // Observe all products list
        viewModel.allExpiryProducts.observe(viewLifecycleOwner) { products ->
            allAdapter.submitList(products)
        }
    }

    private fun navigateToDetail(uiModel: ExpiryProductUiModel) {
        val detailFragment = AccountProductExpiryDetailFragment.newInstance(uiModel.product.id)

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
