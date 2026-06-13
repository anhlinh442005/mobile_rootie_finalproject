package com.veganbeauty.app.features.shop.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.veganbeauty.app.data.local.entities.ProductEntity

class ShopSearchViewModel : ViewModel() {

    private val _hotDeals = MutableLiveData<List<ProductEntity>>()
    val hotDeals: LiveData<List<ProductEntity>> = _hotDeals

    init {
        loadMockHotDeals()
    }

    private fun loadMockHotDeals() {
        val mockData = listOf(
            ProductEntity("1", "Gel tắm đường thốt nốt An Giang 500ml", "SKU01", 241000, null, "Sữa tắm", "", 100, "", "https://picsum.photos/200/300", "Mọi loại da", "An Giang", "2025", true),
            ProductEntity("2", "Gel tắm đường thốt nốt An Giang 500ml", "SKU02", 241000, null, "Sữa tắm", "", 100, "", "https://picsum.photos/200/301", "Mọi loại da", "An Giang", "2025", false),
            ProductEntity("3", "Gel tắm đường thốt nốt An Giang 500ml", "SKU03", 241000, null, "Sữa tắm", "", 100, "", "https://picsum.photos/200/302", "Mọi loại da", "An Giang", "2025", false)
        )
        _hotDeals.value = mockData
    }
}
