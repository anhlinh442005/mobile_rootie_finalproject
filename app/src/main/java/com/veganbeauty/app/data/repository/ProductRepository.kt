package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

import com.veganbeauty.app.data.local.LocalJsonReader

class ProductRepository(
    private val productDao: ProductDao,
    private val localJsonReader: LocalJsonReader
) {
    // Lấy dữ liệu từ SQLite (Real-time updates)
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    // Làm mới dữ liệu từ file Local (Thay vì Firebase)
    suspend fun refreshProducts() {
        try {
            val localProducts = localJsonReader.getAllProducts()
            if (localProducts.isNotEmpty()) {
                productDao.insertProducts(localProducts)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
