package com.veganbeauty.app.features.account.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.veganbeauty.app.MainActivity
import com.veganbeauty.app.R
import com.veganbeauty.app.data.local.ProfileSession

object NotificationPushHelper {

    fun sendPushNotification(
        context: Context,
        id: String,
        title: String,
        content: String,
        notificationType: String,
        voucherCode: String? = null,
        orderId: String? = null,
        scheduleId: String? = null
    ) {
        // Only send if user has enabled notifications in setting AND system allows it
        val isNotiEnabled = ProfileSession.isNotiEnabled(context)
        val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!isNotiEnabled || !systemNotificationsEnabled) {
            return
        }

        val notificationId = Math.abs(id.hashCode())

        val soundEnabled = ProfileSession.isSoundEnabled(context)
        val vibrateEnabled = ProfileSession.isVibrateEnabled(context)

        val channelId = "rootie_push_channel_${if (soundEnabled) "s1" else "s0"}_${if (vibrateEnabled) "v1" else "v0"}_h"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rootie Notifications"
            val descriptionText = "Kênh gửi thông báo từ Rootie"
            val importance = if (soundEnabled) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                if (!soundEnabled) {
                    setSound(null, null)
                }
                enableVibration(vibrateEnabled)
                if (!vibrateEnabled) {
                    vibrationPattern = longArrayOf(0L)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 1. PendingIntent for body click: Opens MainActivity and routes to correct screen
        val actionName = if (notificationType.equals("skin_chat", ignoreCase = true)) "open_skin_chat" else "open_notification_list"
        val bodyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("extra_notification_action", actionName)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val bodyPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            bodyIntent,
            pendingIntentFlags
        )

        // 2. Build Notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(bodyPendingIntent)

        // Set Sound/Vibration
        if (!soundEnabled) {
            builder.setSound(null)
            builder.priority = NotificationCompat.PRIORITY_LOW
        } else {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }
        if (!vibrateEnabled) {
            builder.setVibrate(longArrayOf(0L))
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        // 3. Action Button (e.g. COPY MÃ, CHI TIẾT, etc.)
        val actionText = when (notificationType.lowercase()) {
            "voucher" -> "COPY MÃ"
            "order" -> "CHI TIẾT"
            "skin care" -> "CHĂM DA NGAY"
            "checkin" -> "ĐIỂM DANH"
            "schedule date" -> "XEM LỊCH HẸN"
            else -> null
        }

        if (actionText != null) {
            val actionIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("extra_notification_action", "open_detail")
                putExtra("extra_notification_type", notificationType)
                putExtra("extra_voucher_code", voucherCode)
                putExtra("extra_order_id", orderId)
                putExtra("extra_schedule_id", scheduleId)
            }
            val actionPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                actionIntent,
                pendingIntentFlags
            )
            builder.addAction(0, actionText, actionPendingIntent)
        }

        // 4. "Bỏ qua" Action Button
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.veganbeauty.app.ACTION_SKIP"
            putExtra("extra_notification_id", notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            skipIntent,
            pendingIntentFlags
        )
        builder.addAction(0, "Bỏ qua", skipPendingIntent)

        // Show Notification
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun sendCommunityPushNotification(
        context: Context,
        id: String,
        title: String,
        content: String,
        actionName: String = "open_community_notification_list"
    ) {
        val isNotiEnabled = ProfileSession.isNotiEnabled(context)
        val systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!isNotiEnabled || !systemNotificationsEnabled) {
            return
        }

        val notificationId = Math.abs(id.hashCode())
        val soundEnabled = ProfileSession.isSoundEnabled(context)
        val vibrateEnabled = ProfileSession.isVibrateEnabled(context)

        val channelId = "rootie_community_channel_${if (soundEnabled) "s1" else "s0"}_${if (vibrateEnabled) "v1" else "v0"}_h"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Rootie Community Notifications"
            val descriptionText = "Kênh gửi thông báo cộng đồng từ Rootie"
            val importance = if (soundEnabled) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                if (!soundEnabled) {
                    setSound(null, null)
                }
                enableVibration(vibrateEnabled)
                if (!vibrateEnabled) {
                    vibrationPattern = longArrayOf(0L)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val bodyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("extra_notification_action", actionName)
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val bodyPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            bodyIntent,
            pendingIntentFlags
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setContentIntent(bodyPendingIntent)

        if (!soundEnabled) {
            builder.setSound(null)
            builder.priority = NotificationCompat.PRIORITY_LOW
        } else {
            builder.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION))
            builder.priority = NotificationCompat.PRIORITY_HIGH
        }
        if (!vibrateEnabled) {
            builder.setVibrate(longArrayOf(0L))
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE)
        }

        // Action Button: "Xem ngay"
        val viewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("extra_notification_action", "open_community_notification_list")
        }
        val viewPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1,
            viewIntent,
            pendingIntentFlags
        )
        builder.addAction(0, "Xem ngay", viewPendingIntent)

        // "Bỏ qua" Action Button
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.veganbeauty.app.ACTION_SKIP"
            putExtra("extra_notification_id", notificationId)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2,
            skipIntent,
            pendingIntentFlags
        )
        builder.addAction(0, "Bỏ qua", skipPendingIntent)

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
