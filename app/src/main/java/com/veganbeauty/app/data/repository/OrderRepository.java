package com.veganbeauty.app.data.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.dao.RewardPointDao;
import com.veganbeauty.app.data.local.dao.UserGiftDao;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderItem;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

public class OrderRepository {

    private final OrderDao orderDao;
    private final RewardPointDao rewardPointDao;
    private final UserGiftDao userGiftDao;
    private final LocalJsonReader localJsonReader;

    private static ListenerRegistration orderListener = null;

    public OrderRepository(
            OrderDao orderDao,
            RewardPointDao rewardPointDao,
            UserGiftDao userGiftDao,
            LocalJsonReader localJsonReader
    ) {
        this.orderDao = orderDao;
        this.rewardPointDao = rewardPointDao;
        this.userGiftDao = userGiftDao;
        this.localJsonReader = localJsonReader;
    }

    public Flow<List<OrderEntity>> getAllOrders() {
        return orderDao.getAllOrders();
    }

    public Flow<List<OrderEntity>> getBuyerOrders(String userId) {
        return orderDao.getOrdersByUserId(userId);
    }

    public Flow<List<OrderEntity>> getAffiliateOrders(String referrerUserId) {
        return orderDao.getAffiliateOrdersByReferrer(referrerUserId);
    }

    public void startListeningToOrders(String userId, String phone) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        com.google.firebase.firestore.Query query = null;

        if (!safeUserId.isEmpty()) {
            query = db.collection("orders").whereEqualTo("userId", safeUserId);
        } else if (!safePhone.isEmpty()) {
            query = db.collection("orders").whereEqualTo("billingPhone", safePhone);
        }

        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }

        if (query != null) {
            orderListener = query.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e("OrderRepository", "Listen failed.", e);
                    return;
                }
                if (snapshot != null) {
                    List<OrderEntity> orders = new ArrayList<>();
                    Gson gson = new Gson();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        try {
                            java.util.Map<String, Object> map = doc.getData();
                            if (map == null) continue;
                            JsonElement jsonTree = gson.toJsonTree(map);
                            OrderEntity order = gson.fromJson(jsonTree, OrderEntity.class);
                            orders.add(order);
                        } catch (Exception ex) {
                            Log.e("OrderRepository", "Error parsing order", ex);
                        }
                    }
                    if (!orders.isEmpty()) {
                        GlobalScope.INSTANCE.launch(Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (scope, cont) -> {
                            orderDao.insertOrders(orders, cont);
                            return kotlin.Unit.INSTANCE;
                        });
                    }
                }
            });
        }
    }

    public Flow<List<OrderEntity>> getOrdersForBuyer(String userId, String phone) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        startListeningToOrders(safeUserId, safePhone);
        if (!safeUserId.isEmpty()) {
            return orderDao.getOrdersForUser(safeUserId);
        } else if (!safePhone.isEmpty()) {
            return orderDao.getOrdersForGuestPhone(safePhone);
        } else {
            return orderDao.getAllOrders();
        }
    }

    public Object refreshOrders(String userId, String phone, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            try {
                orderDao.deleteAllOrders(cont);
                List<OrderEntity> mockOrders = localJsonReader.getAllOrders();
                if (!mockOrders.isEmpty()) {
                    orderDao.insertOrders(mockOrders, cont);
                }

                Integer totalPoints = (Integer) FlowKt.first(rewardPointDao.getTotalPointsFlow(), cont);
                if (totalPoints == null) {
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "SYSTEM_INITIAL", 11200, "Tích điểm mua sắm", 1767261600000L
                    ), cont);
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_1", -2000, "Đổi quà: Sữa rửa mặt mini", 1772043600000L
                    ), cont);
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_2", -200, "Đổi quà: Freeship Đơn 0Đ", 1772698500000L
                    ), cont);
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_3", -500, "Đổi quà: Voucher Giảm 50K", 1775831400000L
                    ), cont);
                }

                if (userGiftDao.getUserGiftCount() == 0) {
                    List<UserGiftEntity> initGifts = Arrays.asList(
                            new UserGiftEntity(0, "voucher_50k", "Voucher Giảm 50K", "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.", 500, "2026-12-30 23:59:59", "Còn hạn", "voucher_discount", "SAVE50K", 300000, "Chăm Sóc Da Mặt", "fixed_amount", null, 50000, 1775831400000L),
                            new UserGiftEntity(0, "gift_cleanser", "Quà tặng: Sữa rửa mặt", "Nhận miễn phí 1 tuýp sữa rửa mặt bí đao mini 15ml.", 1000, "2026-12-15 23:59:59", "Còn hạn", "product", "FREECLN", 0, "Sữa rửa mặt", "product_gift", "p003", 0, 1772043600000L),
                            new UserGiftEntity(0, "gift_freeship", "Freeship Đơn 0Đ", "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng.", 200, "2026-06-11 23:59:59", "Hôm nay", "voucher_freeship", "FREESHIP", 150000, "Tất cả sản phẩm", "percentage", null, 100, 1772698500000L),
                            new UserGiftEntity(0, "voucher_10_percent", "Giảm 10% Cho Sản Phẩm Bưởi", "Áp dụng cho dòng sản phẩm tinh chất vỏ bưởi dưỡng tóc.", 300, "2026-10-31 23:59:59", "Hết hạn", "voucher_discount", "GRAPE10", 0, "Tinh chất bưởi", "percentage", null, 10, 1767261600000L)
                    );
                    userGiftDao.insertUserGifts(initGifts, cont);
                }

                String safeUserId = userId != null ? userId.trim() : "";
                String safePhone = phone != null ? phone.trim() : "";
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                com.google.firebase.firestore.Query query = null;

                if (!safeUserId.isEmpty()) {
                    query = db.collection("orders").whereEqualTo("userId", safeUserId);
                } else if (!safePhone.isEmpty()) {
                    query = db.collection("orders").whereEqualTo("billingPhone", safePhone);
                }

                if (query != null) {
                    query.get().addOnSuccessListener(snapshot -> {
                        if (snapshot != null && !snapshot.isEmpty()) {
                            List<OrderEntity> orders = new ArrayList<>();
                            Gson gson = new Gson();
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                try {
                                    java.util.Map<String, Object> map = doc.getData();
                                    if (map == null) continue;
                                    JsonElement jsonTree = gson.toJsonTree(map);
                                    OrderEntity order = gson.fromJson(jsonTree, OrderEntity.class);
                                    orders.add(order);
                                } catch (Exception ex) {
                                    Log.e("OrderRepository", "Error parsing order on refresh", ex);
                                }
                            }
                            if (!orders.isEmpty()) {
                                GlobalScope.INSTANCE.launch(Dispatchers.getIO(), kotlinx.coroutines.CoroutineStart.DEFAULT, (scope2, cont2) -> {
                                    orderDao.insertOrders(orders, cont2);
                                    return kotlin.Unit.INSTANCE;
                                });
                            }
                        }
                    }).addOnFailureListener(e -> Log.e("OrderRepository", "Failed to fetch orders on refresh", e));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    public Object redeemGift(
            String giftId,
            String title,
            String description,
            int cost,
            String expiryDate,
            String code,
            String giftType,
            int minOrderValue,
            String applicableProducts,
            String offerType,
            String productId,
            int discountValue,
            kotlin.coroutines.Continuation<? super Boolean> continuation
    ) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            Integer total = (Integer) FlowKt.first(rewardPointDao.getTotalPointsFlow(), cont);
            if (total == null) total = 0;
            if (total >= cost) {
                rewardPointDao.insertRewardPoints(new RewardPointEntity(
                        0, "REDEEM_" + giftId, -cost, "Đổi quà: " + title, System.currentTimeMillis()
                ), cont);
                SyncDataHelper.INSTANCE.syncRewardPointsToFirestore(localJsonReader.getContext());
                userGiftDao.insertUserGift(new UserGiftEntity(
                        0, giftId, title, description, cost, expiryDate, "Còn hạn", giftType, code,
                        minOrderValue, applicableProducts, offerType, productId, discountValue, System.currentTimeMillis()
                ), cont);
                return true;
            }
            return false;
        }, continuation);
    }

    public Flow<List<UserGiftEntity>> getAllUserGifts() {
        return userGiftDao.getAllUserGiftsFlow();
    }

    public Object deleteUserGiftById(int id, kotlin.coroutines.Continuation<? super Boolean> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            return userGiftDao.deleteUserGiftById(id, cont) > 0;
        }, continuation);
    }

    public Object updateOrderReview(
            String orderId,
            int stars,
            String text,
            String image,
            boolean isAnonymous,
            boolean recommend,
            kotlin.coroutines.Continuation<? super Boolean> continuation
    ) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            int wordCount = 0;
            if (text != null && !text.trim().isEmpty()) {
                String[] words = text.trim().split("\\s+");
                wordCount = words.length;
            }
            boolean hasText = text != null && !text.trim().isEmpty() && wordCount <= 200;
            boolean hasImage = image != null && !image.isEmpty();
            boolean qualifiesForCoins = stars > 0 && hasText && hasImage;

            orderDao.updateOrderReviewStatus(orderId, true, cont);

            if (qualifiesForCoins) {
                Boolean alreadyAwarded = (Boolean) rewardPointDao.hasReceivedPointsForOrder(orderId, cont);
                if (!alreadyAwarded) {
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, orderId, 200, "Đánh giá đơn hàng " + orderId + " kèm hình ảnh", System.currentTimeMillis()
                    ), cont);
                    SyncDataHelper.INSTANCE.syncRewardPointsToFirestore(localJsonReader.getContext());
                    return true;
                }
            }
            return false;
        }, continuation);
    }

    public Object cancelOrder(String orderId, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            orderDao.updateOrderStatus(orderId, "Đã hủy", cont);
            OrderStatusNotifier.INSTANCE.simulateOnly(orderDao, orderId, "Đã hủy", cont);
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    public Flow<OrderEntity> getOrderById(String orderId) {
        return orderDao.getOrderByIdFlow(orderId);
    }

    public Object updateOrderStatus(String orderId, String status, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            orderDao.updateOrderStatus(orderId, status, cont);
            OrderStatusNotifier.INSTANCE.simulateOnly(orderDao, orderId, status, cont);
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    public Object ensureOrderExists(String orderId, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return BuildersKt.withContext(Dispatchers.getIO(), (scope, cont) -> {
            OrderEntity existing = (OrderEntity) orderDao.getOrderById(orderId, cont);
            if (existing == null) {
                List<OrderItem> items = new ArrayList<>();
                items.add(new OrderItem("product_rose_cream", "Kem dưỡng hoa hồng Cocoon 50ml", "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/rose_cream_u2kgwf.png", 1, 222000L));
                
                OrderEntity mockOrder = new OrderEntity(
                        orderId, "16/06/2026", "15:30", "Đang giao", 202000L, 222000L,
                        items, "test_001", false, "Nguyễn Khánh Xuân", "090 123 4567",
                        "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh",
                        30000L, 50000L, "Thanh toán khi nhận hàng (COD)", false
                );
                orderDao.insertOrder(mockOrder, cont);
            }
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }
}
