package com.veganbeauty.app.data.local.dao

import com.veganbeauty.app.data.local.entities.ProductEntity

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>): List<Long>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductById(productId: String): ProductEntity?
}
