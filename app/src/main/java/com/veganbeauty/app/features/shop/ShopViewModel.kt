package com.veganbeauty.app.features.shop

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.repository.ProductRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

class ShopViewModel(private val repository: ProductRepository) : RootieViewModel() {

    private val _categoryFilter = MutableStateFlow<String?>(null)
    private val _subcategoryFilter = MutableStateFlow<String?>("Tất cả")
    
    private val _subcategories = MutableStateFlow<List<String>>(listOf("Tất cả"))
    val subcategories = _subcategories.asLiveData()

    private val subcategoryToIdMap = mapOf(
        "Sữa rửa mặt" to "f5877af6a55f88bcf57c17b4",
        "Tẩy trang" to "389971929086b2ce7fba9dd0",
        "Chống nắng" to "36cbf3f5c4b7a299ce2a2d0c",
        "Nước cân bằng" to "4e20d6bbc1203015ee2ecd48",
        "Tinh chất" to "b1b6cd208332d4f1e015a26c",
        "Mặt nạ" to "7667d982515426a9d88b787b",
        "Kem dưỡng" to "bb88a3306cf95af20d073594",
        "Xịt khoáng" to "9882d5fa14c74dd053e17f33",
        "Tẩy da chết mặt" to "c211afa24702f5d1ff86fe42",
        
        "Sữa tắm" to "7c70e845e829b374e57ee7b1",
        "Tẩy da chết cơ thể" to "b703bb813e660aa88076ee5a",
        "Dưỡng thể" to "8fce5340c618672aa1ae7fb3",
        
        "Chăm sóc tóc" to "24a75aa9d541feed638b1970",
        
        "Tẩy da chết môi" to "755731e01d8c579c633ae4d2",
        "Dưỡng ẩm môi" to "ded17e0716783c133b1a5b9a",
        
        "Chăm sóc da mặt" to "7176b5e7966be88daf95cfd4",
        "Chăm sóc cơ thể" to "f40c1f05dcf4059f25fb89a1",
        "Chăm sóc mái tóc" to "e0754dabb88699e92481e123",
        "Chăm sóc môi" to "bd1c0ff76b19b1b5a3130a79"
    )

    val products = combine(
        repository.allProducts,
        _categoryFilter,
        _subcategoryFilter
    ) { list, category, subcategory ->
        var filteredList = list
        if (category != null && category != "Tất cả") {
            val targetIds = when (category) {
                "Chăm sóc da" -> listOf(
                    "f5877af6a55f88bcf57c17b4", "389971929086b2ce7fba9dd0",
                    "36cbf3f5c4b7a299ce2a2d0c", "4e20d6bbc1203015ee2ecd48",
                    "b1b6cd208332d4f1e015a26c", "7667d982515426a9d88b787b",
                    "bb88a3306cf95af20d073594", "9882d5fa14c74dd053e17f33",
                    "c211afa24702f5d1ff86fe42"
                )
                "Tắm & Dưỡng thể" -> listOf(
                    "7c70e845e829b374e57ee7b1", "b703bb813e660aa88076ee5a",
                    "8fce5340c618672aa1ae7fb3"
                )
                "Chăm sóc tóc" -> listOf("24a75aa9d541feed638b1970")
                "Dưỡng môi" -> listOf("755731e01d8c579c633ae4d2", "ded17e0716783c133b1a5b9a")
                "Combo/Giftbox" -> listOf(
                    "7176b5e7966be88daf95cfd4", "f40c1f05dcf4059f25fb89a1",
                    "e0754dabb88699e92481e123", "bd1c0ff76b19b1b5a3130a79"
                )
                else -> emptyList()
            }

            filteredList = filteredList.filter { product ->
                val productCategoryIds = product.categoryIds.split(",")
                productCategoryIds.any { id -> id in targetIds }
            }
        }
        
        if (subcategory != null && subcategory != "Tất cả") {
            val subcategoryId = subcategoryToIdMap[subcategory]
            if (subcategoryId != null) {
                filteredList = filteredList.filter { product ->
                    val productCategoryIds = product.categoryIds.split(",")
                    productCategoryIds.contains(subcategoryId)
                }
            } else {
                filteredList = filteredList.filter { it.name.contains(subcategory, ignoreCase = true) || it.description.contains(subcategory, ignoreCase = true) }
            }
        }
        
        filteredList
    }.asLiveData()

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
        _subcategoryFilter.value = "Tất cả"
        updateSubcategories(category ?: "Tất cả")
    }

    fun setSubcategoryFilter(subcategory: String) {
        _subcategoryFilter.value = subcategory
    }

    private fun updateSubcategories(category: String) {
        val list = mutableListOf("Tất cả")
        when (category) {
            "Chăm sóc da" -> list.addAll(listOf("Chống nắng", "Tẩy trang", "Sữa rửa mặt", "Tẩy da chết mặt", "Mặt nạ", "Nước cân bằng", "Tinh chất", "Kem dưỡng", "Xịt khoáng"))
            "Tắm & Dưỡng thể" -> list.addAll(listOf("Tẩy da chết cơ thể", "Sữa tắm", "Dưỡng thể"))
            "Dưỡng môi" -> list.addAll(listOf("Tẩy da chết môi", "Dưỡng ẩm môi"))
            "Combo/Giftbox" -> list.addAll(listOf("Chăm sóc da mặt", "Chăm sóc cơ thể", "Chăm sóc mái tóc", "Chăm sóc môi"))
        }
        _subcategories.value = list
    }

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

