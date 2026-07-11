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
import com.veganbeauty.app.data.local.OrderReviewLocalStore;
import com.veganbeauty.app.data.local.ProductReviewLocalStore;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.dao.RewardPointDao;
import com.veganbeauty.app.data.local.dao.UserGiftDao;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.repository.OrderStatusNotifier;
import com.veganbeauty.app.utils.FeedbackImageUploadHelper;
import com.veganbeauty.app.features.community.affiliate.AffiliateProductsHelper;

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

    private static boolean isPendingOrderStatus(@Nullable String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "Chờ xác nhận".equalsIgnoreCase(normalized)
                || "Pending".equalsIgnoreCase(normalized);
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
                if (order == null) {
                    continue;
                }
                if (order.getId() == null || order.getId().trim().isEmpty()) {
                    order.setId(doc.getId());
                }
                if (order.getId() == null || order.getId().trim().isEmpty()) {
                    continue;
                }
                enrichCreatedAt(order, doc);
                normalizeOrderForRoom(order);
                // Ưu tiên feedback đã lưu trên máy khách, rồi mới fallback Room
                if (appContext != null) {
                    OrderReviewLocalStore.applyTo(appContext, order);
                }
                preserveLocalReviewIfNeeded(order);

                orders.add(order);
                if (appContext != null) {
                    OrderStatusNotifier.ensureStatusNotification(appContext, order);
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
            if (appContext != null) {
                for (OrderEntity order : orders) {
                    if (AffiliateProductsHelper.isCompletedOrderStatus(order.getStatus())) {
                        AffiliateProductsHelper.syncProductsFromCompletedOrder(appContext, order);
                    }
                }
            }
        }
    }

    /**
     * Firebase sync dùng REPLACE — nếu remote chưa có review mà local/Room đã có thì giữ lại.
     */
    private void preserveLocalReviewIfNeeded(OrderEntity remoteOrder) {
        if (remoteOrder == null || remoteOrder.getId() == null) return;
        if (remoteOrder.isHasReview() && remoteOrder.getReviewStars() > 0) {
            // Remote/local-store đã có review đủ — vẫn bổ sung text nếu đang trống
            if (remoteOrder.getReviewText() != null && !remoteOrder.getReviewText().trim().isEmpty()) {
                return;
            }
        }
        try {
            android.content.Context appContext = localJsonReader.getAppContext();
            if (appContext != null && OrderReviewLocalStore.applyTo(appContext, remoteOrder)) {
                return;
            }
            OrderEntity local = orderDao.getOrderByIdSync(remoteOrder.getId());
            if (local == null || !local.isHasReview()) {
                return;
            }
            remoteOrder.setHasReview(true);
            remoteOrder.setReviewStars(local.getReviewStars());
            remoteOrder.setReviewText(local.getReviewText());
            remoteOrder.setReviewImage(local.getReviewImage());
            remoteOrder.setAnonymous(local.isAnonymous());
            remoteOrder.setRecommendToFriends(local.isRecommendToFriends());
        } catch (Exception e) {
            Log.w(TAG, "preserveLocalReviewIfNeeded failed for " + remoteOrder.getId(), e);
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

                boolean isFreshDemo = com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                        .isDemoAccount(userId, null)
                        || com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                        .isDemoAccount(null, com.veganbeauty.app.data.local.ProfileSession.getEmail(
                                localJsonReader.getAppContext()));
                boolean allowSampleData = !isFreshDemo
                        && com.veganbeauty.app.data.local.ProfileSession.isDemoTeamUser(userId);

                // Chỉ demo-team mới seed xu/quà mẫu. Tài khoản mới (vd. Nguyễn Thị Demo) = 0.
                String safeSeedUserId = userId != null ? userId.trim() : "";
                if (allowSampleData && !safeSeedUserId.isEmpty()
                        && rewardPointDao.getAllRewardHistoryList(safeSeedUserId).isEmpty()) {
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, safeSeedUserId, "SYSTEM_INITIAL", 11200, "Tích điểm mua sắm", 1767261600000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, safeSeedUserId, "REDEEM_HIST_1", -2000, "Đổi quà: Sữa rửa mặt mini", 1772043600000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, safeSeedUserId, "REDEEM_HIST_2", -200, "Đổi quà: Freeship Đơn 0Đ", 1772698500000L
                    ));
                    rewardPointDao.insertRewardPoints(new RewardPointEntity(
                            0, safeSeedUserId, "REDEEM_HIST_3", -500, "Đổi quà: Voucher Giảm 50K", 1775831400000L
                    ));
                }

                if (allowSampleData && !safeSeedUserId.isEmpty()
                        && userGiftDao.getUserGiftCount(safeSeedUserId) == 0) {
                    List<UserGiftEntity> initGifts = Arrays.asList(
                            new UserGiftEntity(0, safeSeedUserId, "voucher_50k", "Voucher Giảm 50K", "Áp dụng cho đơn hàng từ 300K, sản phẩm nguyên giá.", 500, "2026-12-30 23:59:59", "Còn hạn", "voucher_discount", "SAVE50K", 300000, "Chăm Sóc Da Mặt", "fixed_amount", null, 50000, 1775831400000L),
                            new UserGiftEntity(0, safeSeedUserId, "gift_cleanser", "Quà tặng: Sữa rửa mặt", "Nhận miễn phí 1 tuýp sữa rửa mặt bí đao mini 15ml.", 1000, "2026-12-15 23:59:59", "Còn hạn", "product", "FREECLN", 0, "Sữa rửa mặt", "product_gift", "p003", 0, 1772043600000L),
                            new UserGiftEntity(0, safeSeedUserId, "gift_freeship", "Freeship Đơn 0Đ", "Miễn phí vận chuyển toàn quốc cho mọi đơn hàng.", 200, "2026-06-11 23:59:59", "Hôm nay", "voucher_freeship", "FREESHIP", 150000, "Tất cả sản phẩm", "percentage", null, 100, 1772698500000L),
                            new UserGiftEntity(0, safeSeedUserId, "voucher_10_percent", "Giảm 10% Cho Sản Phẩm Bưởi", "Áp dụng cho dòng sản phẩm tinh chất vỏ bưởi dưỡng tóc.", 300, "2026-10-31 23:59:59", "Hết hạn", "voucher_discount", "GRAPE10", 0, "Tinh chất bưởi", "percentage", null, 10, 1767261600000L)
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
            android.content.Context ctx = localJsonReader.getContext();
            String userId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(ctx);
            if (userId == null || userId.trim().isEmpty()) {
                return false;
            }
            userId = userId.trim();
            int spent = com.veganbeauty.app.utils.RewardPointsHelper.spendPoints(
                    ctx,
                    "REDEEM_" + giftId,
                    cost,
                    "Đổi quà: " + title
            );
            if (spent < 0) {
                return false;
            }
            userGiftDao.insertUserGift(new UserGiftEntity(
                    0, userId, giftId, title, description, cost, expiryDate, "Còn hạn", giftType, code,
                    minOrderValue, applicableProducts, offerType, productId, discountValue, System.currentTimeMillis()
            ));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Flow<List<UserGiftEntity>> getAllUserGifts() {
        android.content.Context ctx = localJsonReader.getContext();
        String userId = com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(ctx);
        if (userId == null) {
            userId = "";
        }
        return userGiftDao.getAllUserGiftsFlow(userId.trim());
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
        List<String> images = new ArrayList<>();
        if (image != null && !image.trim().isEmpty()) {
            images.add(image.trim());
        }
        return updateOrderReview(orderId, stars, text, images, isAnonymous, recommend);
    }

    public boolean updateOrderReview(
            String orderId,
            int stars,
            String text,
            List<String> images,
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
            boolean hasImage = images != null && !images.isEmpty();
            boolean qualifiesForCoins = stars > 0 && hasText && hasImage;

            android.content.Context appContext = localJsonReader.getAppContext();
            List<String> sourceImages = images != null ? images : new ArrayList<>();
            List<String> remoteImages = sourceImages;
            if (appContext != null && !sourceImages.isEmpty()) {
                remoteImages = FeedbackImageUploadHelper.ensureRemoteUrls(
                        appContext,
                        sourceImages,
                        "feedback/orders/" + orderId
                );
            }

            org.json.JSONArray arr = new org.json.JSONArray();
            for (String path : remoteImages) {
                if (path != null && !path.trim().isEmpty()) {
                    arr.put(path.trim());
                }
            }
            String imageJson = arr.toString();

            String safeText = text != null ? text : "";
            int updatedRows = orderDao.updateOrderReviewFull(
                    orderId,
                    true,
                    stars,
                    safeText,
                    imageJson,
                    isAnonymous,
                    recommend
            );
            if (updatedRows <= 0) {
                throw new IllegalStateException("Không tìm thấy đơn hàng để lưu đánh giá: " + orderId);
            }

            // Lưu bền theo orderId trên máy khách (không phụ thuộc Firebase sync)
            if (appContext != null) {
                OrderReviewLocalStore.save(
                        appContext,
                        orderId,
                        stars,
                        safeText,
                        imageJson,
                        isAnonymous,
                        recommend
                );
            }

            // Đồng bộ review lên Firestore (merge) để không bị sync ghi đè mất
            try {
                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                updates.put("hasReview", true);
                updates.put("reviewStars", stars);
                updates.put("reviewText", safeText);
                updates.put("reviewImage", imageJson);
                updates.put("isAnonymous", isAnonymous);
                updates.put("recommendToFriends", recommend);
                Tasks.await(FirebaseFirestore.getInstance()
                        .collection("orders")
                        .document(orderId)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge()));
            } catch (Exception firestoreError) {
                Log.e(TAG, "Failed to sync order review to Firestore: " + orderId, firestoreError);
            }

            // Lưu đánh giá theo từng sản phẩm trong đơn để hiện ở trang sản phẩm
            if (appContext != null) {
                OrderEntity order = orderDao.getOrderByIdSync(orderId);
                if (order != null) {
                    // Đảm bảo entity local cũng có đủ field review (phòng Flow đọc lại)
                    order.setHasReview(true);
                    order.setReviewStars(stars);
                    order.setReviewText(safeText);
                    order.setReviewImage(imageJson);
                    order.setAnonymous(isAnonymous);
                    order.setRecommendToFriends(recommend);
                    orderDao.insertOrder(order);

                    if (order.getItems() != null && !order.getItems().isEmpty()) {
                        List<String> productIds = new ArrayList<>();
                        for (OrderItem item : order.getItems()) {
                            if (item != null && item.getProductId() != null && !item.getProductId().trim().isEmpty()) {
                                productIds.add(item.getProductId().trim());
                            }
                        }
                        String reviewerName = ProfileSession.getFullName(appContext);
                        ProductReviewLocalStore.upsertFromOrder(
                                appContext,
                                orderId,
                                productIds,
                                reviewerName,
                                stars,
                                safeText,
                                isAnonymous
                        );
                    }
                }
            }

            if (qualifiesForCoins) {
                if (!rewardPointDao.hasReceivedPointsForOrder(
                        com.veganbeauty.app.utils.ProfileSessionHelper.getEffectiveUserId(
                                localJsonReader.getAppContext()),
                        orderId)) {
                    com.veganbeauty.app.utils.RewardPointsHelper.awardPoints(
                            localJsonReader.getAppContext(),
                            orderId,
                            200,
                            "Đánh giá đơn hàng " + orderId + " kèm hình ảnh",
                            "từ đánh giá đơn hàng",
                            false
                    );
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Không thể lưu đánh giá đơn hàng", e);
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
            String currentUserId = com.veganbeauty.app.utils.ProfileSessionHelper
                    .getEffectiveUserId(localJsonReader.getAppContext());
            if (currentUserId == null || currentUserId.trim().isEmpty()) {
                return;
            }
            // Chỉ tài khoản demo-team mới được seed đơn mẫu. Tài khoản mới/thường = 0 đơn từ asset.
            if (com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                    .isDemoAccount(currentUserId, null)
                    || com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                    .isDemoAccount(null, com.veganbeauty.app.data.local.ProfileSession.getEmail(
                            localJsonReader.getAppContext()))
                    || !com.veganbeauty.app.data.local.ProfileSession.isDemoTeamUser(currentUserId)) {
                return;
            }

            List<OrderEntity> mockOrders = localJsonReader.getAllOrders();
            if (mockOrders == null || mockOrders.isEmpty()) {
                Log.w("OrderRepository", "No orders found in orders.json");
                return;
            }

            List<OrderEntity> toInsert = new ArrayList<>();
            for (OrderEntity order : mockOrders) {
                normalizeOrderForRoom(order);
                // Giữ nguyên userId trong asset — không remap sang tài khoản đang đăng nhập
                if (currentUserId.equals(order.getUserId())) {
                    toInsert.add(order);
                }
            }
            if (toInsert.isEmpty()) {
                return;
            }

            try {
                orderDao.insertOrders(toInsert);
            } catch (Exception batchError) {
                Log.w("OrderRepository", "Batch insert failed, trying one-by-one", batchError);
                for (OrderEntity order : toInsert) {
                    try {
                        orderDao.insertOrder(order);
                    } catch (Exception singleError) {
                        Log.e("OrderRepository", "Failed to insert order " + order.getId(), singleError);
                    }
                }
            }
            int count = orderDao.getOrderCount();
            Log.d("OrderRepository", "Synced " + toInsert.size() + " orders from assets, DB count=" + count);
        } catch (Exception e) {
            Log.e("OrderRepository", "syncOrdersFromAssets failed", e);
        }
    }

    public List<OrderEntity> filterBuyerOrdersFromAssets(@Nullable String userId, @Nullable String phone) {
        List<OrderEntity> allOrders = localJsonReader.getAllOrders();
        List<OrderEntity> matched = new ArrayList<>();
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = normalizePhone(phone);

        if (com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                .isDemoAccount(safeUserId, null)
                || com.veganbeauty.app.features.auth.FreshDemoAccountSeeder
                .isDemoAccount(null, com.veganbeauty.app.data.local.ProfileSession.getEmail(
                        localJsonReader.getAppContext()))) {
            return matched;
        }

        for (OrderEntity order : allOrders) {
            // Không remap test_001 → user hiện tại; chỉ khớp đúng chủ đơn
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
                    if (appContext != null
                            && AffiliateProductsHelper.isCompletedOrderStatus(status)
                            && !AffiliateProductsHelper.isCompletedOrderStatus(previousStatus)) {
                        AffiliateProductsHelper.syncProductsFromCompletedOrder(appContext, existing);
                    }
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
