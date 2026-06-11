package com.veganbeauty.app.features.shop.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.veganbeauty.app.databinding.ShopFragmentStoreDetailBinding
import com.veganbeauty.app.databinding.ShopItemStoreBinding
import kotlinx.coroutines.launch

class ShopStoreDetailFragment : RootieFragment() {

    private var _binding: ShopFragmentStoreDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: RootieDatabase
    private lateinit var repository: StoreRepository

    private lateinit var currentStore: StoreEntity
    private var nearbyStoresList = listOf<StoreEntity>()
    private lateinit var nearbyAdapter: NearbyStoresAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = RootieDatabase.getDatabase(requireContext())
        repository = StoreRepository(database.storeDao(), LocalJsonReader(requireContext()))

        arguments?.let {
            @Suppress("DEPRECATION")
            currentStore = it.getSerializable(ARG_STORE) as StoreEntity
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentStoreDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun setupUI(view: View) {
        // Back Button
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Bind Store Data
        binding.tvStoreNameDetail.text = currentStore.tenCuaHang
        binding.tvStoreHoursDetail.text = "Mở cửa từ ${currentStore.moCua} đến ${currentStore.dongCua}"
        binding.tvStoreAddressDetail.text = currentStore.diaChiDayDu

        // Amenities
        populateAmenities()

        // Setup RecyclerView for nearby stores
        nearbyAdapter = NearbyStoresAdapter()
        binding.rvNearbyStores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNearbyStores.adapter = nearbyAdapter

        // Bottom Actions
        binding.btnXemChiDuong.setOnClickListener {
            openGoogleMaps(currentStore.lat, currentStore.lng)
        }

        binding.btnLienHe.setOnClickListener {
            dialPhoneNumber(currentStore.soDienThoai)
        }

        loadNearbyStores()
    }

    private fun populateAmenities() {
        binding.cgAmenitiesDetail.removeAllViews()
        val amenities = currentStore.tienNghi
        if (amenities.isNotEmpty()) {
            val list = amenities.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            for (item in list) {
                val tv = TextView(requireContext()).apply {
                    text = item
                    setTextColor(resources.getColor(R.color.primary, null))
                    setBackgroundResource(R.drawable.bg_amenity_tag)
                    setPadding(
                        dpToPx(12),
                        dpToPx(6),
                        dpToPx(12),
                        dpToPx(6)
                    )
                    textSize = 13f
                    // Try to load Be Vietnam Pro regular font
                    try {
                        val typeface = resources.getFont(R.font.be_vietnam_pro_regular)
                        setTypeface(typeface)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                binding.cgAmenitiesDetail.addView(tv)
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun loadNearbyStores() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.allStores.collect { stores ->
                val currentDistrict = currentStore.quanHuyen.trim()
                
                // Filter other stores in same district
                val filtered = stores.filter {
                    it.id != currentStore.id && it.quanHuyen.equals(currentDistrict, ignoreCase = true)
                }
                
                nearbyStoresList = filtered
                nearbyAdapter.notifyDataSetChanged()

                val hasNearby = filtered.isNotEmpty()
                binding.rvNearbyStores.isVisible = hasNearby
                binding.tvNoNearbyStores.isVisible = !hasNearby
            }
        }
    }

    private fun openGoogleMaps(lat: Double, lng: Double) {
        try {
            val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
            startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể mở Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dialPhoneNumber(phones: String) {
        if (phones.isEmpty()) {
            Toast.makeText(requireContext(), "Cửa hàng chưa có số điện thoại liên hệ", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            // Take the first number if multiple separated by comma
            val firstNumber = phones.split(",").firstOrNull()?.trim() ?: phones
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$firstNumber"))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Không thể thực hiện cuộc gọi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Nearby stores adapter
    inner class NearbyStoresAdapter : RecyclerView.Adapter<NearbyStoresAdapter.ViewHolder>() {

        inner class ViewHolder(val itemBinding: ShopItemStoreBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ShopItemStoreBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val store = nearbyStoresList[position]
            val b = holder.itemBinding

            b.tvStoreName.text = store.tenCuaHang
            b.tvStoreHours.text = "Mở cửa từ ${store.moCua} đến ${store.dongCua}"
            b.tvStoreAddress.text = store.diaChiDayDu
            b.ivRadio.isVisible = false // Hidden for nearby stores

            // Click listener for details page
            b.root.setOnClickListener {
                val detailFragment = newInstance(store)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }

            b.btnDirections.setOnClickListener {
                openGoogleMaps(store.lat, store.lng)
            }
        }

        override fun getItemCount(): Int = nearbyStoresList.size
    }

    companion object {
        const val TAG = "ShopStoreDetailFragment"
        private const val ARG_STORE = "arg_store"

        fun newInstance(store: StoreEntity): ShopStoreDetailFragment {
            return ShopStoreDetailFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_STORE, store)
                }
            }
        }
    }
}
