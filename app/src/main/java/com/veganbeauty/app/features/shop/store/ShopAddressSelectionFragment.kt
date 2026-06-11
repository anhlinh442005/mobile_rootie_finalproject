package com.veganbeauty.app.features.shop.store

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.veganbeauty.app.databinding.ShopFragmentAddressSelectionBinding
import com.veganbeauty.app.databinding.ShopItemAddressRowBinding
import kotlinx.coroutines.launch

class ShopAddressSelectionFragment : RootieFragment() {

    enum class SelectionStage {
        PROVINCE,
        DISTRICT
    }

    private var currentStage = SelectionStage.PROVINCE

    private var _binding: ShopFragmentAddressSelectionBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: RootieDatabase
    private lateinit var repository: StoreRepository

    private var allStoresList = listOf<StoreEntity>()
    private var selectedProvince: String? = null
    private var selectedDistrict: String? = null

    private var displayedItems = listOf<String>()
    private lateinit var addressAdapter: AddressAdapter

    private val vietnamProvinces = listOf(
        "Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng", "Huế",
        "Tỉnh An Giang", "Bà Rịa - Vũng Tàu", "Bắc Giang", "Bắc Kạn", "Bạc Liêu",
        "Bắc Ninh", "Bến Tre", "Bình Định", "Bình Dương", "Bình Phước", "Bình Thuận",
        "Cà Mau", "Cao Bằng", "Đắk Lắk", "Đắk Nông", "Điện Biên", "Đồng Nai",
        "Đồng Tháp", "Gia Lai", "Hà Giang", "Hà Nam", "Hà Tĩnh", "Hải Dương",
        "Hậu Giang", "Hòa Bình", "Hưng Yên", "Khánh Hòa", "Kiên Giang", "Kon Tum",
        "Lai Châu", "Lâm Đồng", "Lạng Sơn", "Lào Cai", "Long An", "Nam Định",
        "Nghệ An", "Ninh Bình", "Ninh Thuận", "Phú Thọ", "Phú Yên", "Quảng Bình",
        "Quảng Nam", "Quảng Ngãi", "Quảng Ninh", "Quảng Trị", "Sóc Trăng", "Sơn La",
        "Tây Ninh", "Thái Bình", "Thái Nguyên", "Thanh Hóa", "Thừa Thiên Huế",
        "Tiền Giang", "Trà Vinh", "Tuyên Quang", "Vĩnh Long", "Vĩnh Phúc", "Yên Bái"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        repository = StoreRepository(database.storeDao(), LocalJsonReader(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentAddressSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        binding.btnBack.setOnClickListener {
            if (currentStage == SelectionStage.DISTRICT) {
                switchToProvinceStage()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        binding.tvStep1Change.setOnClickListener {
            switchToProvinceStage()
        }

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.isVisible = query.isNotEmpty()
                filterDisplayedList(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupRecyclerView()
        loadStores()
    }

    private fun setupRecyclerView() {
        addressAdapter = AddressAdapter()
        binding.rvAddressItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAddressItems.adapter = addressAdapter
    }

    private fun loadStores() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allStores.collect { stores ->
                allStoresList = stores
                updateStageUI()
            }
        }
    }

    private fun updateStageUI() {
        val query = binding.etSearch.text.toString().trim()
        if (currentStage == SelectionStage.PROVINCE) {
            binding.clStep1.setBackgroundResource(R.drawable.bg_btn_outlined)
            binding.tvStep1Text.text = selectedProvince ?: "Chọn Tỉnh/Thành phố"
            binding.tvStep1Change.visibility = View.GONE
            binding.viewStep1Dot.setBackgroundResource(R.drawable.bg_circle_green)

            binding.viewConnectingLine.visibility = View.GONE
            binding.clStep2.visibility = View.GONE

            binding.etSearch.hint = "Nhập tìm Tỉnh/Thành phố"

            // Get provinces from both predefined list and database stores to make it complete
            val storeProvinces = allStoresList.map { it.tinhThanh }.distinct().filter { it.isNotEmpty() }
            val combined = (storeProvinces + vietnamProvinces).distinct()
            displayedItems = combined
            filterDisplayedList(query)
        } else {
            binding.clStep1.setBackgroundResource(android.R.color.transparent)
            binding.tvStep1Text.text = selectedProvince
            binding.tvStep1Change.visibility = View.VISIBLE
            binding.viewStep1Dot.setBackgroundResource(R.drawable.bg_circle_grey)

            binding.viewConnectingLine.visibility = View.VISIBLE
            binding.clStep2.visibility = View.VISIBLE
            binding.clStep2.setBackgroundResource(R.drawable.bg_btn_outlined)
            binding.tvStep2Text.text = selectedDistrict ?: "Chọn Quận/Huyện"
            binding.viewStep2Dot.setBackgroundResource(R.drawable.bg_circle_green)

            binding.etSearch.hint = "Nhập tìm Quận/Huyện"

            // Dynamically extract districts from stores in the selected province
            val prov = selectedProvince ?: ""
            val dbDistricts = allStoresList
                .filter { it.tinhThanh.equals(prov, ignoreCase = true) }
                .map { it.quanHuyen }
                .distinct()
                .filter { it.isNotEmpty() }
            
            displayedItems = dbDistricts
            filterDisplayedList(query)
        }
    }

    private fun filterDisplayedList(query: String) {
        val filtered = if (query.isEmpty()) {
            displayedItems
        } else {
            displayedItems.filter { it.contains(query, ignoreCase = true) }
        }
        addressAdapter.submitList(filtered)
    }

    private fun switchToProvinceStage() {
        currentStage = SelectionStage.PROVINCE
        selectedProvince = null
        selectedDistrict = null
        binding.etSearch.setText("")
        updateStageUI()
    }

    private fun switchToDistrictStage(province: String) {
        selectedProvince = province
        selectedDistrict = null
        currentStage = SelectionStage.DISTRICT
        binding.etSearch.setText("")
        updateStageUI()
    }

    private fun selectDistrict(district: String) {
        selectedDistrict = district
        binding.tvStep2Text.text = district
        // Hide keyboard
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)

        val province = selectedProvince
        if (!province.isNullOrEmpty() && district.isNotEmpty()) {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putString(RESULT_PROVINCE, province)
                    putString(RESULT_DISTRICT, district)
                }
            )
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AddressAdapter : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

        private var items = listOf<String>()

        fun submitList(newItems: List<String>) {
            items = newItems
            notifyDataSetChanged()
        }

        inner class AddressViewHolder(val itemBinding: ShopItemAddressRowBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
            val itemBinding = ShopItemAddressRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return AddressViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
            val item = items[position]
            holder.itemBinding.tvAddressName.text = item
            holder.itemBinding.tvAddressName.setOnClickListener {
                if (currentStage == SelectionStage.PROVINCE) {
                    switchToDistrictStage(item)
                } else {
                    selectDistrict(item)
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "ShopAddressSelectionFragment"
        const val REQUEST_KEY = "address_selection_request"
        const val RESULT_PROVINCE = "result_province"
        const val RESULT_DISTRICT = "result_district"

        fun newInstance(): ShopAddressSelectionFragment {
            return ShopAddressSelectionFragment()
        }
    }
}
