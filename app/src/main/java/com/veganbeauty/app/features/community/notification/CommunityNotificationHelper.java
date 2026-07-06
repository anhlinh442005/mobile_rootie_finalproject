package com.veganbeauty.app.features.community.notification;

import android.content.Context;

import com.veganbeauty.app.data.repository.CommunityNotificationRepository;
import com.veganbeauty.app.utils.RootieBrandHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommunityNotificationHelper {

    public static void addCommunityNotificationForUser(
            Context context,
            String targetUserId,
            String id,
            String userId,
            String userName,
            String userAvatar,
            String type,
            String actionType,
            String content,
            String postId,
            String commentId
    ) {
        if (CommunityNotificationRepository.isGuest(context)) return;

        if (targetUserId == null || targetUserId.isEmpty()) return;

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        java.util.Map<String, Object> notificationMap = new java.util.HashMap<>();
        notificationMap.put("id", id);
        notificationMap.put("userId", userId != null ? userId : "");
        notificationMap.put("userName", userName);
        notificationMap.put("userAvatar", userAvatar != null ? userAvatar : RootieBrandHelper.AVATAR_URL);
        notificationMap.put("type", type);
        notificationMap.put("actionType", actionType);
        notificationMap.put("content", content);
        notificationMap.put("time", currentTime);
        notificationMap.put("date", currentDate);
        notificationMap.put("isRead", false);
        notificationMap.put("postId", postId != null ? postId : "");
        notificationMap.put("commentId", commentId != null ? commentId : "");
        
        // Asynchronously add to firestore so it doesn't block UI thread
        new Thread(() -> {
            new com.veganbeauty.app.data.remote.FirestoreService().addCommunityNotification(targetUserId, notificationMap);
        }).start();
    }

    public static void addCommunityNotificationLocalOnly(
            Context context,
            String targetUserId,
            String id,
            String userId,
            String userName,
            String userAvatar,
            String type,
            String actionType,
            String content,
            String postId,
            String commentId
    ) {
        if (CommunityNotificationRepository.isGuest(context)) return;
        if (targetUserId == null || targetUserId.isEmpty()) return;

        File file = new File(context.getFilesDir(), "local_notifications_" + targetUserId + ".json");
        List<ComNotificationItem> list = new ArrayList<>();

        if (file.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(file), java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                org.json.JSONArray jsonArray = new org.json.JSONArray(sb.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    list.add(new ComNotificationItem(
                            obj.getString("id"),
                            obj.optString("userId"),
                            obj.getString("userName"),
                            obj.optString("userAvatar"),
                            obj.getString("type"),
                            obj.getString("actionType"),
                            obj.getString("content"),
                            obj.getString("time"),
                            obj.getString("date"),
                            obj.optBoolean("isRead", false),
                            obj.optString("postId"),
                            obj.optString("commentId"),
                            ""
                    ));
                }
            } catch (Exception e) {}
        }

        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        String currentTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());

        ComNotificationItem newItem = new ComNotificationItem(
                id, userId, userName, userAvatar, type, actionType, content,
                currentTime, currentDate, false, postId, commentId, ""
        );

        list.removeIf(it -> it.getId().equals(id));
        list.add(0, newItem);

        try {
            org.json.JSONArray newArray = new org.json.JSONArray();
            for (ComNotificationItem item : list) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", item.getId());
                obj.put("userId", item.getUserId() != null ? item.getUserId() : "");
                obj.put("userName", item.getUserName());
                obj.put("userAvatar", item.getUserAvatar() != null ? item.getUserAvatar() : "");
                obj.put("type", item.getType());
                obj.put("actionType", item.getActionType());
                obj.put("content", item.getContent());
                obj.put("time", item.getTime());
                obj.put("date", item.getDate());
                obj.put("isRead", item.isRead());
                obj.put("postId", item.getPostId() != null ? item.getPostId() : "");
                obj.put("commentId", item.getCommentId() != null ? item.getCommentId() : "");
                newArray.put(obj);
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(newArray.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            CommunityNotificationRepository.getInstance(context).refresh();
        } catch (Exception e) {}
    }
}
