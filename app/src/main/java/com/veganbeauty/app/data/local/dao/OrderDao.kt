package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId")
    fun getOrdersByUserId(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE isAffiliate = 1 AND aff_referrerUserId = :referrerUserId")
    fun getAffiliateOrdersByReferrer(referrerUserId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId OR (isGuest = 1 AND billingPhone = :phone)")
    fun getOrdersForUserOrGuest(userId: String, phone: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE userId = :userId")
    fun getOrdersForUser(userId: String): Flow<List<OrderEntity>>

    /**
     * Guest orders identified by the phone number typed at checkout.
     * Used so a returning guest with the same phone sees their prior
     * orders, while different phones do not see each other's orders.
     */
    @Query("SELECT * FROM orders WHERE isGuest = 1 AND billingPhone = :phone")
    fun getOrdersForGuestPhone(phone: String): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>): List<Long>

<<<<<<< HEAD
    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
=======
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
>>>>>>> 35f09837414391a9ba011bce61277d4577c69501
    suspend fun updateOrderStatus(orderId: String, status: String): Int

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderByIdFlow(orderId: String): Flow<OrderEntity?>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun getOrderCount(): Int

    @Query("UPDATE orders SET hasReview = :hasReview WHERE id = :orderId")
    suspend fun updateOrderReviewStatus(orderId: String, hasReview: Boolean): Int

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders(): Int
}
