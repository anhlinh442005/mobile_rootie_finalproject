package com.veganbeauty.app.features.account.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.veganbeauty.app.data.local.entities.NotificationItem
import com.veganbeauty.app.data.repository.NotificationRepository
import com.veganbeauty.app.features.community.notification.CommunityNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_TOKEN", "New FCM registration token generated: $token")
        val prefs = getSharedPreferences("RootiePrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("FCM_REGISTRATION_TOKEN", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("FCM_MSG", "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isEmpty()) {
            remoteMessage.notification?.let {
                val title = it.title ?: "Thông báo từ Rootie"
                val body = it.body ?: ""
                triggerDefaultRegularNotification(title, body)
            }
            return
        }

        val isCommunity = data["isCommunity"]?.toBoolean() ?: false
        if (isCommunity) {
            handleCommunityNotification(data)
        } else {
            handleRegularNotification(data)
        }
    }

    private fun handleCommunityNotification(data: Map<String, String>) {
        val id = data["id"] ?: UUID.randomUUID().toString()
        val userId = data["userId"]
        val userName = data["userName"] ?: "Cộng đồng Rootie"
        val userAvatar = data["userAvatar"]
        val type = data["type"] ?: "POST"
        val actionType = data["actionType"] ?: "COMMENT"
        val content = data["content"] ?: ""
        val postId = data["postId"]
        val commentId = data["commentId"]

        val title = data["title"] ?: "Hoạt động mới trong Cộng đồng"
        val body = data["body"] ?: "$userName đã thực hiện hành động trong cộng đồng."

        // 1. Save locally so it appears in the Community Notification UI
        CommunityNotificationHelper.addCommunityNotification(
            context = applicationContext,
            id = id,
            userId = userId,
            userName = userName,
            userAvatar = userAvatar,
            type = type,
            actionType = actionType,
            content = content,
            postId = postId,
            commentId = commentId
        )

        // 2. Trigger push notification
        NotificationPushHelper.sendCommunityPushNotification(
            context = applicationContext,
            id = id,
            title = title,
            content = body
        )
    }

    private fun handleRegularNotification(data: Map<String, String>) {
        val id = data["id"] ?: UUID.randomUUID().toString()
        val title = data["title"] ?: "Thông báo từ Rootie"
        val content = data["content"] ?: ""
        val category = data["category"] ?: "Khác"
        val tag = data["tag"]
        val voucherCode = data["voucherCode"]
        val actionText = data["actionText"]
        val notificationType = data["notificationType"] ?: "general"
        val orderId = data["orderId"]
        val scheduleId = data["scheduleId"]
        val iconResName = data["iconResName"] ?: "ic_notification"

        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val notificationItem = NotificationItem(
            id = id,
            title = title,
            content = content,
            time = currentDate,
            category = category,
            tag = tag,
            voucherCode = voucherCode,
            actionText = actionText,
            isRead = false,
            section = "Hôm nay",
            iconResName = iconResName,
            notificationType = notificationType,
            orderId = orderId,
            scheduleId = scheduleId
        )

        // 1. Add to local list and save
        NotificationRepository.getInstance(applicationContext).addNotification(notificationItem)

        // 2. Trigger push notification
        NotificationPushHelper.sendPushNotification(
            context = applicationContext,
            id = id,
            title = title,
            content = content,
            notificationType = notificationType,
            voucherCode = voucherCode,
            orderId = orderId,
            scheduleId = scheduleId
        )
    }

    private fun triggerDefaultRegularNotification(title: String, body: String) {
        val id = UUID.randomUUID().toString()
        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val item = NotificationItem(
            id = id,
            title = title,
            content = body,
            time = currentDate,
            category = "Khác",
            tag = null,
            voucherCode = null,
            actionText = null,
            isRead = false,
            section = "Hôm nay",
            iconResName = "ic_notification",
            notificationType = "general",
            orderId = null,
            scheduleId = null
        )
        NotificationRepository.getInstance(applicationContext).addNotification(item)
        NotificationPushHelper.sendPushNotification(
            context = applicationContext,
            id = id,
            title = title,
            content = body,
            notificationType = "general"
        )
    }
}
