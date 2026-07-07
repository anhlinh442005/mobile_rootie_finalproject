package com.veganbeauty.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Tracks the last order status we already notified the user about.
 * Prevents missed notifications when Room already has the new status.
 */
final class OrderNotificationTracker {

    private static final String PREFS = "order_notification_tracker";

    private OrderNotificationTracker() {
    }

    static void markNotified(Context context, String orderId, String status) {
        if (context == null || orderId == null || orderId.trim().isEmpty() || status == null) {
            return;
        }
        prefs(context).edit()
                .putString(key(orderId), normalize(status))
                .apply();
    }

    @Nullable
    static String getLastNotifiedStatus(Context context, String orderId) {
        if (context == null || orderId == null || orderId.trim().isEmpty()) {
            return null;
        }
        return prefs(context).getString(key(orderId), null);
    }

    static boolean needsNotification(Context context, String orderId, String currentStatus) {
        if (orderId == null || orderId.trim().isEmpty() || currentStatus == null || currentStatus.trim().isEmpty()) {
            return false;
        }
        String lastNotified = getLastNotifiedStatus(context, orderId);
        return !normalize(currentStatus).equals(normalize(lastNotified));
    }

    static String normalize(@Nullable String status) {
        if (status == null) {
            return "";
        }
        String value = status.trim();
        if (value.isEmpty()) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if ("chờ xác nhận".equals(lower) || "pending".equals(lower)) {
            return "pending";
        }
        if ("đang xử lý".equals(lower) || "đã xác nhận".equals(lower)) {
            return "processing";
        }
        if ("đang giao".equals(lower)
                || "đang giao hàng".equals(lower)
                || "đang vận chuyển".equals(lower)) {
            return "delivering";
        }
        if ("đã giao hàng".equals(lower) || "hoàn tất".equals(lower)) {
            return "delivered";
        }
        if ("đã hủy".equals(lower) || "đã huỷ".equals(lower)) {
            return "cancelled";
        }
        return lower;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String orderId) {
        return "status_" + orderId.trim();
    }
}
