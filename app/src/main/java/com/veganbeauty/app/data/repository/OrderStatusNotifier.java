package com.veganbeauty.app.data.repository;

import android.util.Log;

import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.entities.OrderEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OrderStatusNotifier {

    private static final String TAG = "OrderStatusNotifier";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void updateStatus(OrderDao orderDao, String orderId, String newStatus) {
        executor.execute(() -> {
            try {
                int updated = orderDao.updateOrderStatusSync(orderId, newStatus);
                if (updated <= 0) {
                    Log.w(TAG, "updateStatus: no order found for id=" + orderId);
                    return;
                }
                simulateNotificationForOrder(orderDao, orderId, newStatus);
            } catch (Exception e) {
                Log.e(TAG, "updateStatus failed for orderId=" + orderId, e);
            }
        });
    }

    public static void simulateOnly(OrderDao orderDao, String orderId, String newStatus) {
        try {
            simulateNotificationForOrder(orderDao, orderId, newStatus);
        } catch (Exception e) {
            Log.e(TAG, "simulateOnly failed for orderId=" + orderId, e);
        }
    }

    private static void simulateNotificationForOrder(OrderDao orderDao, String orderId, String newStatus) throws Exception {
        OrderEntity order = orderDao.getOrderByIdSync(orderId);
        if (order == null) return;
        simulateOutboundNotification(order, newStatus);
    }

    private static void simulateOutboundNotification(OrderEntity order, String newStatus) {
        boolean isGuest = order.isGuest() || order.getUserId() == null || order.getUserId().trim().isEmpty();
        if (!isGuest) {
            Log.i(TAG, "Order " + order.getId() + " -> " + newStatus + " (user=" + order.getUserId() + ", in-app notification dispatched via user preferences)");
            return;
        }
        
        String email = order.getBillingEmail() != null ? order.getBillingEmail().trim() : "";
        String phone = order.getBillingPhone() != null ? order.getBillingPhone().trim() : "";
        String name = (order.getBillingName() != null && !order.getBillingName().trim().isEmpty()) ? order.getBillingName().trim() : "Quý khách";
        String body = buildMessageBody(order.getId(), newStatus);

        if (!email.isEmpty()) {
            Log.i(TAG, "[MOCK EMAIL -> " + email + "] To: " + name + " <" + email + "> | Subject: Cập nhật đơn hàng #" + order.getId() + " | Body: " + body);
        } else if (!phone.isEmpty()) {
            Log.i(TAG, "[MOCK SMS -> " + phone + "] To: " + name + " | Body: " + body);
        } else {
            Log.w(TAG, "Guest order " + order.getId() + " has no email or phone on file; cannot route notification.");
        }
    }

    private static String buildMessageBody(String orderId, String status) {
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
        return "Rootie: Đơn hàng #" + orderId + " của bạn " + friendlyStatus + ". Cảm ơn bạn đã mua sắm tại Rootie Vietnam.";
    }
}
