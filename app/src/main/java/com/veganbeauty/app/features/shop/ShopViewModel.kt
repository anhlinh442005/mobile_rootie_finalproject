package com.veganbeauty.app.features.shop

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.repository.ProductRepository

class ShopViewModel(private val repository: ProductRepository) : ViewModel() {
    val products: LiveData<List<ProductEntity>> = repository.allProducts.asLiveData()

    fun setProduct(product: ProductEntity) {
        // Implementation
    }
}
