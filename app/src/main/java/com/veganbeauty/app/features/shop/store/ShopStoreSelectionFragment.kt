package com.veganbeauty.app.features.shop.store

import android.app.AlertDialog
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
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.RootieDatabase
import com.veganbeauty.app.data.local.entities.StoreEntity
import com.veganbeauty.app.data.repository.StoreRepository
import com.veganbeauty.app.databinding.ShopFragmentStoreSelectionBinding
import com.veganbeauty.app.databinding.ShopItemStoreBinding
import com.veganbeauty.app.databinding.ShopItemSearchHistoryBinding
import kotlinx.coroutines.launch

class ShopStoreSelectionFragment : RootieFragment() {

    enum class SearchState {
        NORMAL,
        SEARCH_HISTORY,
        SEARCH_RESULTS
    }

    private var currentState = SearchState.NORMAL

    private var _binding: ShopFragmentStoreSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: RootieDatabase
    private lateinit var repository: StoreRepository

    private var isSelectionMode = true
    private var selectedStoreId: String? = null

    private var allStoresList = listOf<StoreEntity>()
    private var filteredStoresList = listOf<StoreEntity>()
    private lateinit var storeAdapter: StoreAdapter
    private lateinit var historyAdapter: HistoryAdapter

    private var selectedProvince: String? = null
    private var selectedDistrict: String? = null

    private var startInSearchMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        repository = StoreRepository(database.storeDao(), LocalJsonReader(requireContext()))

        arguments?.let {
            isSelectionMode = it.getBoolean(ARG_IS_SELECTION_MODE, true)
            selectedStoreId = it.getString(ARG_SELECTED_STORE_ID)
            startInSearchMode = it.getBoolean(ARG_START_IN_SEARCH_MODE, false)
            selectedProvince = it.getString(ARG_INITIAL_PROVINCE)
            selectedDistrict = it.getString(ARG_INITIAL_DISTRICT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentStoreSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back button
        binding.btnBack.setOnClickListener {
            if (currentState != SearchState.NORMAL) {
                updateUIState(SearchState.NORMAL)
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        // Toggle Search Input to History Mode
        binding.btnSearch.setOnClickListener {
            updateUIState(SearchState.SEARCH_HISTORY)
            binding.etSearch.requestFocus()
        }

        // Clear search text button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
            updateUIState(SearchState.SEARCH_HISTORY)
        }

        // Search text watcher
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.isVisible = query.isNotEmpty()
                if (currentState != SearchState.NORMAL) {
                    if (query.isEmpty()) {
                        updateUIState(SearchState.SEARCH_HISTORY)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Search action listener on keyboard
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    addToSearchHistory(query)
                    performSearch()
                }
                true
            } else {
                false
            }
        }

        // Clear all history
        binding.btnClearAll.setOnClickListener {
            clearSearchHistory()
            refreshHistoryList()
        }

        // Region selector click
        binding.clRegionSelectorInner.setOnClickListener {
            val addressSelectionFragment = ShopAddressSelectionFragment.newInstance()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, addressSelectionFragment)
                .addToBackStack(null)
                .commit()
        }

        // Listen for address selection result
        parentFragmentManager.setFragmentResultListener(
            ShopAddressSelectionFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val province = bundle.getString(ShopAddressSelectionFragment.RESULT_PROVINCE)
            val district = bundle.getString(ShopAddressSelectionFragment.RESULT_DISTRICT)
            if (!province.isNullOrEmpty() && !district.isNullOrEmpty()) {
                selectedProvince = province
                selectedDistrict = district
                binding.tvSelectedRegion.text = "$province, $district"
                filterStores()
            }
        }

        // Confirm Selection button
        binding.btnSelect.setOnClickListener {
            if (selectedStoreId == null) {
                Toast.makeText(requireContext(), "Vui lòng chọn một cửa hàng", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selectedStore = allStoresList.find { it.id == selectedStoreId }
            if (selectedStore != null) {
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putString(RESULT_STORE_ID, selectedStore.id)
                        putString(RESULT_STORE_NAME, selectedStore.tenCuaHang)
                        putString(RESULT_STORE_ADDRESS, selectedStore.diaChiDayDu)
                    }
                )
                parentFragmentManager.popBackStack()
            }
        }

        setupRecyclerView()
        setupHistoryRecyclerView()
        
        // Initialize State after adapters are set up
        if (selectedProvince != null && selectedDistrict != null) {
            binding.tvSelectedRegion.text = "$selectedProvince, $selectedDistrict"
        }

        if (startInSearchMode) {
            updateUIState(SearchState.SEARCH_HISTORY)
            binding.etSearch.postDelayed({
                if (_binding != null) {
                    binding.etSearch.requestFocus()
                    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                }
            }, 100)
        } else {
            updateUIState(SearchState.NORMAL)
        }
        
        loadAndSyncStores()
    }

    private fun setupRecyclerView() {
        storeAdapter = StoreAdapter()
        binding.rvStores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStores.adapter = storeAdapter
    }

    private fun loadAndSyncStores() {
        // Listen to local DB stores
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allStores.collect { stores ->
                allStoresList = stores
                filterStores()
            }
        }

        // Fetch updates from network/assets fallback
        viewLifecycleOwner.lifecycleScope.launch {
            repository.refreshStores()
        }
    }

    private fun filterStores() {
        val query = binding.etSearch.text.toString().trim()
        var list = allStoresList

        // Filter by province
        if (!selectedProvince.isNullOrEmpty()) {
            list = list.filter { it.tinhThanh.equals(selectedProvince, ignoreCase = true) }
        }

        // Filter by district
        if (!selectedDistrict.isNullOrEmpty()) {
            list = list.filter { it.quanHuyen.equals(selectedDistrict, ignoreCase = true) }
        }

        // Filter by search query
        if (query.isNotEmpty()) {
            list = list.filter {
                it.tenCuaHang.contains(query, ignoreCase = true) ||
                        it.diaChiDayDu.contains(query, ignoreCase = true)
            }
        }

        filteredStoresList = list
        storeAdapter.notifyDataSetChanged()

        // Update count text
        binding.tvStoreCount.text = "Tìm thấy ${filteredStoresList.size} cửa hàng gần bạn"
    }

    private fun showRegionSelectionDialog() {
        // Extract all provinces
        val provinces = allStoresList.map { it.tinhThanh }.distinct().filter { it.isNotEmpty() }.toMutableList()
        provinces.add(0, "Tất cả")

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn Tỉnh/Thành phố")
            .setItems(provinces.toTypedArray()) { _, index ->
                val province = provinces[index]
                if (province == "Tất cả") {
                    selectedProvince = null
                    selectedDistrict = null
                    binding.tvSelectedRegion.text = "Chọn Tỉnh/Thành phố, Quận/Huyện"
                    filterStores()
                } else {
                    selectedProvince = province
                    selectedDistrict = null
                    binding.tvSelectedRegion.text = province
                    filterStores()
                    showDistrictSelectionDialog(province)
                }
            }
            .show()
    }

    private fun showDistrictSelectionDialog(province: String) {
        val districts = allStoresList
            .filter { it.tinhThanh.equals(province, ignoreCase = true) }
            .map { it.quanHuyen }
            .distinct()
            .filter { it.isNotEmpty() }
            .toMutableList()

        if (districts.isEmpty()) return

        districts.add(0, "Tất cả Quận/Huyện")

        AlertDialog.Builder(requireContext())
            .setTitle("Chọn Quận/Huyện")
            .setItems(districts.toTypedArray()) { _, index ->
                val district = districts[index]
                if (district == "Tất cả Quận/Huyện") {
                    selectedDistrict = null
                    binding.tvSelectedRegion.text = province
                } else {
                    selectedDistrict = district
                    binding.tvSelectedRegion.text = "$province, $district"
                }
                filterStores()
            }
            .show()
    }

    private fun updateUIState(state: SearchState) {
        currentState = state
        when (state) {
            SearchState.NORMAL -> {
                binding.tvTitle.visibility = View.VISIBLE
                binding.btnSearch.visibility = View.VISIBLE
                binding.clSearchBar.visibility = View.GONE
                binding.cvRegionSelector.visibility = View.VISIBLE
                binding.tvStoreCount.visibility = View.VISIBLE
                binding.rvStores.visibility = View.VISIBLE
                binding.llSearchHistory.visibility = View.GONE
                binding.flBottomButtonContainer.isVisible = isSelectionMode

                // Clear search input text & filter
                binding.etSearch.setText("")
                filterStores()
            }
            SearchState.SEARCH_HISTORY -> {
                binding.tvTitle.visibility = View.GONE
                binding.btnSearch.visibility = View.GONE
                binding.clSearchBar.visibility = View.VISIBLE
                binding.cvRegionSelector.visibility = View.GONE
                binding.tvStoreCount.visibility = View.GONE
                binding.rvStores.visibility = View.GONE
                binding.llSearchHistory.visibility = View.VISIBLE
                binding.flBottomButtonContainer.visibility = View.GONE

                // Refresh history recyclerview
                refreshHistoryList()
            }
            SearchState.SEARCH_RESULTS -> {
                binding.tvTitle.visibility = View.GONE
                binding.btnSearch.visibility = View.GONE
                binding.clSearchBar.visibility = View.VISIBLE
                binding.cvRegionSelector.visibility = View.GONE
                binding.tvStoreCount.visibility = View.VISIBLE
                binding.rvStores.visibility = View.VISIBLE
                binding.llSearchHistory.visibility = View.GONE
                binding.flBottomButtonContainer.isVisible = isSelectionMode

                filterStores()
            }
        }
    }

    private fun performSearch() {
        updateUIState(SearchState.SEARCH_RESULTS)
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    private fun getSearchHistory(): List<String> {
        val prefs = requireContext().getSharedPreferences("StoreSearchPrefs", Context.MODE_PRIVATE)
        val historyString = prefs.getString("search_history", "") ?: ""
        if (historyString.isEmpty()) return emptyList()
        return historyString.split(",").filter { it.isNotEmpty() }
    }

    private fun saveSearchHistory(history: List<String>) {
        val prefs = requireContext().getSharedPreferences("StoreSearchPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("search_history", history.joinToString(",")).apply()
    }

    private fun addToSearchHistory(query: String) {
        if (query.isEmpty()) return
        val currentHistory = getSearchHistory().toMutableList()
        currentHistory.remove(query) // Remove if exists to move to top
        currentHistory.add(0, query)
        if (currentHistory.size > 10) {
            currentHistory.removeAt(currentHistory.lastIndex)
        }
        saveSearchHistory(currentHistory)
    }

    private fun removeFromSearchHistory(query: String) {
        val currentHistory = getSearchHistory().toMutableList()
        currentHistory.remove(query)
        saveSearchHistory(currentHistory)
    }

    private fun clearSearchHistory() {
        saveSearchHistory(emptyList())
    }

    private fun setupHistoryRecyclerView() {
        historyAdapter = HistoryAdapter(
            getSearchHistory(),
            onItemClick = { query ->
                binding.etSearch.setText(query)
                performSearch()
            },
            onDeleteClick = { query ->
                removeFromSearchHistory(query)
                refreshHistoryList()
            }
        )
        binding.rvSearchHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearchHistory.adapter = historyAdapter
    }

    private fun refreshHistoryList() {
        if (::historyAdapter.isInitialized) {
            historyAdapter.updateItems(getSearchHistory())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Search History RecyclerView Adapter
    inner class HistoryAdapter(
        private var items: List<String>,
        private val onItemClick: (String) -> Unit,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        inner class HistoryViewHolder(val itemBinding: ShopItemSearchHistoryBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val itemBinding = ShopItemSearchHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return HistoryViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = items[position]
            holder.itemBinding.tvSearchTerm.text = item
            holder.itemBinding.root.setOnClickListener { onItemClick(item) }
            holder.itemBinding.btnDelete.setOnClickListener { onDeleteClick(item) }
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }
    }

    // RecyclerView Adapter
    inner class StoreAdapter : RecyclerView.Adapter<StoreAdapter.StoreViewHolder>() {

        inner class StoreViewHolder(val itemBinding: ShopItemStoreBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreViewHolder {
            val itemBinding = ShopItemStoreBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return StoreViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: StoreViewHolder, position: Int) {
            val store = filteredStoresList[position]
            val b = holder.itemBinding

            b.tvStoreName.text = store.tenCuaHang
            b.tvStoreHours.text = "Mở cửa từ ${store.moCua} đến ${store.dongCua}"
            b.tvStoreAddress.text = store.diaChiDayDu

            // Selection controls visibility
            b.ivRadio.isVisible = isSelectionMode
            if (isSelectionMode) {
                if (store.id == selectedStoreId) {
                    b.ivRadio.setImageResource(R.drawable.ic_cart_checked)
                    b.ivRadio.imageTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#3E4D44")
                    )
                } else {
                    b.ivRadio.setImageResource(R.drawable.ic_cart_unchecked)
                    b.ivRadio.imageTintList = android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#807F7F")
                    )
                }
            }

            // Click listener for selection
            if (isSelectionMode) {
                b.root.setOnClickListener {
                    selectedStoreId = store.id
                    notifyDataSetChanged()
                }
            } else {
                b.root.setOnClickListener {
                    val detailFragment = ShopStoreDetailFragment.newInstance(store)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit()
                }
            }

            // Click listener for directions
            b.btnDirections.setOnClickListener {
                try {
                    val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${store.lat},${store.lng}")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                    holder.itemView.context.startActivity(mapIntent)
                } catch (e: Exception) {
                    Toast.makeText(holder.itemView.context, "Không thể mở bản đồ", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int = filteredStoresList.size
    }

    companion object {
        const val TAG = "ShopStoreSelectionFragment"
        const val REQUEST_KEY = "store_selection_request"
        const val RESULT_STORE_ID = "result_store_id"
        const val RESULT_STORE_NAME = "result_store_name"
        const val RESULT_STORE_ADDRESS = "result_store_address"

        private const val ARG_IS_SELECTION_MODE = "is_selection_mode"
        private const val ARG_SELECTED_STORE_ID = "selected_store_id"
        private const val ARG_START_IN_SEARCH_MODE = "start_in_search_mode"
        private const val ARG_INITIAL_PROVINCE = "initial_province"
        private const val ARG_INITIAL_DISTRICT = "initial_district"

        fun newInstance(
            isSelectionMode: Boolean = true,
            selectedStoreId: String? = null,
            startInSearchMode: Boolean = false,
            initialProvince: String? = null,
            initialDistrict: String? = null
        ): ShopStoreSelectionFragment {
            return ShopStoreSelectionFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_SELECTION_MODE, isSelectionMode)
                    putString(ARG_SELECTED_STORE_ID, selectedStoreId)
                    putBoolean(ARG_START_IN_SEARCH_MODE, startInSearchMode)
                    putString(ARG_INITIAL_PROVINCE, initialProvince)
                    putString(ARG_INITIAL_DISTRICT, initialDistrict)
                }
            }
        }
    }
}
