package com.veganbeauty.app.features.shop.product

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.veganbeauty.app.data.local.ProfileSession

/**
 * Mock push-notification handler.
 *
 * Instead of showing a blocking dialog, we post a real Android System Notification
 * (push-notification style) for guest checkouts (khách vãng lai), and immediately
 * navigate to the success screen. For logged-in users, we directly navigate to the
 * success screen without any notification.
 *
 * Guest notification types:
 * - Email: shown when guest provided email address
 * - SMS: shown when guest only provided phone (no email)
 */
object PushNotiDialog {

    private const val TAG = "PushNotiDialog"

    /**
     * Trigger the push-notification if the user is a guest, and advance to success screen.
     *
     * @param orderCode the order id (without the leading `#`) the
     *  push noti body should reference.
     * @param recipientName optional name for personalization.
     * @param isEmailNotification if true, shows as email notification; if false, shows as SMS.
     * @param onContinue fired to advance to the success screen.
     * @param onDismiss unused fallback callback to keep signature compatibility.
     * @param fragment optional fragment reference for banner display.
     */
    fun show(
        context: Context,
        orderCode: String,
        recipientName: String? = null,
        isEmailNotification: Boolean = false,
        onContinue: () -> Unit,
        @Suppress("UNUSED_PARAMETER") onDismiss: () -> Unit = {},
        fragment: androidx.fragment.app.Fragment? = null
    ) {
        val isLoggedIn = ProfileSession.isLoggedIn(context)
        if (!isLoggedIn) {
            triggerSystemNotification(context, orderCode, recipientName, isEmailNotification)
        }

        // Immediately trigger navigation to the success screen
        onContinue()
    }

    private fun triggerSystemNotification(
        context: Context,
        orderCode: String,
        recipientName: String?,
        isEmailNotification: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "rootie_checkout_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Thông báo đơn hàng"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Thông báo trạng thái đặt hàng tại Rootie"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val greeting = if (!recipientName.isNullOrBlank()) "$recipientName ơi, " else ""

        // Customize message based on notification type
        val titleText: String
        val messageText: String

        if (isEmailNotification) {
            // Email notification style
            titleText = "Xác nhận đặt hàng qua Email"
            messageText = "${greeting}chúng tôi đã nhận được đơn hàng #$orderCode. " +
                    "Xác nhận đã được gửi qua email của bạn."
            Log.i(
                TAG,
                "[MOCK EMAIL] Order: $orderCode | Recipient: $recipientName | " +
                        "Title: $titleText | Body: $messageText"
            )
        } else {
            // SMS notification style
            titleText = "Xác nhận đặt hàng qua SMS"
            messageText = "${greeting}cam on ban da dat hang tai Rootie. " +
                    "Ma don hang: #$orderCode. Don hang dang duoc chuan bi."
            Log.i(
                TAG,
                "[MOCK SMS] Order: $orderCode | Recipient: $recipientName | " +
                        "Title: $titleText | Body: $messageText"
            )
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.veganbeauty.app.R.drawable.ic_notification)
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}

