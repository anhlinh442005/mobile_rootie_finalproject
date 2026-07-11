package com.veganbeauty.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.entities.OrderEntity;

import org.json.JSONObject;

/**
 * Lưu feedback đơn hàng theo orderId (SharedPreferences).
 * Dùng làm nguồn sự thật trên máy khách — tránh mất data khi Firebase sync REPLACE.
 */
public final class OrderReviewLocalStore {

    private static final String PREFS = "order_reviews_store";
    private static final String KEY_DATA = "reviews_by_order";

    private OrderReviewLocalStore() {
    }

    public static void save(
            @NonNull Context context,
            @NonNull String orderId,
            int stars,
            @Nullable String text,
            @Nullable String imageJson,
            boolean anonymous,
            boolean recommend
    ) {
        if (orderId.trim().isEmpty() || stars <= 0) return;
        try {
            JSONObject root = readRoot(context);
            JSONObject item = new JSONObject();
            item.put("orderId", orderId.trim());
            item.put("hasReview", true);
            item.put("stars", stars);
            item.put("text", text != null ? text : "");
            item.put("imageJson", imageJson != null ? imageJson : "[]");
            item.put("anonymous", anonymous);
            item.put("recommend", recommend);
            item.put("updatedAt", System.currentTimeMillis());
            root.put(orderId.trim(), item);
            writeRoot(context, root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean hasReview(@NonNull Context context, @Nullable String orderId) {
        JSONObject item = get(context, orderId);
        return item != null && item.optBoolean("hasReview", false) && item.optInt("stars", 0) > 0;
    }

    /**
     * Gắn feedback đã lưu vào OrderEntity (nếu có).
     * Ưu tiên data local của khách trên máy.
     */
    public static boolean applyTo(@Nullable Context context, @Nullable OrderEntity order) {
        if (context == null || order == null || order.getId() == null) return false;
        JSONObject item = get(context, order.getId());
        if (item == null || !item.optBoolean("hasReview", false)) {
            return false;
        }
        int stars = item.optInt("stars", 0);
        if (stars <= 0) return false;

        order.setHasReview(true);
        order.setReviewStars(stars);
        order.setReviewText(item.optString("text", ""));
        order.setReviewImage(item.optString("imageJson", "[]"));
        order.setAnonymous(item.optBoolean("anonymous", false));
        order.setRecommendToFriends(item.optBoolean("recommend", false));
        return true;
    }

    @Nullable
    private static JSONObject get(@NonNull Context context, @Nullable String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) return null;
        try {
            return readRoot(context).optJSONObject(orderId.trim());
        } catch (Exception e) {
            return null;
        }
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
