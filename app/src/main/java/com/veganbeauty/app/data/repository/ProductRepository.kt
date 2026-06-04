package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

class ProductRepository(
    private val productDao: ProductDao,
    private val localJsonReader: LocalJsonReader
) {
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

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
