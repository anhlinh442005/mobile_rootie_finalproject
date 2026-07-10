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

    @Query("SELECT * FROM orders WHERE userId = :userId OR shippingPhone = :phone OR REPLACE(shippingPhone, ' ', '') = REPLACE(:phone, ' ', '')")
    Flow<List<OrderEntity>> getOrdersForBuyerIdentity(String userId, String phone);

    @Query("SELECT * FROM orders WHERE isGuest = 1 AND billingPhone = :phone")
    Flow<List<OrderEntity>> getOrdersForGuestPhone(String phone);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertOrders(List<OrderEntity> orders);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertOrder(OrderEntity order);

    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    int updateOrderStatus(String orderId, String status);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM orders WHERE id = :orderId")
    OrderEntity getOrderById(String orderId);

    @Query("SELECT * FROM orders WHERE id = :orderId")
    Flow<List<OrderEntity>> getOrderByIdFlow(String orderId);

    @Query("SELECT COUNT(*) FROM orders")
    int getOrderCount();

    @Query("UPDATE orders SET hasReview = :hasReview WHERE id = :orderId")
    int updateOrderReviewStatus(String orderId, boolean hasReview);

    @Query("DELETE FROM orders")
    int deleteAllOrders();

    // Sync versions
    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    int updateOrderStatusSync(String orderId, String status);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM orders WHERE id = :orderId")
    OrderEntity getOrderByIdSync(String orderId);

    @Query("SELECT * FROM orders WHERE userId = :userId AND status = 'Chờ xác nhận'")
    List<OrderEntity> getPendingOrdersForUserSync(String userId);

    @Query("SELECT * FROM orders WHERE userId = :userId OR shippingPhone = :phone OR REPLACE(shippingPhone, ' ', '') = REPLACE(:phone, ' ', '')")
    List<OrderEntity> getOrdersForBuyerIdentitySync(String userId, String phone);

    @Query("DELETE FROM orders WHERE userId = :userId OR shippingPhone = :phone OR REPLACE(shippingPhone, ' ', '') = REPLACE(:phone, ' ', '')")
    void deleteByUserIdentity(String userId, String phone);
}
