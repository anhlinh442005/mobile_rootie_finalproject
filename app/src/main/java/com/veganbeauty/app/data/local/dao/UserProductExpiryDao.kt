package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProductExpiryDao {
    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId")
    fun getProductsByUserIdFlow(userId: String): Flow<List<UserProductExpiryEntity>>

    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId")
    suspend fun getProductsByUserId(userId: String): List<UserProductExpiryEntity>

    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId AND productId = :productId LIMIT 1")
    suspend fun getProductById(userId: String, productId: String): UserProductExpiryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProducts(products: List<UserProductExpiryEntity>)

    @Query("SELECT COUNT(*) FROM user_product_expiry WHERE userId = :userId")
    suspend fun getProductCountByUserId(userId: String): Int

    @Query("DELETE FROM user_product_expiry WHERE userId = :userId AND productId = :productId")
    suspend fun deleteUserProductExpiry(userId: String, productId: String)
}
