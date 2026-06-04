package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>): List<Long>

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String): Int

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE orderId = :orderId")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("UPDATE orders SET hasReview = :hasReview, reviewStars = :stars, reviewText = :text, reviewImage = :image, isAnonymous = :isAnonymous, recommendToFriends = :recommend WHERE orderId = :orderId")
    suspend fun updateOrderReview(
        orderId: String,
        hasReview: Boolean,
        stars: Int,
        text: String?,
        image: String?,
        isAnonymous: Boolean,
        recommend: Boolean
    ): Int

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders(): Int
}
