package com.veganbeauty.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.features.account.notification.NotificationPushHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
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
                    notifyIfStatusChanged(context, order, previousStatus, newStatus);
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
            String previousStatus = order.getStatus();
            if (newStatus.equals(previousStatus)) {
                return;
            }
            notifyIfStatusChanged(context, order, previousStatus, newStatus);
        } catch (Exception e) {
            Log.e(TAG, "simulateOnly failed for orderId=" + orderId, e);
        }
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
        if (newStatus.equals(previousStatus)) {
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

        String title;
        String content;
        switch (newStatus) {
            case "Đang xử lý":
            case "Đã xác nhận":
                title = "Đơn hàng đã được xác nhận";
                content = buildMessageBody(order.getId(), "đã được xác nhận và đang được chuẩn bị");
                break;
            case "Đang giao hàng":
            case "Đang vận chuyển":
                title = "Đơn hàng đang được giao";
                content = buildMessageBody(order.getId(), "đang được giao đến bạn");
                break;
            case "Đã giao hàng":
            case "Hoàn tất":
                title = "Đơn hàng đã giao thành công";
                content = buildMessageBody(order.getId(), "đã được giao thành công");
                break;
            case "Đã hủy":
                title = "Đơn hàng đã bị huỷ";
                content = buildMessageBody(order.getId(), "đã được huỷ theo yêu cầu");
                break;
            default:
                title = "Cập nhật đơn hàng";
                content = buildMessageBody(order.getId(), "đã được cập nhật trạng thái: " + newStatus);
                break;
        }

        dispatchInApp(context.getApplicationContext(), order.getId(), title, content);
    }

    private static void dispatchInApp(Context appContext, String orderId, String title, String content) {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String notificationId = "order_" + orderId + "_" + System.currentTimeMillis();

        NotificationItem item = new NotificationItem(
                notificationId,
                title,
                content,
                now,
                "Đơn hàng",
                null,
                null,
                "CHI TIẾT",
                false,
                "Hôm nay",
                "ic_notification",
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
        Log.i(TAG, "Order notification dispatched for " + orderId + ": " + title);
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
        String friendlyStatus;
        switch (status) {
            case "Đang xử lý":
            case "Đã xác nhận":
                friendlyStatus = "đã được xác nhận và đang được chuẩn bị";
                break;
            case "Đang giao hàng":
            case "Đang vận chuyển":
                friendlyStatus = "đang được giao đến bạn";
                break;
            case "Đã giao hàng":
            case "Hoàn tất":
                friendlyStatus = "đã được giao thành công";
                break;
            case "Đã hủy":
                friendlyStatus = "đã được hủy theo yêu cầu";
                break;
            default:
                friendlyStatus = "đã được cập nhật trạng thái: " + status;
                break;
        }
        return buildMessageBody(orderId, friendlyStatus);
    }

    private static String buildMessageBody(String orderId, String statusPhrase) {
        return "Rootie: Đơn hàng #" + orderId + " của bạn " + statusPhrase + ". Cảm ơn bạn đã mua sắm tại Rootie Vietnam.";
    }
}
