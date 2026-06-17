package com.veganbeauty.app.data.repository

import android.util.Log
import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.entities.OrderEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Mock Order Status Update handler.
 *
 * Whenever the status of an order changes, this helper is invoked. It is
 * deliberately a stand-in for a real backend "order_status_updated" webhook:
 *
 *  - If the order has a [OrderEntity.userId] of `null` (i.e. a guest checkout):
 *      - If [OrderEntity.billingEmail] is present, a simulated **Email** message
 *        is printed to the backend (Logcat) console.
 *      - Otherwise, a simulated **SMS** message is printed.
 *  - If the order belongs to a registered user, no outbound notification is
 *    sent from here — that responsibility is delegated to the user-facing
 *    notification preferences (see [com.veganbeauty.app.data.local.ProfileSession]).
 *
 * No real SMS gateway or email provider is contacted. All output is
 * written to `Logcat` under the [TAG] tag so the team can verify the
 * notification routing from the [Verification Plan] test cases.
 */
object OrderStatusNotifier {

    private const val TAG = "OrderStatusNotifier"

    /** Fire-and-forget status update. Safe to call from any coroutine context. */
    fun updateStatus(
        orderDao: OrderDao,
        orderId: String,
        newStatus: String,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        scope.launch {
            try {
                val updated = orderDao.updateOrderStatus(orderId, newStatus)
                if (updated <= 0) {
                    Log.w(TAG, "updateStatus: no order found for id=$orderId")
                    return@launch
                }
                simulateNotificationForOrder(orderDao, orderId, newStatus)
            } catch (e: Exception) {
                Log.e(TAG, "updateStatus failed for orderId=$orderId", e)
            }
        }
    }

    /**
     * Dispatch the mock SMS/Email notification only. The caller is expected
     * to have already persisted the new status on the order. Useful when
     * the status update and the notification dispatch live in the same
     * suspend transaction (e.g. inside
     * [com.veganbeauty.app.data.repository.OrderRepository.updateOrderStatus]).
     */
    suspend fun simulateOnly(orderDao: OrderDao, orderId: String, newStatus: String) {
        try {
            simulateNotificationForOrder(orderDao, orderId, newStatus)
        } catch (e: Exception) {
            Log.e(TAG, "simulateOnly failed for orderId=$orderId", e)
        }
    }

    private suspend fun simulateNotificationForOrder(
        orderDao: OrderDao,
        orderId: String,
        newStatus: String
    ) {
        val order = orderDao.getOrderById(orderId) ?: return
        simulateOutboundNotification(order, newStatus)
    }

    /**
     * Prints a mock SMS/Email notification to Logcat. In a real backend this
     * would POST to a transactional email/SMS provider; here we just log the
     * payload so the Android team can verify the routing decision.
     */
    private fun simulateOutboundNotification(order: OrderEntity, newStatus: String) {
        val isGuest = order.isGuest || order.userId.isNullOrBlank()
        if (!isGuest) {
            Log.i(TAG, "Order ${order.id} → $newStatus (user=${order.userId}, " +
                "in-app notification dispatched via user preferences)")
            return
        }
        val email = order.billingEmail?.trim().orEmpty()
        val phone = order.billingPhone?.trim().orEmpty()
        val name = order.billingName?.trim().orEmpty().ifEmpty { "Quý khách" }
        val body = buildMessageBody(order.id, newStatus)

        if (email.isNotEmpty()) {
            // Email routing
            Log.i(
                TAG,
                "[MOCK EMAIL → $email] To: $name <$email> | Subject: " +
                    "Cập nhật đơn hàng #${order.id} | Body: $body"
            )
        } else if (phone.isNotEmpty()) {
            // SMS routing
            Log.i(
                TAG,
                "[MOCK SMS → $phone] To: $name | Body: $body"
            )
        } else {
            Log.w(
                TAG,
                "Guest order ${order.id} has no email or phone on file; " +
                    "cannot route notification."
            )
        }
    }

    private fun buildMessageBody(orderId: String, status: String): String {
        val friendlyStatus = when (status) {
            "Đang xử lý", "Đã xác nhận" -> "đã được xác nhận và đang được chuẩn bị"
            "Đang giao hàng", "Đang vận chuyển" -> "đang được giao đến bạn"
            "Đã giao hàng", "Hoàn tất" -> "đã được giao thành công"
            "Đã hủy" -> "đã được hủy theo yêu cầu"
            else -> "đã được cập nhật trạng thái: $status"
        }
        return "Rootie: Đơn hàng #$orderId của bạn $friendlyStatus. Cảm ơn bạn đã mua sắm tại Rootie Vietnam."
    }
}
