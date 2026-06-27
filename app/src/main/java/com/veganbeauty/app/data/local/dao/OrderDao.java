package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.OrderEntity;

import java.util.List;

import kotlinx.coroutines.flow.Flow;

@Dao
public interface OrderDao {
    @Query("SELECT * FROM orders")
    Flow<List<OrderEntity>> getAllOrders();

    @Query("SELECT * FROM orders WHERE userId = :userId")
    Flow<List<OrderEntity>> getOrdersByUserId(String userId);

    @Query("SELECT * FROM orders WHERE isAffiliate = 1 AND aff_referrerUserId = :referrerUserId")
    Flow<List<OrderEntity>> getAffiliateOrdersByReferrer(String referrerUserId);

    @Query("SELECT * FROM orders WHERE status = :status")
    Flow<List<OrderEntity>> getOrdersByStatus(String status);

    @Query("SELECT * FROM orders WHERE userId = :userId OR (isGuest = 1 AND billingPhone = :phone)")
    Flow<List<OrderEntity>> getOrdersForUserOrGuest(String userId, String phone);

    @Query("SELECT * FROM orders WHERE userId = :userId")
    Flow<List<OrderEntity>> getOrdersForUser(String userId);

    @Query("SELECT * FROM orders WHERE isGuest = 1 AND billingPhone = :phone")
    Flow<List<OrderEntity>> getOrdersForGuestPhone(String phone);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertOrders(List<OrderEntity> orders, kotlin.coroutines.Continuation<? super List<Long>> continuation);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Object insertOrder(OrderEntity order, kotlin.coroutines.Continuation<? super Long> continuation);

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    Object updateOrderStatus(String orderId, String status, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("SELECT * FROM orders WHERE id = :orderId")
    Object getOrderById(String orderId, kotlin.coroutines.Continuation<? super OrderEntity> continuation);

    @Query("SELECT * FROM orders WHERE id = :orderId")
    Flow<OrderEntity> getOrderByIdFlow(String orderId);

    @Query("SELECT COUNT(*) FROM orders")
    Object getOrderCount(kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("UPDATE orders SET hasReview = :hasReview WHERE id = :orderId")
    Object updateOrderReviewStatus(String orderId, boolean hasReview, kotlin.coroutines.Continuation<? super Integer> continuation);

    @Query("DELETE FROM orders")
    Object deleteAllOrders(kotlin.coroutines.Continuation<? super Integer> continuation);
}
