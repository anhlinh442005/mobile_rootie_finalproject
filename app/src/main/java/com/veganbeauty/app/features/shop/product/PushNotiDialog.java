package com.veganbeauty.app.features.shop.product;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;

public class PushNotiDialog {

    private static final String TAG = "PushNotiDialog";

    public interface OnContinueListener {
        void onContinue();
    }

    public interface OnDismissListener {
        void onDismiss();
    }

    public static void show(Context context, String orderCode, String recipientName, boolean isEmailNotification,
                            OnContinueListener onContinue, OnDismissListener onDismiss, Fragment fragment) {
        boolean isLoggedIn = ProfileSession.isLoggedIn(context);
        if (!isLoggedIn) {
            triggerSystemNotification(context, orderCode, recipientName, isEmailNotification);
        }

        if (onContinue != null) {
            onContinue.onContinue();
        }
    }

    public static void show(Context context, String orderCode, String recipientName, boolean isEmailNotification,
                            OnContinueListener onContinue) {
        show(context, orderCode, recipientName, isEmailNotification, onContinue, null, null);
    }

    private static void triggerSystemNotification(Context context, String orderCode, String recipientName, boolean isEmailNotification) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "rootie_checkout_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Thông báo đơn hàng";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription("Thông báo trạng thái đặt hàng tại Rootie");
            notificationManager.createNotificationChannel(channel);
        }

        String greeting = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName + " ơi, " : "";

        String titleText;
        String messageText;

        if (isEmailNotification) {
            titleText = "Xác nhận đặt hàng qua Email";
            messageText = greeting + "chúng tôi đã nhận được đơn hàng #" + orderCode + ". Xác nhận đã được gửi qua email của bạn.";
            Log.i(TAG, "[MOCK EMAIL] Order: " + orderCode + " | Recipient: " + recipientName + " | Title: " + titleText + " | Body: " + messageText);
        } else {
            titleText = "Xác nhận đặt hàng qua SMS";
            messageText = greeting + "cam on ban da dat hang tai Rootie. Ma don hang: #" + orderCode + ". Don hang dang duoc chuan bi.";
            Log.i(TAG, "[MOCK SMS] Order: " + orderCode + " | Recipient: " + recipientName + " | Title: " + titleText + " | Body: " + messageText);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(titleText)
                .setContentText(messageText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
