package com.veganbeauty.app.features.routine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.veganbeauty.app.MainActivity
import com.veganbeauty.app.R

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("REMINDER_TYPE") ?: "MORNING"
        val isLead = intent.getBooleanExtra("IS_LEAD_REMINDER", false)
        val channelId = "skin_routine_channel"
        val notificationId = if (type == "MORNING") 1001 else 1002

        Log.d("RoutineAlarmReceiver", "onReceive triggered. Type: $type, isLead: $isLead")

        val notiAllowed = com.veganbeauty.app.data.local.ProfileSession.isNotiEnabled(context)
        val routineEnabled = if (type == "MORNING") {
            com.veganbeauty.app.data.local.ProfileSession.isMorningReminderEnabled(context)
        } else {
            com.veganbeauty.app.data.local.ProfileSession.isEveningReminderEnabled(context)
        }

        if (!notiAllowed || !routineEnabled) {
            Log.d("RoutineAlarmReceiver", "Notifications are disabled. Rescheduling and returning.")
            RoutineAlarmScheduler.rescheduleAlarms(context)
            return
        }

        // 1. Check Notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(
                        context,
                        "Rootie: Vui lòng cấp quyền thông báo trong Cài đặt thiết bị để hiển thị nhắc nhở!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                Log.w("RoutineAlarmReceiver", "POST_NOTIFICATIONS permission NOT granted!")
                RoutineAlarmScheduler.rescheduleAlarms(context)
                return
            }
        }

        // 2. Set native text according to Figma mockups
        val titleText = if (type == "MORNING") {
            if (isLead) "Routine buổi sáng sắp đến" else "Đến giờ chăm da buổi sáng rồi✨"
        } else {
            if (isLead) "Routine buổi tối sắp đến" else "Đến giờ chăm da buổi tối rồi✨"
        }

        val messageText = if (type == "MORNING") {
            if (isLead) {
                "Routine chăm da buổi sáng sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!"
            } else {
                "Mở Rootie và bắt đầu quy trình giúp làn da của bạn luôn rạng rỡ hôm nay."
            }
        } else {
            if (isLead) {
                "Routine chăm da buổi tối sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!"
            } else {
                "Mở Rootie và bắt đầu quy trình giúp làn da của bạn phục hồi hôm nay."
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở Chăm sóc da",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh gửi thông báo nhắc nhở routine sáng và tối"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "SKIN_REMINDER")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Decode App Launcher Icon to use as Large Icon (looks completely natural on VIVO/OriginOS)
        val appIconBitmap = try {
            BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Status bar monochrome icon
            .setContentTitle(titleText)
            .setContentText(messageText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText)) // Auto expand for full message
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (appIconBitmap != null) {
            builder.setLargeIcon(appIconBitmap)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d("RoutineAlarmReceiver", "Standard notification posted successfully. ID: $notificationId")
            
            Handler(Looper.getMainLooper()).post {
                val timeLabel = if (isLead) "Nhắc trước 15 phút" else "Đúng giờ"
                Toast.makeText(context, "Đã gửi thông báo: $timeLabel", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("RoutineAlarmReceiver", "Failed to show standard notification", e)
        } finally {
            RoutineAlarmScheduler.rescheduleAlarms(context)
        }
    }
}
