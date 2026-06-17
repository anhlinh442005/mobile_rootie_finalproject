package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.ProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): kotlinx.coroutines.flow.Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?
}
