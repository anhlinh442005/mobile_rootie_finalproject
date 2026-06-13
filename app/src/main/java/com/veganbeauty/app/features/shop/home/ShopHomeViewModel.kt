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
    }

    private fun loadMockData() {
        // Mock dữ liệu Banner (Sử dụng 3 ảnh organic đã gen)
        _banners.value = listOf(
            BannerUiModel("1", com.veganbeauty.app.R.drawable.shop_banner_1), 
            BannerUiModel("2", com.veganbeauty.app.R.drawable.shop_banner_2),
            BannerUiModel("3", com.veganbeauty.app.R.drawable.shop_banner_3)
        )

        // Mock dữ liệu Danh mục (Dựa theo Figma)
        _categories.value = listOf(
            CategoryUiModel("c1", "Chăm sóc da", com.veganbeauty.app.R.drawable.ic_skincare, 46),
            CategoryUiModel("c2", "Tắm & Dưỡng thể", com.veganbeauty.app.R.drawable.ic_shower, 7),
            CategoryUiModel("c4", "Dưỡng môi", com.veganbeauty.app.R.drawable.ic_lips, 3),
            CategoryUiModel("c5", "Combo/Giftbox", com.veganbeauty.app.R.drawable.ic_combo, 14)
        )
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
