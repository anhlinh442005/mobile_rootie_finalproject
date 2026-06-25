package com.veganbeauty.app.features.shop.home

import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.veganbeauty.app.core.base.RootieViewModel
import com.veganbeauty.app.data.repository.ProductRepository
import com.veganbeauty.app.features.shop.home.models.BannerUiModel
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopHomeViewModel(private val productRepository: ProductRepository) : RootieViewModel() {

    // Danh sách banner (Mock data tạm thời cho UI)
    private val _banners = MutableStateFlow<List<BannerUiModel>>(emptyList())
    val banners: StateFlow<List<BannerUiModel>> = _banners.asStateFlow()

    // Danh sách category (Mock data tạm thời cho UI)
    private val _categories = MutableStateFlow<List<CategoryUiModel>>(emptyList())
    val categories: StateFlow<List<CategoryUiModel>> = _categories.asStateFlow()

    // Danh sách sản phẩm gợi ý lấy từ Repository (sử dụng ProductEntity)
    val suggestedProducts = productRepository.allProducts.asLiveData()

    init {
        loadMockData()
        refreshProducts()
        observeProductCounts()
    }

    private fun loadMockData() {
        // Mock dữ liệu Banner (Sử dụng 3 ảnh organic đã gen)
        _banners.value = listOf(
            BannerUiModel("1", com.veganbeauty.app.R.drawable.shop_banner_1), 
            BannerUiModel("2", com.veganbeauty.app.R.drawable.shop_banner_2),
            BannerUiModel("3", com.veganbeauty.app.R.drawable.shop_banner_3)
        )

        // Mock dữ liệu Danh mục ban đầu trước khi load sản phẩm
        _categories.value = listOf(
            CategoryUiModel("c1", "Chăm sóc da", com.veganbeauty.app.R.drawable.ic_skincare, 0),
            CategoryUiModel("c2", "Tắm & Dưỡng thể", com.veganbeauty.app.R.drawable.ic_shower, 0),
            CategoryUiModel("c4", "Dưỡng môi", com.veganbeauty.app.R.drawable.ic_lips, 0),
            CategoryUiModel("c5", "Combo/Giftbox", com.veganbeauty.app.R.drawable.ic_combo, 0)
        )
    }

    private fun observeProductCounts() {
        viewModelScope.launch {
            productRepository.allProducts.collect { products ->
                val skincareIds = listOf(
                    "f5877af6a55f88bcf57c17b4", "389971929086b2ce7fba9dd0",
                    "36cbf3f5c4b7a299ce2a2d0c", "4e20d6bbc1203015ee2ecd48",
                    "b1b6cd208332d4f1e015a26c", "7667d982515426a9d88b787b",
                    "bb88a3306cf95af20d073594", "9882d5fa14c74dd053e17f33",
                    "c211afa24702f5d1ff86fe42"
                )
                val bodyIds = listOf(
                    "7c70e845e829b374e57ee7b1", "b703bb813e660aa88076ee5a",
                    "8fce5340c618672aa1ae7fb3"
                )
                val lipsIds = listOf("755731e01d8c579c633ae4d2", "ded17e0716783c133b1a5b9a")
                val comboIds = listOf(
                    "7176b5e7966be88daf95cfd4", "f40c1f05dcf4059f25fb89a1",
                    "e0754dabb88699e92481e123", "bd1c0ff76b19b1b5a3130a79"
                )

                var skincareCount = 0
                var bodyCount = 0
                var lipsCount = 0
                var comboCount = 0

                products.forEach { product ->
                    val productCategoryIds = product.categoryIds.split(",")
                    if (productCategoryIds.any { it in skincareIds }) skincareCount++
                    if (productCategoryIds.any { it in bodyIds }) bodyCount++
                    if (productCategoryIds.any { it in lipsIds }) lipsCount++
                    if (productCategoryIds.any { it in comboIds }) comboCount++
                }

                _categories.value = listOf(
                    CategoryUiModel("c1", "Chăm sóc da", com.veganbeauty.app.R.drawable.ic_skincare, skincareCount),
                    CategoryUiModel("c2", "Tắm & Dưỡng thể", com.veganbeauty.app.R.drawable.ic_shower, bodyCount),
                    CategoryUiModel("c4", "Dưỡng môi", com.veganbeauty.app.R.drawable.ic_lips, lipsCount),
                    CategoryUiModel("c5", "Combo/Giftbox", com.veganbeauty.app.R.drawable.ic_combo, comboCount)
                )
            }
        }
    }

    private fun refreshProducts() {
        viewModelScope.launch {
            try {
                productRepository.refreshProducts()
            } catch (e: Exception) {
                // Xử lý lỗi nếu cần
            }
        }
    }
}
