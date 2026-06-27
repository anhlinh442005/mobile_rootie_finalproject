package com.veganbeauty.app.features.account.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;

public class NotificationPushHelper {

    public static void sendPushNotification(
            Context context,
            String id,
            String title,
            String content,
            String notificationType,
            String voucherCode,
            String orderId,
            String scheduleId
    ) {
        boolean isNotiEnabled = ProfileSession.isNotiEnabled(context);
        boolean systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        if (!isNotiEnabled || !systemNotificationsEnabled) {
            return;
        }

        int notificationId = Math.abs(id.hashCode());

        boolean soundEnabled = ProfileSession.isSoundEnabled(context);
        boolean vibrateEnabled = ProfileSession.isVibrateEnabled(context);

        String channelId = "rootie_push_channel_" + (soundEnabled ? "s1" : "s0") + "_" + (vibrateEnabled ? "v1" : "v0") + "_h";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Rootie Notifications";
            String descriptionText = "Kênh gửi thông báo từ Rootie";
            int importance = soundEnabled ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(descriptionText);
            if (!soundEnabled) {
                channel.setSound(null, null);
            }
            channel.enableVibration(vibrateEnabled);
            if (!vibrateEnabled) {
                channel.setVibrationPattern(new long[]{0L});
            }
            notificationManager.createNotificationChannel(channel);
        }

        String actionName = "skin_chat".equalsIgnoreCase(notificationType) ? "open_skin_chat" : "open_notification_list";
        Intent bodyIntent = new Intent(context, MainActivity.class);
        bodyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        bodyIntent.putExtra("extra_notification_action", actionName);

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE) :
                PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent bodyPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                bodyIntent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(bodyPendingIntent);

        if (!soundEnabled) {
            builder.setSound(null);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (!vibrateEnabled) {
            builder.setVibrate(new long[]{0L});
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        String actionText = null;
        if (notificationType != null) {
            switch (notificationType.toLowerCase()) {
                case "voucher": actionText = "COPY MÃ"; break;
                case "order": actionText = "CHI TIẾT"; break;
                case "skin care": actionText = "CHĂM DA NGAY"; break;
                case "checkin": actionText = "ĐIỂM DANH"; break;
                case "schedule date": actionText = "XEM LỊCH HẸN"; break;
            }
        }

        if (actionText != null) {
            Intent actionIntent = new Intent(context, MainActivity.class);
            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            actionIntent.putExtra("extra_notification_action", "open_detail");
            actionIntent.putExtra("extra_notification_type", notificationType);
            actionIntent.putExtra("extra_voucher_code", voucherCode);
            actionIntent.putExtra("extra_order_id", orderId);
            actionIntent.putExtra("extra_schedule_id", scheduleId);

            PendingIntent actionPendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId + 1,
                    actionIntent,
                    pendingIntentFlags
            );
            builder.addAction(0, actionText, actionPendingIntent);
        }

        Intent skipIntent = new Intent(context, NotificationActionReceiver.class);
        skipIntent.setAction("com.veganbeauty.app.ACTION_SKIP");
        skipIntent.putExtra("extra_notification_id", notificationId);

        PendingIntent skipPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                skipIntent,
                pendingIntentFlags
        );
        builder.addAction(0, "Bỏ qua", skipPendingIntent);

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public static void sendCommunityPushNotification(
            Context context,
            String id,
            String title,
            String content,
            String actionName
    ) {
        boolean isNotiEnabled = ProfileSession.isNotiEnabled(context);
        boolean systemNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        if (!isNotiEnabled || !systemNotificationsEnabled) {
            return;
        }

        int notificationId = Math.abs(id.hashCode());
        boolean soundEnabled = ProfileSession.isSoundEnabled(context);
        boolean vibrateEnabled = ProfileSession.isVibrateEnabled(context);

        String channelId = "rootie_community_channel_" + (soundEnabled ? "s1" : "s0") + "_" + (vibrateEnabled ? "v1" : "v0") + "_h";
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Rootie Community Notifications";
            String descriptionText = "Kênh gửi thông báo cộng đồng từ Rootie";
            int importance = soundEnabled ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(descriptionText);
            if (!soundEnabled) {
                channel.setSound(null, null);
            }
            channel.enableVibration(vibrateEnabled);
            if (!vibrateEnabled) {
                channel.setVibrationPattern(new long[]{0L});
            }
            notificationManager.createNotificationChannel(channel);
        }

        Intent bodyIntent = new Intent(context, MainActivity.class);
        bodyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        bodyIntent.putExtra("extra_notification_action", actionName != null ? actionName : "open_community_notification_list");

        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                (PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE) :
                PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent bodyPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                bodyIntent,
                pendingIntentFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(bodyPendingIntent);

        if (!soundEnabled) {
            builder.setSound(null);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
        } else {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        if (!vibrateEnabled) {
            builder.setVibrate(new long[]{0L});
        } else {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        Intent viewIntent = new Intent(context, MainActivity.class);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        viewIntent.putExtra("extra_notification_action", "open_community_notification_list");

        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                viewIntent,
                pendingIntentFlags
        );
        builder.addAction(0, "Xem ngay", viewPendingIntent);

        Intent skipIntent = new Intent(context, NotificationActionReceiver.class);
        skipIntent.setAction("com.veganbeauty.app.ACTION_SKIP");
        skipIntent.putExtra("extra_notification_id", notificationId);

        PendingIntent skipPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 2,
                skipIntent,
                pendingIntentFlags
        );
        builder.addAction(0, "Bỏ qua", skipPendingIntent);

        try {
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
