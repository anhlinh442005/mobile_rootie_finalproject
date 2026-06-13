package com.veganbeauty.app.features.shop.store

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.entities.StoreEntity
import com.veganbeauty.app.databinding.ShopFragmentStoresBottomSheetBinding
import com.veganbeauty.app.databinding.ShopItemStoreBinding

class ShopStoresBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: ShopFragmentStoresBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var storesList = ArrayList<StoreEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            storesList = it.getSerializable(ARG_STORES_LIST) as? ArrayList<StoreEntity> ?: ArrayList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ShopFragmentStoresBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.tvSheetTitle.text = "Tìm thấy ${storesList.size} cửa hàng gần bạn"
        
        binding.rvStoresList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStoresList.adapter = StoresAdapter(storesList)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class StoresAdapter(private val items: List<StoreEntity>) :
        RecyclerView.Adapter<StoresAdapter.ViewHolder>() {

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
            val store = items[position]
            val b = holder.itemBinding

            b.tvStoreName.text = store.tenCuaHang
            b.tvStoreHours.text = "Mở cửa từ ${store.moCua} đến ${store.dongCua}"
            b.tvStoreAddress.text = store.diaChiDayDu
            b.ivRadio.isVisible = false // Selection radio not needed in map overview list

            b.btnDirections.setOnClickListener {
                openGoogleMaps(store.lat, store.lng)
            }

            b.root.setOnClickListener {
                dismiss()
                val detailFragment = ShopStoreDetailFragment.newInstance(store)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "ShopStoresBottomSheetFragment"
        private const val ARG_STORES_LIST = "stores_list"

        fun newInstance(stores: ArrayList<StoreEntity>): ShopStoresBottomSheetFragment {
            return ShopStoresBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_STORES_LIST, stores)
                }
            }
        }
    }
}
