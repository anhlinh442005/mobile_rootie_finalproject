package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

import com.veganbeauty.app.data.local.LocalJsonReader

class ProductRepository(
    private val productDao: ProductDao,
    private val localJsonReader: LocalJsonReader,
    private val firestoreService: FirestoreService = FirestoreService()
) {
    // Lấy dữ liệu từ SQLite (Real-time updates)
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    // Làm mới dữ liệu từ Firebase Firestore (có fallback về dữ liệu local assets)
    suspend fun refreshProducts() {
        try {
            val remoteProducts = firestoreService.fetchAllProducts()
            if (remoteProducts.isNotEmpty()) {
                productDao.insertProducts(remoteProducts)
            } else {
                // Fallback to local JSON if Firestore returns empty
                val localProducts = localJsonReader.getAllProducts()
                if (localProducts.isNotEmpty()) {
                    productDao.insertProducts(localProducts)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to local JSON on exception
            try {
                val localProducts = localJsonReader.getAllProducts()
                if (localProducts.isNotEmpty()) {
                    productDao.insertProducts(localProducts)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
