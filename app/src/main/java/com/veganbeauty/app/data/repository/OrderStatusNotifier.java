package com.veganbeauty.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.features.account.notification.NotificationDateHelper;
import com.veganbeauty.app.features.account.notification.NotificationPushHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OrderStatusNotifier {

    private static final String TAG = "OrderStatusNotifier";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private OrderStatusNotifier() {
    }

    public static void updateStatus(OrderDao orderDao, Context context, String orderId, String newStatus) {
        executor.execute(() -> {
            try {
                OrderEntity existing = orderDao.getOrderByIdSync(orderId);
                String previousStatus = existing != null ? existing.getStatus() : null;
                int updated = orderDao.updateOrderStatusSync(orderId, newStatus);
                if (updated <= 0) {
                    Log.w(TAG, "updateStatus: no order found for id=" + orderId);
                    return;
                }
                OrderEntity order = orderDao.getOrderByIdSync(orderId);
                if (order != null) {
                    ensureStatusNotification(context, order);
                }
            } catch (Exception e) {
                Log.e(TAG, "updateStatus failed for orderId=" + orderId, e);
            }
        });
    }

    public static void simulateOnly(OrderDao orderDao, Context context, String orderId, String newStatus) {
        try {
            OrderEntity order = orderDao.getOrderByIdSync(orderId);
            if (order == null) {
                return;
            }
            order.setStatus(newStatus);
            ensureStatusNotification(context, order);
        } catch (Exception e) {
            Log.e(TAG, "simulateOnly failed for orderId=" + orderId, e);
        }
    }

    /**
     * Sends order notifications based on the current status and what was already notified.
     * Safe to call repeatedly — skips statuses the user was already notified about.
     */
    public static void ensureStatusNotification(Context context, OrderEntity order) {
        if (order == null || order.getId() == null || order.getId().trim().isEmpty()) {
            return;
        }
        String newStatus = order.getStatus();
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return;
        }

        boolean isGuest = order.isGuest()
                || order.getUserId() == null
                || order.getUserId().trim().isEmpty();
        if (isGuest) {
            simulateOutboundNotification(order, newStatus);
            return;
        }

        if (context == null) {
            Log.w(TAG, "Missing context for order notification " + order.getId());
            return;
        }

        Context appContext = context.getApplicationContext();
        if (!OrderNotificationTracker.needsNotification(appContext, order.getId(), newStatus)) {
            return;
        }

        if (!ProfileSession.isOrderStatusEnabled(appContext)) {
            return;
        }

        String normalized = OrderNotificationTracker.normalize(newStatus);
        if ("pending".equals(normalized)
                && OrderNotificationTracker.getLastNotifiedStatus(appContext, order.getId()) == null) {
            notifyOrderPlaced(appContext, order);
            return;
        }

        String previousNotified = OrderNotificationTracker.getLastNotifiedStatus(appContext, order.getId());
        notifyIfStatusChanged(appContext, order, previousNotified, newStatus);
    }

    public static void notifyOrderPlaced(Context context, OrderEntity order) {
        if (order == null || order.getId() == null || order.getId().trim().isEmpty()) {
            return;
        }

        boolean isGuest = order.isGuest()
                || order.getUserId() == null
                || order.getUserId().trim().isEmpty();
        if (isGuest) {
            simulateOutboundNotification(order, order.getStatus() != null ? order.getStatus() : "Chờ xác nhận");
            return;
        }
        if (context == null) {
            Log.w(TAG, "Missing context for order placed notification " + order.getId());
            return;
        }

        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isOrderStatusEnabled(appContext)) {
            return;
        }

        String orderId = order.getId().trim();
        String status = order.getStatus() != null ? order.getStatus() : "Chờ xác nhận";
        if (!OrderNotificationTracker.needsNotification(appContext, orderId, status)) {
            return;
        }

        String notificationId = "order_placed_" + orderId;
        NotificationRepository repository = NotificationRepository.getInstance(appContext);
        for (NotificationItem existing : repository.notifications.getValue()) {
            if (notificationId.equals(existing.getId())) {
                OrderNotificationTracker.markNotified(appContext, orderId, status);
                return;
            }
        }

        String title = "Đơn hàng đặt thành công! \uD83D\uDED2";
        String content = buildMessageBody(orderId, "đã được ghi nhận và đang chờ xác nhận");
        dispatchInApp(
                appContext,
                orderId,
                notificationId,
                title,
                content,
                resolveOrderPlacementTime(order),
                status
        );
    }

    public static void notifyIfStatusChanged(
            Context context,
            OrderEntity order,
            String previousStatus,
            String newStatus
    ) {
        if (order == null || newStatus == null || newStatus.trim().isEmpty()) {
            return;
        }

        boolean isGuest = order.isGuest()
                || order.getUserId() == null
                || order.getUserId().trim().isEmpty();
        if (isGuest) {
            simulateOutboundNotification(order, newStatus);
            return;
        }

        if (context == null) {
            Log.w(TAG, "Missing context for in-app order notification " + order.getId());
            return;
        }

        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isOrderStatusEnabled(appContext)) {
            return;
        }
        if (!OrderNotificationTracker.needsNotification(appContext, order.getId(), newStatus)) {
            return;
        }

        NotificationContent notificationContent = resolveNotificationContent(order.getId(), newStatus);
        String notificationId = "order_status_" + order.getId() + "_" + System.currentTimeMillis();
        dispatchInApp(
                appContext,
                order.getId(),
                notificationId,
                notificationContent.title,
                notificationContent.content,
                currentEventTime(),
                newStatus
        );
    }

    private static void dispatchInApp(
            Context appContext,
            String orderId,
            String notificationId,
            String title,
            String content,
            String eventTime,
            String trackedStatus
    ) {
        NotificationItem item = new NotificationItem(
                notificationId,
                title,
                content,
                eventTime,
                "Đơn hàng",
                null,
                null,
                "CHI TIẾT",
                false,
                "Hôm nay",
                "ic_bell",
                "order",
                orderId,
                null
        );

        NotificationRepository.getInstance(appContext).addNotification(item);
        NotificationPushHelper.sendPushNotification(
                appContext,
                notificationId,
                title,
                content,
                "order",
                null,
                orderId,
                null
        );
        OrderNotificationTracker.markNotified(appContext, orderId, trackedStatus);
        Log.i(TAG, "Order notification dispatched for " + orderId + ": " + title);
    }

    public static void backfillPendingOrderNotifications(
            Context context,
            OrderDao orderDao,
            String userId
    ) {
        if (context == null || orderDao == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        try {
            List<OrderEntity> pendingOrders = orderDao.getPendingOrdersForUserSync(userId.trim());
            if (pendingOrders == null) {
                return;
            }
            for (OrderEntity order : pendingOrders) {
                notifyOrderPlaced(context, order);
            }
        } catch (Exception e) {
            Log.e(TAG, "backfillPendingOrderNotifications failed", e);
        }
    }

    /**
     * Re-checks local orders against last notified status — no extra Firestore reads.
     */
    public static void backfillMissedOrderStatusNotifications(
            Context context,
            OrderDao orderDao,
            String userId,
            String phone
    ) {
        if (context == null || orderDao == null) {
            return;
        }
        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        if (safeUserId.isEmpty() && safePhone.isEmpty()) {
            return;
        }
        try {
            List<OrderEntity> orders = orderDao.getOrdersForBuyerIdentitySync(safeUserId, safePhone);
            if (orders == null || orders.isEmpty()) {
                return;
            }
            for (OrderEntity order : orders) {
                ensureStatusNotification(context, order);
            }
        } catch (Exception e) {
            Log.e(TAG, "backfillMissedOrderStatusNotifications failed", e);
        }
    }

    private static NotificationContent resolveNotificationContent(String orderId, String status) {
        switch (OrderNotificationTracker.normalize(status)) {
            case "processing":
                return new NotificationContent(
                        "Đơn hàng đã được xác nhận",
                        buildMessageBody(orderId, "đã được xác nhận và đang được chuẩn bị")
                );
            case "delivering":
                return new NotificationContent(
                        "Đơn hàng đang được giao",
                        buildMessageBody(orderId, "đang được giao đến bạn")
                );
            case "delivered":
                return new NotificationContent(
                        "Đơn hàng đã giao thành công",
                        buildMessageBody(orderId, "đã được giao thành công")
                );
            case "cancelled":
                return new NotificationContent(
                        "Đơn hàng đã bị huỷ",
                        buildMessageBody(orderId, "đã được huỷ theo yêu cầu")
                );
            default:
                return new NotificationContent(
                        "Cập nhật đơn hàng",
                        buildMessageBody(orderId, "đã được cập nhật trạng thái: " + status)
                );
        }
    }

    private static void simulateOutboundNotification(OrderEntity order, String newStatus) {
        String email = order.getBillingEmail() != null ? order.getBillingEmail().trim() : "";
        String phone = order.getBillingPhone() != null ? order.getBillingPhone().trim() : "";
        String name = (order.getBillingName() != null && !order.getBillingName().trim().isEmpty())
                ? order.getBillingName().trim()
                : "Quý khách";
        String body = buildGuestMessageBody(order.getId(), newStatus);

        if (!email.isEmpty()) {
            Log.i(TAG, "[MOCK EMAIL -> " + email + "] To: " + name + " <" + email + "> | Subject: Cập nhật đơn hàng #" + order.getId() + " | Body: " + body);
        } else if (!phone.isEmpty()) {
            Log.i(TAG, "[MOCK SMS -> " + phone + "] To: " + name + " | Body: " + body);
        } else {
            Log.w(TAG, "Guest order " + order.getId() + " has no email or phone on file; cannot route notification.");
        }
    }

    private static String buildGuestMessageBody(String orderId, String status) {
        return resolveNotificationContent(orderId, status).content;
    }

    private static String buildMessageBody(String orderId, String statusPhrase) {
        return "Rootie: Đơn hàng #" + orderId + " của bạn " + statusPhrase + ". Cảm ơn bạn đã mua sắm tại Rootie Vietnam.";
    }

    private static String currentEventTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private static String resolveOrderPlacementTime(OrderEntity order) {
        if (order != null) {
            String fromFields = NotificationDateHelper.toStorageTimeFromOrder(
                    order.getOrderDate(),
                    order.getOrderTime()
            );
            if (fromFields != null) {
                return fromFields;
            }
            if (order.getCreatedAt() > 0L) {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date(order.getCreatedAt()));
            }
        }
        return currentEventTime();
    }

    private static final class NotificationContent {
        private final String title;
        private final String content;

        private NotificationContent(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }
}
