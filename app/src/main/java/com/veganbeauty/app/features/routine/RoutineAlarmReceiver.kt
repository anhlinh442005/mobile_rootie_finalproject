package com.veganbeauty.app.features.routine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.veganbeauty.app.MainActivity
import com.veganbeauty.app.R

class RoutineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("REMINDER_TYPE") ?: "MORNING"
        val channelId = "skin_routine_channel"
        val notificationId = if (type == "MORNING") 1001 else 1002

        val title = if (type == "MORNING") {
            "🌅 Thời gian dưỡng da buổi sáng!"
        } else {
            "🌙 Thời gian dưỡng da buổi tối!"
        }

        val message = if (type == "MORNING") {
            "Đã đến 06:30 AM rồi. Hãy bắt đầu Routine sáng để bảo vệ da cả ngày nhé!"
        } else {
            "Đã đến 09:45 PM rồi. Hãy thực hiện Routine tối để phục hồi làn da thôi nào!"
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

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
