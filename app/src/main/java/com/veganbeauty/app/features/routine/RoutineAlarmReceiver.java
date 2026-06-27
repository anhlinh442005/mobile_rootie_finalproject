package com.veganbeauty.app.features.routine;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;

public class RoutineAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "RoutineAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("REMINDER_TYPE");
        if (type == null) type = "MORNING";
        
        boolean isLead = intent.getBooleanExtra("IS_LEAD_REMINDER", false);
        String channelId = "skin_routine_channel";
        int notificationId = type.equals("MORNING") ? 1001 : 1002;

        Log.d(TAG, "onReceive triggered. Type: " + type + ", isLead: " + isLead);

        boolean notiAllowed = ProfileSession.isNotiEnabled(context);
        boolean routineEnabled = type.equals("MORNING") ? 
                ProfileSession.isMorningReminderEnabled(context) : 
                ProfileSession.isEveningReminderEnabled(context);

        if (!notiAllowed || !routineEnabled) {
            Log.d(TAG, "Notifications are disabled. Rescheduling and returning.");
            RoutineAlarmScheduler.rescheduleAlarms(context);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasPermission = ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
            
            if (!hasPermission) {
                new Handler(Looper.getMainLooper()).post(() -> 
                    Toast.makeText(context, "Rootie: Vui lòng cấp quyền thông báo trong Cài đặt thiết bị để hiển thị nhắc nhở!", Toast.LENGTH_LONG).show()
                );
                Log.w(TAG, "POST_NOTIFICATIONS permission NOT granted!");
                RoutineAlarmScheduler.rescheduleAlarms(context);
                return;
            }
        }

        String titleText;
        String messageText;

        if (type.equals("MORNING")) {
            titleText = isLead ? "Routine buổi sáng sắp đến" : "Đến giờ chăm da buổi sáng rồi✨";
            messageText = isLead ? "Routine chăm da buổi sáng sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!" : 
                                   "Mở Rootie và bắt đầu quy trình giúp làn da của bạn luôn rạng rỡ hôm nay.";
        } else {
            titleText = isLead ? "Routine buổi tối sắp đến" : "Đến giờ chăm da buổi tối rồi✨";
            messageText = isLead ? "Routine chăm da buổi tối sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!" : 
                                   "Mở Rootie và bắt đầu quy trình giúp làn da của bạn phục hồi hôm nay.";
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Nhắc nhở Chăm sóc da",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh gửi thông báo nhắc nhở routine sáng và tối");
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainIntent.putExtra("NAVIGATE_TO", "SKIN_REMINDER");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Bitmap appIconBitmap = null;
        try {
            appIconBitmap = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
        } catch (Exception e) {
            // ignored
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleText)
                .setContentText(messageText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (appIconBitmap != null) {
            builder.setLargeIcon(appIconBitmap);
        }

        try {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Standard notification posted successfully. ID: " + notificationId);

            new Handler(Looper.getMainLooper()).post(() -> {
                String timeLabel = isLead ? "Nhắc trước 15 phút" : "Đúng giờ";
                Toast.makeText(context, "Đã gửi thông báo: " + timeLabel, Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to show standard notification", e);
        } finally {
            RoutineAlarmScheduler.rescheduleAlarms(context);
        }
    }
}
