package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.veganbeauty.app.data.local.dao.UserProductExpiryDao
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity

import com.veganbeauty.app.data.local.LocalJsonReader

class ProductRepository @JvmOverloads constructor(
    private val productDao: ProductDao,
    private val localJsonReader: LocalJsonReader,
    private val firestoreService: FirestoreService = FirestoreService(),
    private val userProductExpiryDao: UserProductExpiryDao? = null
) {
    // Lấy dữ liệu từ SQLite (Real-time updates)
    val allProducts: Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getExpiryProductsForUser(userId: String): Flow<List<ProductEntity>> {
        val dao = userProductExpiryDao ?: throw IllegalStateException("UserProductExpiryDao must be provided")
        return dao.getProductsByUserIdFlow(userId).map { list ->
            list.map { it.toProductEntity() }
        }
    }

    suspend fun getExpiryProductById(userId: String, productId: String): ProductEntity? {
        val dao = userProductExpiryDao ?: throw IllegalStateException("UserProductExpiryDao must be provided")
        return dao.getProductById(userId, productId)?.toProductEntity()
    }

    suspend fun deleteExpiryProduct(userId: String, productId: String) {
        val dao = userProductExpiryDao ?: throw IllegalStateException("UserProductExpiryDao must be provided")
        dao.deleteUserProductExpiry(userId, productId)
    }

    suspend fun seedExpiryProductsIfEmpty(userId: String) {
        val dao = userProductExpiryDao ?: return
        if (dao.getProductCountByUserId(userId) == 0) {
            val mockList = listOf(
                UserProductExpiryEntity(
                    userId = userId,
                    productId = "0b8fadbc1bd44562f75704e6",
                    name = "Nước tẩy trang sen Hậu Giang 500ml",
                    mainImage = "https://image.cocoonvietnam.com/uploads/Avatar_Website_Nuoc_tay_trang_sen_Artboard_7_copy_ac0bf66b46.jpg",
                    brand = "Cocoon Vietnam",
                    price = 309000,
                    category = "Chăm sóc da",
                    sku = "101106",
                    stock = 555,
                    expiryDate = "10/06/2026"
                ),
                UserProductExpiryEntity(
                    userId = userId,
                    productId = "dd23909f6a123054c8cf62f4",
                    name = "Dầu tẩy trang hoa hồng 310ml",
                    mainImage = "https://image.cocoonvietnam.com/uploads/Template_Website_Dau_Tay_Trang_310ml_b098c76143.jpg",
                    brand = "Cocoon Vietnam",
                    price = 339000,
                    category = "Chăm sóc da",
                    sku = "101110",
                    stock = 962,
                    expiryDate = "04/07/2026"
                ),
                UserProductExpiryEntity(
                    userId = userId,
                    productId = "dc312be5eec4d740ae26acbb",
                    name = "Nước tẩy trang bí đao 500ml",
                    mainImage = "https://image.cocoonvietnam.com/uploads/z4394607766854_2aca12462b79836bb49c3bf9aeef6bd1_1_fe9bcfe8db.jpg",
                    brand = "Cocoon Vietnam",
                    price = 299000,
                    category = "Chăm sóc da",
                    sku = "101108",
                    stock = 999,
                    expiryDate = "15/09/2026"
                ),
                UserProductExpiryEntity(
                    userId = userId,
                    productId = "812ea7faa15bac41e52d4170",
                    name = "Sữa rửa mặt sen Hậu Giang 310ml",
                    mainImage = "https://image.cocoonvietnam.com/uploads/Avatar_1_dcfa1bbf4e.jpg",
                    brand = "Cocoon Vietnam",
                    price = 339000,
                    category = "Chăm sóc da",
                    sku = "101114",
                    stock = 479,
                    expiryDate = "20/05/2026"
                )
            )
            dao.insertUserProducts(mockList)
        }
    }

    suspend fun getProductByBarcode(barcode: String): ProductEntity? {
        val normalized = barcode.trim()
        if (normalized.isEmpty()) return null

        productDao.getProductByBarcode(normalized)?.let { return it }

        return try {
            val remote = firestoreService.fetchProductByBarcode(normalized)
            if (remote != null) {
                productDao.insertProducts(listOf(remote))
            }
            remote
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    companion object {
        private var lastSyncTime = 0L
        private const val CACHE_DURATION = 5 * 60 * 1000L // 5 minutes
    }

    // Làm mới dữ liệu từ Firebase Firestore (có fallback về dữ liệu local assets)
    suspend fun refreshProducts(force: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        var count = productDao.getProductCount()
        
        // Cải tiến: Nếu SQLite đang trống hoàn toàn, đọc nhanh từ assets và đổ vào SQLite trước để UI hiện sản phẩm ngay lập tức
        if (count == 0) {
            try {
                val localProducts = localJsonReader.getAllProducts()
                if (localProducts.isNotEmpty()) {
                    productDao.insertProducts(localProducts)
                    count = productDao.getProductCount()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        
        if (!force && count > 0 && (currentTime - lastSyncTime < CACHE_DURATION)) {
            // Dữ liệu đã được cache và local DB không rỗng, bỏ qua đồng bộ từ Firestore
            return
        }

        try {
            val remoteProducts = firestoreService.fetchAllProducts()
            if (remoteProducts.isNotEmpty()) {
                productDao.insertProducts(remoteProducts)
                lastSyncTime = currentTime
            } else {
                // Fallback to local JSON if Firestore returns empty
                val localProducts = localJsonReader.getAllProducts()
                if (localProducts.isNotEmpty()) {
                    productDao.insertProducts(localProducts)
                    lastSyncTime = currentTime
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
