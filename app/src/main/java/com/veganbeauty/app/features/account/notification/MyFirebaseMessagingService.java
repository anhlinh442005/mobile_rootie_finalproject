package com.veganbeauty.app.features.account.notification;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.features.community.notification.CommunityNotificationHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "New FCM registration token generated: " + token);
        SharedPreferences prefs = getSharedPreferences("RootiePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("FCM_REGISTRATION_TOKEN", token).apply();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCM_MSG", "FCM Message received from: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) {
            if (remoteMessage.getNotification() != null) {
                String title = remoteMessage.getNotification().getTitle() != null ? remoteMessage.getNotification().getTitle() : "Thông báo từ Rootie";
                String body = remoteMessage.getNotification().getBody() != null ? remoteMessage.getNotification().getBody() : "";
                triggerDefaultRegularNotification(title, body);
            }
            return;
        }

        boolean isCommunity = Boolean.parseBoolean(data.get("isCommunity"));
        if (isCommunity) {
            handleCommunityNotification(data);
        } else {
            handleRegularNotification(data);
        }
    }

    private void handleCommunityNotification(Map<String, String> data) {
        String id = data.get("id") != null ? data.get("id") : UUID.randomUUID().toString();
        String userId = data.get("userId");
        String userName = data.get("userName") != null ? data.get("userName") : "Cộng đồng Rootie";
        String userAvatar = data.get("userAvatar");
        String type = data.get("type") != null ? data.get("type") : "POST";
        String actionType = data.get("actionType") != null ? data.get("actionType") : "COMMENT";
        String content = data.get("content") != null ? data.get("content") : "";
        String postId = data.get("postId");
        String commentId = data.get("commentId");

        String title = data.get("title") != null ? data.get("title") : "Hoạt động mới trong Cộng đồng";
        String body = data.get("body") != null ? data.get("body") : userName + " đã thực hiện hành động trong cộng đồng.";

        CommunityNotificationHelper.addCommunityNotification(
                getApplicationContext(),
                id,
                userId,
                userName,
                userAvatar,
                type,
                actionType,
                content,
                postId,
                commentId
        );

        String action = type.equalsIgnoreCase("CHAT") ? "open_community_message_list" : "open_community_notification_list";
        NotificationPushHelper.sendCommunityPushNotification(
                getApplicationContext(),
                id,
                title,
                body,
                action
        );
    }

    private void handleRegularNotification(Map<String, String> data) {
        String id = data.get("id") != null ? data.get("id") : UUID.randomUUID().toString();
        String title = data.get("title") != null ? data.get("title") : "Thông báo từ Rootie";
        String content = data.get("content") != null ? data.get("content") : "";
        String category = data.get("category") != null ? data.get("category") : "Khác";
        String tag = data.get("tag");
        String voucherCode = data.get("voucherCode");
        String actionText = data.get("actionText");
        String notificationType = data.get("notificationType") != null ? data.get("notificationType") : "general";
        String orderId = data.get("orderId");
        String scheduleId = data.get("scheduleId");
        String iconResName = data.get("iconResName") != null ? data.get("iconResName") : "ic_bell";

        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        NotificationItem notificationItem = new NotificationItem(
                id,
                title,
                content,
                currentDate,
                category,
                tag,
                voucherCode,
                actionText,
                false,
                "Hôm nay",
                iconResName,
                notificationType,
                orderId,
                scheduleId
        );

        NotificationRepository.getInstance(getApplicationContext()).addNotification(notificationItem);

        NotificationPushHelper.sendPushNotification(
                getApplicationContext(),
                id,
                title,
                content,
                notificationType,
                voucherCode,
                orderId,
                scheduleId
        );
    }

    private void triggerDefaultRegularNotification(String title, String body) {
        String id = UUID.randomUUID().toString();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        NotificationItem item = new NotificationItem(
                id,
                title,
                body,
                currentDate,
                "Khác",
                null,
                null,
                null,
                false,
                "Hôm nay",
                "ic_bell",
                "general",
                null,
                null
        );
        NotificationRepository.getInstance(getApplicationContext()).addNotification(item);
        NotificationPushHelper.sendPushNotification(
                getApplicationContext(),
                id,
                title,
                body,
                "general",
                null,
                null,
                null
        );
    }
}
