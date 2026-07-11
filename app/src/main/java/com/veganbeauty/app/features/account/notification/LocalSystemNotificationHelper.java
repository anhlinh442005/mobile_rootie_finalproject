package com.veganbeauty.app.features.account.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.repository.NotificationRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LocalSystemNotificationHelper {

    private static final String TAG = "LocalSystemNotiHelper";

    private LocalSystemNotificationHelper() {
    }

    public static boolean canPost(Context context) {
        if (context == null) {
            return false;
        }
        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isNotiEnabled(appContext)) {
            return false;
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    appContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void dispatch(
            Context context,
            String stableId,
            String title,
            String content,
            String category,
            String notificationType,
            String navigateTo,
            String actionText
    ) {
        dispatch(context, stableId, title, content, category, notificationType, navigateTo, actionText, false);
    }

    public static void dispatch(
            Context context,
            String stableId,
            String title,
            String content,
            String category,
            String notificationType,
            String navigateTo,
            String actionText,
            boolean allowDuplicateToday
    ) {
        if (context == null || stableId == null || stableId.trim().isEmpty()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (!canPost(appContext)) {
            Log.w(TAG, "Skipped notification " + stableId + " because posting is not allowed");
            return;
        }

        NotificationRepository repository = NotificationRepository.getInstance(appContext);
        if (!allowDuplicateToday && repository.hasNotificationId(stableId)) {
            Log.d(TAG, "Notification already recorded today: " + stableId);
            return;
        }

        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        NotificationItem item = new NotificationItem(
                stableId,
                title,
                content,
                now,
                category != null ? category : "Khác",
                null,
                null,
                actionText,
                false,
                NotificationDateHelper.getSectionFromTime(now),
                "ic_bell",
                notificationType,
                null,
                null
        );
        repository.addNotification(item);
        postSystemNotification(appContext, stableId, title, content, navigateTo);
        Log.i(TAG, "Dispatched notification: " + stableId);
    }

    public static String dailyStableId(String prefix) {
        String day = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return prefix + "_" + day;
    }

    private static void postSystemNotification(
            Context context,
            String stableId,
            String title,
            String content,
            String navigateTo
    ) {
        boolean soundEnabled = ProfileSession.isSoundEnabled(context);
        boolean vibrateEnabled = ProfileSession.isVibrateEnabled(context);
        String channelId = "rootie_local_channel_" + (soundEnabled ? "s1" : "s0") + "_" + (vibrateEnabled ? "v1" : "v0");
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Thông báo Rootie",
                    soundEnabled ? NotificationManager.IMPORTANCE_HIGH : NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Thông báo thời tiết, routine và ưu đãi gần bạn");
            channel.enableVibration(vibrateEnabled);
            if (!soundEnabled) {
                channel.setSound(null, null);
            }
            notificationManager.createNotificationChannel(channel);
        }

        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (navigateTo != null && !navigateTo.trim().isEmpty()) {
            mainIntent.putExtra("NAVIGATE_TO", navigateTo);
        } else {
            mainIntent.putExtra("extra_notification_action", "open_notification_list");
        }

        int notificationId = Math.abs(stableId.hashCode());
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        } else {
            builder.setSound(null);
        }
        if (vibrateEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        } else {
            builder.setVibrate(new long[]{0L});
        }

        try {
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to post system notification " + stableId, e);
        }
    }
}
