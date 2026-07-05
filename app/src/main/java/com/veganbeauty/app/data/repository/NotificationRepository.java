package com.veganbeauty.app.data.repository;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.NotificationItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

public class NotificationRepository {

    private final Context context;
    private final LocalJsonReader localJsonReader;

    private final MutableStateFlow<List<NotificationItem>> _notifications = kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(new ArrayList<>());
    public final StateFlow<List<NotificationItem>> notifications = _notifications;

    private Set<String> getDeletedNotificationIds() {
        android.content.SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        return prefs.getStringSet("deleted_ids", new HashSet<>());
    }

    private void addDeletedNotificationId(String id) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        Set<String> deletedIds = new HashSet<>(prefs.getStringSet("deleted_ids", new HashSet<>()));
        deletedIds.add(id);
        prefs.edit().putStringSet("deleted_ids", deletedIds).apply();
    }

    private final MutableStateFlow<Integer> _unreadCount = kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(0);

    private NotificationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.localJsonReader = new LocalJsonReader(this.context);
        refreshNotifications();
    }

    public Flow<Integer> getUnreadCount() {
        return _unreadCount;
    }

    private void updateUnreadCount(List<NotificationItem> list) {
        int count = 0;
        if (list != null) {
            for (NotificationItem item : list) {
                if (!item.isRead()) {
                    count++;
                }
            }
        }
        _unreadCount.setValue(count);
    }

    public void refreshNotifications() {
        List<NotificationItem> assetList = localJsonReader.getAllNotifications();
        Set<String> deletedIds = getDeletedNotificationIds();
        
        List<NotificationItem> filteredAssetList = new ArrayList<>();
        for (NotificationItem item : assetList) {
            if (!deletedIds.contains(item.getId())) {
                filteredAssetList.add(item);
            }
        }
        assetList = filteredAssetList;

        List<NotificationItem> localList = loadNotificationsFromLocal();
        List<NotificationItem> filteredLocalList = new ArrayList<>();
        for (NotificationItem item : localList) {
            if (!deletedIds.contains(item.getId())) {
                filteredLocalList.add(item);
            }
        }
        localList = filteredLocalList;

        List<NotificationItem> finalList;
        if (localList.isEmpty()) {
            finalList = assetList;
        } else {
            Map<String, NotificationItem> assetMap = new HashMap<>();
            for (NotificationItem item : assetList) {
                assetMap.put(item.getId(), item);
            }

            List<NotificationItem> updatedList = new ArrayList<>();
            for (NotificationItem localItem : localList) {
                NotificationItem assetItem = assetMap.get(localItem.getId());
                if (assetItem != null) {
                    updatedList.add(new NotificationItem(
                            assetItem.getId(), assetItem.getTitle(), assetItem.getContent(), assetItem.getTime(),
                            assetItem.getCategory(), assetItem.getTag(), assetItem.getVoucherCode(),
                            assetItem.getActionText(), localItem.isRead(), assetItem.getSection(),
                            assetItem.getIconResName(), assetItem.getNotificationType(),
                            assetItem.getOrderId(), assetItem.getScheduleId()
                    ));
                } else {
                    updatedList.add(localItem);
                }
            }

            Set<String> localIds = new HashSet<>();
            for (NotificationItem item : localList) localIds.add(item.getId());
            for (NotificationItem assetItem : assetList) {
                if (!localIds.contains(assetItem.getId())) updatedList.add(assetItem);
            }
            finalList = updatedList;
        }

        _notifications.setValue(finalList);
        updateUnreadCount(finalList);
        saveNotificationsToLocal(finalList);
    }

    private List<NotificationItem> loadNotificationsFromLocal() {
        File file = new File(context.getFilesDir(), "local_account_notifications.json");
        if (!file.exists()) return new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray jsonArray = new JSONArray(sb.toString());
            List<NotificationItem> list = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                list.add(new NotificationItem(
                        obj.getString("id"),
                        obj.getString("title"),
                        obj.getString("content"),
                        obj.getString("time"),
                        obj.getString("category"),
                        obj.optString("tag", "").isEmpty() ? null : obj.optString("tag", ""),
                        obj.optString("voucherCode", "").isEmpty() ? null : obj.optString("voucherCode", ""),
                        obj.optString("actionText", "").isEmpty() ? null : obj.optString("actionText", ""),
                        obj.optBoolean("isRead", false),
                        obj.optString("section", "Hôm nay"),
                        obj.getString("iconResName"),
                        obj.optString("notificationType", "").isEmpty() ? null : obj.optString("notificationType", ""),
                        obj.optString("orderId", "").isEmpty() ? null : obj.optString("orderId", ""),
                        obj.optString("scheduleId", "").isEmpty() ? null : obj.optString("scheduleId", "")
                ));
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void saveNotificationsToLocal(List<NotificationItem> list) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (NotificationItem item : list) {
                JSONObject obj = new JSONObject();
                obj.put("id", item.getId());
                obj.put("title", item.getTitle());
                obj.put("content", item.getContent());
                obj.put("time", item.getTime());
                obj.put("category", item.getCategory());
                obj.put("tag", item.getTag() != null ? item.getTag() : "");
                obj.put("voucherCode", item.getVoucherCode() != null ? item.getVoucherCode() : "");
                obj.put("actionText", item.getActionText() != null ? item.getActionText() : "");
                obj.put("isRead", item.isRead());
                obj.put("section", item.getSection());
                obj.put("iconResName", item.getIconResName());
                obj.put("notificationType", item.getNotificationType() != null ? item.getNotificationType() : "");
                obj.put("orderId", item.getOrderId() != null ? item.getOrderId() : "");
                obj.put("scheduleId", item.getScheduleId() != null ? item.getScheduleId() : "");
                jsonArray.put(obj);
            }
            File file = new File(context.getFilesDir(), "local_account_notifications.json");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markAsRead(String id) {
        List<NotificationItem> currentList = _notifications.getValue();
        List<NotificationItem> updatedList = new ArrayList<>();

        for (NotificationItem item : currentList) {
            if (item.getId().equals(id)) {
                updatedList.add(new NotificationItem(
                        item.getId(), item.getTitle(), item.getContent(), item.getTime(), item.getCategory(),
                        item.getTag(), item.getVoucherCode(), item.getActionText(), true, item.getSection(),
                        item.getIconResName(), item.getNotificationType(), item.getOrderId(), item.getScheduleId()
                ));
            } else {
                updatedList.add(item);
            }
        }
        _notifications.setValue(updatedList);
        updateUnreadCount(updatedList);
        saveNotificationsToLocal(updatedList);
    }

    public void markAllAsRead() {
        List<NotificationItem> currentList = _notifications.getValue();
        List<NotificationItem> updatedList = new ArrayList<>();

        for (NotificationItem item : currentList) {
            updatedList.add(new NotificationItem(
                    item.getId(), item.getTitle(), item.getContent(), item.getTime(), item.getCategory(),
                    item.getTag(), item.getVoucherCode(), item.getActionText(), true, item.getSection(),
                    item.getIconResName(), item.getNotificationType(), item.getOrderId(), item.getScheduleId()
            ));
        }
        _notifications.setValue(updatedList);
        updateUnreadCount(updatedList);
        saveNotificationsToLocal(updatedList);
    }

    public void deleteNotification(String id) {
        addDeletedNotificationId(id);
        List<NotificationItem> currentList = new ArrayList<>(_notifications.getValue());
        currentList.removeIf(item -> item.getId().equals(id));
        _notifications.setValue(currentList);
        updateUnreadCount(currentList);
        saveNotificationsToLocal(currentList);
    }

    public void deleteAllNotifications() {
        List<NotificationItem> currentList = _notifications.getValue();
        if (currentList != null) {
            for (NotificationItem item : currentList) {
                addDeletedNotificationId(item.getId());
            }
        }
        List<NotificationItem> emptyList = new ArrayList<>();
        _notifications.setValue(emptyList);
        updateUnreadCount(emptyList);
        saveNotificationsToLocal(emptyList);
    }

    public void addNotification(NotificationItem item) {
        List<NotificationItem> currentList = new ArrayList<>(_notifications.getValue());
        currentList.removeIf(it -> it.getId().equals(item.getId()));
        currentList.add(0, item);
        _notifications.setValue(currentList);
        updateUnreadCount(currentList);
        saveNotificationsToLocal(currentList);
    }

    private static volatile NotificationRepository INSTANCE;

    public static NotificationRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (NotificationRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NotificationRepository(context);
                }
            }
        }
        return INSTANCE;
    }
}
