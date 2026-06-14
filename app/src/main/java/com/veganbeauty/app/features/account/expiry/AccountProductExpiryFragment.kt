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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.databinding.AccountProductExpiryFragmentBinding
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment
import kotlinx.coroutines.launch

class AccountProductExpiryFragment : RootieFragment() {

    private var _binding: AccountProductExpiryFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AccountProductExpiryViewModel

    private val soonAdapter = AccountProductExpiryAdapter(
        AccountProductExpiryAdapter.ExpiryLayoutMode.HORIZONTAL,
        onItemClick = { uiModel -> navigateToDetail(uiModel) },
        onItemLongClick = { uiModel -> showActionBottomSheet(uiModel) }
    )

    private val allAdapter = AccountProductExpiryAdapter(
        AccountProductExpiryAdapter.ExpiryLayoutMode.GRID,
        onItemClick = { uiModel -> navigateToDetail(uiModel) },
        onItemLongClick = { uiModel -> showActionBottomSheet(uiModel) }
    )

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
        val repository = ProductRepository(
            productDao = db.productDao(),
            localJsonReader = LocalJsonReader(requireContext()),
            userProductExpiryDao = db.userProductExpiryDao()
        )
        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(requireContext())

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AccountProductExpiryViewModel(repository, userId) as T
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
            val filterBottomSheet = ExpiryFilterBottomSheet.newInstance()
            filterBottomSheet.show(childFragmentManager, ExpiryFilterBottomSheet.TAG)
        }

        // View all soon button
        binding.btnViewAllSoon.setOnClickListener {
            val bottomSheet = SoonExpiryBottomSheet.newInstance()
            bottomSheet.show(childFragmentManager, SoonExpiryBottomSheet.TAG)
        }
    }

    private fun showActionBottomSheet(uiModel: ExpiryProductUiModel) {
        val actionBottomSheet = ExpiryActionBottomSheet.newInstance(
            productName = uiModel.product.name,
            productExpiry = uiModel.product.expiryDate,
            productImage = uiModel.product.mainImage
        )
        actionBottomSheet.setActions(
            onBuyAgain = {
                lifecycleScope.launch {
                    val db = RootieDatabase.getDatabase(requireContext())
                    val fullProduct = db.productDao().getProductById(uiModel.product.id)
                    val detailFragment = ShopDetailFragment().apply {
                        setProduct(fullProduct ?: uiModel.product)
                    }
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
            },
            onDelete = {
                viewModel.deleteExpiryProduct(uiModel.product.id)
                Toast.makeText(requireContext(), "Đã xoá ${uiModel.product.name} khỏi kệ", Toast.LENGTH_SHORT).show()
            }
        )
        actionBottomSheet.show(childFragmentManager, ExpiryActionBottomSheet.TAG)
    }

    override fun observeViewModel() {
        // Observe soon products list
        viewModel.soonExpiryProducts.observe(viewLifecycleOwner) { products ->
            soonAdapter.submitList(products.take(5))
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

    fun navigateToDetail(uiModel: ExpiryProductUiModel) {
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
