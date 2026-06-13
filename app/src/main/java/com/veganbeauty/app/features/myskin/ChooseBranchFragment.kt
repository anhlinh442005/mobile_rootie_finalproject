package com.veganbeauty.app.features.myskin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.veganbeauty.app.R
import com.veganbeauty.app.core.base.RootieFragment
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.entities.StoreEntity

class ChooseBranchFragment : RootieFragment() {

    private lateinit var rvBranches: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var filterAll: TextView
    private lateinit var filterHcm: TextView
    private lateinit var filterHn: TextView
    private lateinit var filterDn: TextView
    private lateinit var btnContinue: TextView
    private lateinit var btnBack: ImageView

    private lateinit var branchAdapter: BranchAdapter
    private var allStores: List<StoreEntity> = emptyList()
    private var currentFilter = "ALL"
    private var currentQuery = ""
    private var selectedStore: StoreEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.skin_fragment_choose_branch, container, false)
    }

    override fun setupUI(view: View) {
        rvBranches = view.findViewById(R.id.rv_branches)
        etSearch = view.findViewById(R.id.et_search)
        filterAll = view.findViewById(R.id.filter_all)
        filterHcm = view.findViewById(R.id.filter_hcm)
        filterHn = view.findViewById(R.id.filter_hn)
        filterDn = view.findViewById(R.id.filter_dn)
        btnContinue = view.findViewById(R.id.btn_continue)
        btnBack = view.findViewById(R.id.btn_back)

        // Load data
        val jsonReader = LocalJsonReader(requireContext())
        allStores = jsonReader.getAllStores()

        // Setup RecyclerView
        rvBranches.layoutManager = LinearLayoutManager(context)
        branchAdapter = BranchAdapter(allStores) { store ->
            selectedStore = store
        }
        rvBranches.adapter = branchAdapter

        // Setup Search
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentQuery = s?.toString()?.trim() ?: ""
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Setup Filters
        filterAll.setOnClickListener { setFilter("ALL", filterAll) }
        filterHcm.setOnClickListener { setFilter("Hồ Chí Minh", filterHcm) }
        filterHn.setOnClickListener { setFilter("Hà Nội", filterHn) }
        filterDn.setOnClickListener { setFilter("Đà Nẵng", filterDn) }

        // Setup Buttons
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnContinue.setOnClickListener {
            if (selectedStore != null) {
                val bookingFragment = BookingFragment.newInstance(
                    selectedStore!!.tenCuaHang,
                    selectedStore!!.diaChiDayDu,
                    ""
                )
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        android.R.anim.slide_in_left,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.slide_out_right
                    )
                    .replace(R.id.main_container, bookingFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(context, "Vui lòng chọn một chi nhánh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun observeViewModel() {
        // Not used
    }

    private fun setFilter(province: String, selectedTextView: TextView) {
        currentFilter = province
        
        // Reset styles
        val unselectedBg = R.drawable.skin_bg_store_card
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.primary)
        
        filterAll.setBackgroundResource(unselectedBg)
        filterAll.setTextColor(unselectedColor)
        
        filterHcm.setBackgroundResource(unselectedBg)
        filterHcm.setTextColor(unselectedColor)
        
        filterHn.setBackgroundResource(unselectedBg)
        filterHn.setTextColor(unselectedColor)
        
        filterDn.setBackgroundResource(unselectedBg)
        filterDn.setTextColor(unselectedColor)
        
        // Set selected style
        selectedTextView.setBackgroundResource(R.drawable.skin_bg_btn_book)
        selectedTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.neutral))

        applyFilters()
    }

    private fun applyFilters() {
        var filteredList = allStores

        // Filter by province
        if (currentFilter != "ALL") {
            filteredList = filteredList.filter { 
                it.tinhThanh.contains(currentFilter, ignoreCase = true) || 
                it.diaChiDayDu.contains(currentFilter, ignoreCase = true)
            }
        }

        // Filter by search query
        if (currentQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.tenCuaHang.contains(currentQuery, ignoreCase = true) || 
                it.diaChiDayDu.contains(currentQuery, ignoreCase = true)
            }
        }

        branchAdapter.updateData(filteredList)
    }
}
