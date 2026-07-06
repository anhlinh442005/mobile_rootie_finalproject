package com.veganbeauty.app.data.repository;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
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
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.repository.OrderStatusNotifier;
import com.veganbeauty.app.utils.SyncDataHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

import kotlinx.coroutines.flow.Flow;

public class OrderRepository {

    private static final String TAG = "OrderRepository";

    private static volatile int listenerGeneration = 0;

    private final OrderDao orderDao;
    private final RewardPointDao rewardPointDao;
    private final UserGiftDao userGiftDao;
    private final LocalJsonReader localJsonReader;

    private static ListenerRegistration orderListener = null;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

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

        attachOrderListener(query);
    }

    public void stopListeningToOrders() {
        listenerGeneration++;
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
            Log.d(TAG, "Orders listener removed");
        }
    }

    private void attachOrderListener(@Nullable com.google.firebase.firestore.Query query) {
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }

        if (query != null) {
            orderListener = query.addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    return;
                }
                if (snapshot == null) {
                    return;
                }
                ioExecutor.execute(() -> handleOrderSnapshot(snapshot));
            });
        }
    }

    private void handleOrderSnapshot(com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<OrderEntity> orders = new ArrayList<>();
        Gson gson = new Gson();
        android.content.Context appContext = localJsonReader.getAppContext();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                java.util.Map<String, Object> map = doc.getData();
                if (map == null) continue;
                JsonElement jsonTree = gson.toJsonTree(map);
                OrderEntity order = gson.fromJson(jsonTree, OrderEntity.class);
                if (order == null || order.getId() == null) continue;
                enrichCreatedAt(order, doc);
                normalizeOrderForRoom(order);

                OrderEntity existing = orderDao.getOrderByIdSync(order.getId());
                String previousStatus = existing != null ? existing.getStatus() : null;
                String newStatus = order.getStatus();
                orders.add(order);

                if (existing != null
                        && newStatus != null
                        && !newStatus.equals(previousStatus)) {
                    OrderStatusNotifier.notifyIfStatusChanged(
                            appContext,
                            order,
                            previousStatus,
                            newStatus
                    );
                }
            } catch (Exception ex) {
                Log.e("OrderRepository", "Error parsing order", ex);
            }
        }
        if (!orders.isEmpty()) {
            try {
                orderDao.insertOrders(orders);
            } catch (Exception ex) {
                Log.e("OrderRepository", "Error inserting orders", ex);
            }
        }
    }

    public Flow<List<OrderEntity>> getOrdersForBuyer(String userId, String phone) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        startListeningToOrders(safeUserId, safePhone);
        return observeOrdersForBuyer(safeUserId, safePhone);
    }

    public Flow<List<OrderEntity>> observeOrdersForBuyer(String userId, String phone) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        if (!safeUserId.isEmpty()) {
            if (!safePhone.isEmpty()) {
                return orderDao.getOrdersForBuyerIdentity(safeUserId, safePhone);
            }
            return orderDao.getOrdersForUser(safeUserId);
        } else if (!safePhone.isEmpty()) {
            return orderDao.getOrdersForGuestPhone(safePhone);
        } else {
            return orderDao.getOrdersForUser("__no_user__");
        }
    }

    public void syncOrdersFromFirebaseBlocking(@Nullable String userId, @Nullable String phone) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        if (safeUserId.isEmpty() && safePhone.isEmpty()) {
            return;
        }

        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            com.google.firebase.firestore.Query query = null;
            if (!safeUserId.isEmpty()) {
                query = db.collection("orders").whereEqualTo("userId", safeUserId);
            } else if (!safePhone.isEmpty()) {
                query = db.collection("orders").whereEqualTo("billingPhone", safePhone);
            }
            if (query != null) {
                QuerySnapshot snapshot = Tasks.await(query.get());
                handleOrderSnapshot(snapshot);
            }
        } catch (Exception e) {
            Log.e(TAG, "syncOrdersFromFirebase failed", e);
        }
    }

    public void refreshOrders(String userId, String phone) {
        new Thread(() -> {
            try {
                syncOrdersFromFirebaseBlocking(userId, phone);

                if (rewardPointDao.getAllRewardHistoryList().isEmpty()) {
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "SYSTEM_INITIAL", 11200, "Tích điểm mua sắm", 1767261600000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_1", -2000, "Đổi quà: Sữa rửa mặt mini", 1772043600000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_2", -200, "Đổi quà: Freeship Đơn 0Đ", 1772698500000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, "REDEEM_HIST_3", -500, "Đổi quà: Voucher Giảm 50K", 1775831400000L
                    ));
                }

                if (userGiftDao.getUserGiftCount() == 0) {
                    List<UserGiftEntity> initGifts = Arrays.asList(
                            new UserGiftEntity(0, "voucher_50k", "Voucher Giảm 50K", "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.", 500, "2026-12-30 23:59:59", "Còn hạn", "voucher_discount", "SAVE50K", 300000, "Chăm Sóc Da Mặt", "fixed_amount", null, 50000, 1775831400000L),
                            new UserGiftEntity(0, "gift_cleanser", "Quà tặng: Sữa rửa mặt", "Nhận miễn phí 1 tuýp sữa rửa mặt bí đao mini 15ml.", 1000, "2026-12-15 23:59:59", "Còn hạn", "product", "FREECLN", 0, "Sữa rửa mặt", "product_gift", "p003", 0, 1772043600000L),
                            new UserGiftEntity(0, "gift_freeship", "Freeship Đơn 0Đ", "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng.", 200, "2026-06-11 23:59:59", "Hôm nay", "voucher_freeship", "FREESHIP", 150000, "Tất cả sản phẩm", "percentage", null, 100, 1772698500000L),
                            new UserGiftEntity(0, "voucher_10_percent", "Giảm 10% Cho Sản Phẩm Bưởi", "Áp dụng cho dòng sản phẩm tinh chất vỏ bưởi dưỡng tóc.", 300, "2026-10-31 23:59:59", "Hết hạn", "voucher_discount", "GRAPE10", 0, "Tinh chất bưởi", "percentage", null, 10, 1767261600000L)
                    );
                    userGiftDao.insertUserGifts(initGifts);
                }

                startListeningToOrders(userId, phone);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean redeemGift(
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
            int discountValue
    ) {
        try {
            rewardPointDao.insertRewardPoints(new RewardPointEntity(
                    0, "REDEEM_" + giftId, -cost, "Đổi quà: " + title, System.currentTimeMillis()
            ));
            SyncDataHelper.syncRewardPointsToFirestore(localJsonReader.getContext());
            userGiftDao.insertUserGift(new UserGiftEntity(
                    0, giftId, title, description, cost, expiryDate, "Còn hạn", giftType, code,
                    minOrderValue, applicableProducts, offerType, productId, discountValue, System.currentTimeMillis()
            ));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Flow<List<UserGiftEntity>> getAllUserGifts() {
        return userGiftDao.getAllUserGiftsFlow();
    }

    public boolean deleteUserGiftById(int id) {
        try {
            return userGiftDao.deleteUserGiftById(id) > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateOrderReview(
            String orderId,
            int stars,
            String text,
            String image,
            boolean isAnonymous,
            boolean recommend
    ) {
        try {
            int wordCount = 0;
            if (text != null && !text.trim().isEmpty()) {
                String[] words = text.trim().split("\\s+");
                wordCount = words.length;
            }
            boolean hasText = text != null && !text.trim().isEmpty() && wordCount <= 200;
            boolean hasImage = image != null && !image.isEmpty();
            boolean qualifiesForCoins = stars > 0 && hasText && hasImage;

            orderDao.updateOrderReviewStatus(orderId, true);

            if (qualifiesForCoins) {
                if (!rewardPointDao.hasReceivedPointsForOrder(orderId)) {
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, orderId, 200, "Đánh giá đơn hàng " + orderId + " kèm hình ảnh", System.currentTimeMillis()
                    ));
                    SyncDataHelper.syncRewardPointsToFirestore(localJsonReader.getContext());
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void cancelOrder(String orderId) {
        android.content.Context appContext = localJsonReader.getAppContext();
        new Thread(() -> {
            try {
                OrderEntity existing = orderDao.getOrderByIdSync(orderId);
                String previousStatus = existing != null ? existing.getStatus() : null;
                if ("Đã hủy".equals(previousStatus)) {
                    return;
                }
                Tasks.await(FirebaseFirestore.getInstance().collection("orders").document(orderId)
                        .update("status", "Đã hủy"));
                orderDao.updateOrderStatus(orderId, "Đã hủy");
                if (existing != null) {
                    existing.setStatus("Đã hủy");
                    OrderStatusNotifier.notifyIfStatusChanged(appContext, existing, previousStatus, "Đã hủy");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public Flow<List<OrderEntity>> getOrderById(String orderId) {
        return orderDao.getOrderByIdFlow(orderId);
    }

    public void syncOrdersFromAssets() {
        ioExecutor.execute(this::syncOrdersFromAssetsBlocking);
    }

    public void syncOrdersFromAssetsBlocking() {
        try {
            List<OrderEntity> mockOrders = localJsonReader.getAllOrders();
            if (mockOrders == null || mockOrders.isEmpty()) {
                Log.w("OrderRepository", "No orders found in orders.json");
                return;
            }
            
            String currentUserId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(localJsonReader.getAppContext());
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                currentUserId = "test_001";
            }
            
            for (OrderEntity order : mockOrders) {
                normalizeOrderForRoom(order);
                // Map the mock data to the current logged-in user so they can see the orders
                if ("test_001".equals(order.getUserId())) {
                    order.setUserId(currentUserId);
                }
                if (order.getAffiliate() != null && "test_001".equals(order.getAffiliate().getReferrerUserId())) {
                    order.getAffiliate().setReferrerUserId(currentUserId);
                }
            }
            
            // Force replace existing orders without deleting user-created orders
            try {
                orderDao.insertOrders(mockOrders);
            } catch (Exception batchError) {
                Log.w("OrderRepository", "Batch insert failed, trying one-by-one", batchError);
                for (OrderEntity order : mockOrders) {
                    try {
                        orderDao.insertOrder(order);
                    } catch (Exception singleError) {
                        Log.e("OrderRepository", "Failed to insert order " + order.getId(), singleError);
                    }
                }
            }
            int count = orderDao.getOrderCount();
            Log.d("OrderRepository", "Synced " + mockOrders.size() + " orders from assets, DB count=" + count);
        } catch (Exception e) {
            Log.e("OrderRepository", "syncOrdersFromAssets failed", e);
        }
    }

    public List<OrderEntity> filterBuyerOrdersFromAssets(@Nullable String userId, @Nullable String phone) {
        List<OrderEntity> allOrders = localJsonReader.getAllOrders();
        List<OrderEntity> matched = new ArrayList<>();
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = normalizePhone(phone);
        
        String currentUserId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(localJsonReader.getAppContext());
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            currentUserId = "test_001";
        }

        for (OrderEntity order : allOrders) {
            if ("test_001".equals(order.getUserId())) {
                order.setUserId(currentUserId);
            }
            if (matchesBuyer(order, safeUserId, safePhone)) {
                matched.add(order);
            }
        }
        return matched;
    }

    @Nullable
    public OrderEntity findOrderByIdFromAssets(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        for (OrderEntity order : localJsonReader.getAllOrders()) {
            if (orderId.equals(order.getId())) {
                return order;
            }
        }
        return null;
    }

    private static String normalizePhone(@Nullable String phone) {
        return phone != null ? phone.replace(" ", "").trim() : "";
    }

    /** Room/KSP rejects null for String columns parsed from JSON/Firestore without this field. */
    private static void normalizeOrderForRoom(OrderEntity order) {
        if (order == null) {
            return;
        }
        if (order.getExpectedDeliveryTime() == null) {
            order.setExpectedDeliveryTime("");
        }
        if (order.getDeliveryDate() == null) {
            order.setDeliveryDate("");
        }
    }

    private static void enrichCreatedAt(OrderEntity order, DocumentSnapshot doc) {
        if (order == null) {
            return;
        }
        if (doc != null && doc.contains("createdAt")) {
            Long value = doc.getLong("createdAt");
            if (value != null && value > 0L) {
                order.setCreatedAt(value);
                return;
            }
        }
        if (order.getCreatedAt() <= 0L) {
            long fromId = createdAtFromOrderId(order.getId());
            if (fromId > 0L) {
                order.setCreatedAt(fromId);
                return;
            }
            if (doc != null) {
                order.setCreatedAt(createdAtFromOrderFields(
                        doc.getString("orderDate"),
                        doc.getString("orderTime")
                ));
            }
        }
    }

    private static long resolveCreatedAtForFirestore(DocumentSnapshot doc) {
        if (doc == null) {
            return 0L;
        }
        Long existing = doc.contains("createdAt") ? doc.getLong("createdAt") : null;
        if (existing != null && existing > 0L) {
            return existing;
        }

        String orderId = doc.getString("id");
        if (orderId == null || orderId.trim().isEmpty()) {
            orderId = doc.getId();
        }

        long fromId = createdAtFromOrderId(orderId);
        if (fromId > 0L) {
            return fromId;
        }
        return createdAtFromOrderFields(doc.getString("orderDate"), doc.getString("orderTime"));
    }

    private static long createdAtFromOrderId(@Nullable String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return 0L;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d{8})").matcher(orderId);
        if (!matcher.find()) {
            return 0L;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
            Date parsed = sdf.parse(matcher.group(1));
            return parsed != null ? parsed.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static long createdAtFromOrderFields(@Nullable String orderDate, @Nullable String orderTime) {
        if (orderDate == null || orderDate.trim().isEmpty()) {
            return 0L;
        }
        try {
            String datePart = orderDate.trim();
            String timePart = orderTime != null ? orderTime.trim() : "";
            SimpleDateFormat sdf;
            String raw;
            if (!timePart.isEmpty()) {
                sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
                raw = datePart + " " + timePart;
            } else {
                sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
                raw = datePart;
            }
            Date parsed = sdf.parse(raw);
            return parsed != null ? parsed.getTime() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    private static boolean matchesBuyer(OrderEntity order, String userId, String normalizedPhone) {
        if (order == null) {
            return false;
        }
        if (!userId.isEmpty() && userId.equals(order.getUserId())) {
            return true;
        }
        if (!normalizedPhone.isEmpty()) {
            String orderPhone = normalizePhone(order.getShippingPhone());
            if (!orderPhone.isEmpty() && orderPhone.equals(normalizedPhone)) {
                return true;
            }
        }
        return userId.isEmpty() && normalizedPhone.isEmpty();
    }

    public void seedOrdersFromAssetsIfNeeded() {
        syncOrdersFromAssetsBlocking();
    }

    public void updateOrderStatus(String orderId, String status) {
        android.content.Context appContext = localJsonReader.getAppContext();
        new Thread(() -> {
            try {
                OrderEntity existing = orderDao.getOrderByIdSync(orderId);
                String previousStatus = existing != null ? existing.getStatus() : null;
                if (status != null && status.equals(previousStatus)) {
                    return;
                }
                Tasks.await(FirebaseFirestore.getInstance().collection("orders").document(orderId)
                        .update("status", status));
                orderDao.updateOrderStatus(orderId, status);
                if (existing != null) {
                    existing.setStatus(status);
                    OrderStatusNotifier.notifyIfStatusChanged(appContext, existing, previousStatus, status);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void ensureOrderExists(String orderId) {
        new Thread(() -> {
            try {
                OrderEntity existing = orderDao.getOrderById(orderId);
                if (existing == null) {
                    List<OrderItem> items = new ArrayList<>();
                    OrderItem item = new OrderItem();
                    item.setProductId("product_rose_cream");
                    item.setProductName("Kem dưỡng hoa hồng Cocoon 50ml");
                    item.setProductImage("https://res.cloudinary.com/dpjkzxjl2/image/upload/v1781257994/rose_cream_u2kgwf.png");
                    item.setQuantity(1);
                    item.setPrice(222000L);
                    items.add(item);
                    
                    OrderEntity mockOrder = new OrderEntity(orderId);
                    mockOrder.setOrderDate("16/06/2026");
                    mockOrder.setOrderTime("15:30");
                    mockOrder.setStatus("Đang giao");
                    mockOrder.setTotalAmount(202000L);
                    mockOrder.setSubTotal(222000L);
                    mockOrder.setItems(items);
                    mockOrder.setUserId("test_001");
                    mockOrder.setHasReview(false);
                    mockOrder.setShippingName("Nguyễn Khánh Xuân");
                    mockOrder.setShippingPhone("090 123 4567");
                    mockOrder.setShippingAddress("123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh");
                    mockOrder.setShippingCost(30000L);
                    mockOrder.setVoucherDiscount(50000L);
                    mockOrder.setPaymentMethod("Thanh toán khi nhận hàng (COD)");
                    mockOrder.setAffiliate(null);
                    
                    orderDao.insertOrder(mockOrder);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
