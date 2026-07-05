package com.veganbeauty.app.data.repository;

import android.content.Context;

import com.veganbeauty.app.utils.ProfileSessionHelper;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

/**
 * Tracks the unread count for community notifications, scoped per logged in user.
 * The notification list/content itself is still owned by {@link com.veganbeauty.app.features.community.notification.ComNotificationViewModel}
 * and {@link com.veganbeauty.app.features.community.notification.CommunityNotificationHelper}; this repository
 * only centralizes the per-user file naming and exposes a reactive unread count so it can never be
 * conflated with the general (account) notification unread count.
 */
public class CommunityNotificationRepository {

    private final Context context;
    private String currentUserId;

    private final MutableStateFlow<Integer> _unreadCount = StateFlowKt.MutableStateFlow(0);

    private CommunityNotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.currentUserId = resolveUserId(this.context);
        refresh();
    }

    public Flow<Integer> getUnreadCount() {
        return _unreadCount;
    }

    /** Empty means "khách vãng lai" (not logged in) — such sessions must never see any notification. */
    public static String resolveUserId(Context context) {
        String userId = ProfileSessionHelper.getEffectiveUserId(context);
        return userId == null ? "" : userId.trim();
    }

    public static boolean isGuest(Context context) {
        return resolveUserId(context).isEmpty();
    }

    public static File getNotificationsFile(Context context) {
        return new File(context.getFilesDir(), "local_notifications_" + resolveUserId(context) + ".json");
    }

    private void ensureUserScope() {
        String userId = resolveUserId(context);
        if (!userId.equals(currentUserId)) {
            currentUserId = userId;
            refresh();
        }
    }

    public void refresh() {
        _unreadCount.setValue(countUnread());
    }

    private int countUnread() {
        if (currentUserId == null || currentUserId.isEmpty()) return 0;
        try {
            String json;
            File file = getNotificationsFile(context);
            if (file.exists()) {
                json = readFile(file);
            } else {
                json = readAsset("notification_com.json");
            }
            if (json == null) return 0;

            JSONArray array = new JSONArray(json);
            int count = 0;
            for (int i = 0; i < array.length(); i++) {
                if (!array.getJSONObject(i).optBoolean("isRead", false)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String readFile(File file) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String readAsset(String assetName) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(assetName), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static volatile CommunityNotificationRepository INSTANCE;

    public static CommunityNotificationRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (CommunityNotificationRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CommunityNotificationRepository(context);
                }
            }
        }
        INSTANCE.ensureUserScope();
        return INSTANCE;
    }
}
