package com.veganbeauty.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.features.shop.product.detail.ProductReview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Lưu đánh giá sản phẩm từ đơn hàng (local), hiển thị trên trang chi tiết sản phẩm. */
public final class ProductReviewLocalStore {

    private static final String PREFS = "product_reviews_store";
    private static final String KEY_DATA = "reviews_by_product";

    private ProductReviewLocalStore() {
    }

    public static void upsertFromOrder(
            @NonNull Context context,
            @NonNull String orderId,
            @Nullable List<String> productIds,
            @NonNull String reviewerName,
            int stars,
            @Nullable String comment,
            boolean anonymous
    ) {
        if (productIds == null || productIds.isEmpty() || stars <= 0) {
            return;
        }
        try {
            JSONObject root = readRoot(context);
            String displayName = anonymous
                    ? "Người dùng Rootie"
                    : (reviewerName != null && !reviewerName.trim().isEmpty()
                    ? reviewerName.trim()
                    : "Người dùng Rootie");
            String safeComment = comment != null ? comment.trim() : "";
            long now = System.currentTimeMillis();

            for (String productId : productIds) {
                if (productId == null || productId.trim().isEmpty()) continue;
                String pid = productId.trim();
                JSONArray list = root.optJSONArray(pid);
                if (list == null) list = new JSONArray();

                JSONArray updated = new JSONArray();
                boolean replaced = false;
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.optJSONObject(i);
                    if (item == null) continue;
                    if (orderId.equals(item.optString("orderId", ""))) {
                        item.put("orderId", orderId);
                        item.put("reviewerName", displayName);
                        item.put("rating", stars);
                        item.put("comment", safeComment);
                        item.put("anonymous", anonymous);
                        item.put("createdAt", now);
                        updated.put(item);
                        replaced = true;
                    } else {
                        updated.put(item);
                    }
                }
                if (!replaced) {
                    JSONObject neu = new JSONObject();
                    neu.put("orderId", orderId);
                    neu.put("reviewerName", displayName);
                    neu.put("rating", stars);
                    neu.put("comment", safeComment);
                    neu.put("anonymous", anonymous);
                    neu.put("createdAt", now);
                    // Đưa review mới lên đầu
                    JSONArray withNew = new JSONArray();
                    withNew.put(neu);
                    for (int i = 0; i < updated.length(); i++) {
                        withNew.put(updated.get(i));
                    }
                    updated = withNew;
                }
                root.put(pid, updated);
            }
            writeRoot(context, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    public static List<ProductReview> getReviews(@NonNull Context context, @Nullable String productId) {
        List<ProductReview> result = new ArrayList<>();
        if (productId == null || productId.trim().isEmpty()) {
            return result;
        }
        String pid = productId.trim();
        try {
            // Backfill từ đơn hàng đã đánh giá trong Room (phòng trường hợp store chưa có)
            backfillFromOrdersIfNeeded(context, pid);

            JSONObject root = readRoot(context);
            JSONArray list = root.optJSONArray(pid);
            if (list == null) return result;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item == null) continue;
                int rating = item.optInt("rating", 0);
                if (rating <= 0) continue;
                String name = item.optString("reviewerName", "Người dùng Rootie");
                String comment = item.optString("comment", "");
                if (comment.isEmpty()) {
                    comment = "Đã mua và đánh giá sản phẩm.";
                }
                result.add(new ProductReview(name, rating, comment));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void backfillFromOrdersIfNeeded(@NonNull Context context, @NonNull String productId) {
        try {
            List<com.veganbeauty.app.data.local.entities.OrderEntity> reviewed =
                    com.veganbeauty.app.data.local.RootieDatabase
                            .getDatabase(context)
                            .orderDao()
                            .getReviewedOrdersSync();
            if (reviewed == null || reviewed.isEmpty()) return;

            for (com.veganbeauty.app.data.local.entities.OrderEntity order : reviewed) {
                if (order == null || order.getItems() == null) continue;
                boolean containsProduct = false;
                List<String> productIds = new ArrayList<>();
                for (com.veganbeauty.app.data.local.entities.OrderEntity.OrderItem item : order.getItems()) {
                    if (item == null || item.getProductId() == null) continue;
                    String id = item.getProductId().trim();
                    if (id.isEmpty()) continue;
                    productIds.add(id);
                    if (productId.equals(id)) containsProduct = true;
                }
                if (!containsProduct) continue;

                // Chỉ upsert nếu chưa có review của order này
                if (hasOrderReview(context, productId, order.getId())) continue;

                String reviewerName = order.isAnonymous()
                        ? "Người dùng Rootie"
                        : ProfileSession.getFullName(context);
                upsertFromOrder(
                        context,
                        order.getId(),
                        productIds,
                        reviewerName,
                        order.getReviewStars() > 0 ? order.getReviewStars() : 5,
                        order.getReviewText(),
                        order.isAnonymous()
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean hasOrderReview(
            @NonNull Context context,
            @NonNull String productId,
            @Nullable String orderId
    ) {
        if (orderId == null || orderId.isEmpty()) return false;
        try {
            JSONObject root = readRoot(context);
            JSONArray list = root.optJSONArray(productId);
            if (list == null) return false;
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item != null && orderId.equals(item.optString("orderId", ""))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @NonNull
    private static JSONObject readRoot(@NonNull Context context) {
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String raw = prefs.getString(KEY_DATA, "{}");
            if (raw == null || raw.trim().isEmpty()) return new JSONObject();
            return new JSONObject(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static void writeRoot(@NonNull Context context, @NonNull JSONObject root) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DATA, root.toString())
                .apply();
    }
}
