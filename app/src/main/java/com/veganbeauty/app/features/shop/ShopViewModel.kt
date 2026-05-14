package com.veganbeauty.app.features.shop

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ShopViewModel(private val repository: ProductRepository) : RootieViewModel() {

    // LiveData quan sát từ SQLite
    val products = repository.allProducts.asLiveData()

    init {
        refreshProducts()
    }

    fun refreshProducts() {
        viewModelScope.launch {
            try {
                repository.refreshProducts()
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
            }
        }
    }
}
