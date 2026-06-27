package com.veganbeauty.app.features.community.notification;

import android.content.Context;

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

    public static void addCommunityNotification(
            Context context,
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
        File file = new File(context.getFilesDir(), "local_notifications.json");
        List<ComNotificationItem> list = new ArrayList<>();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONArray jsonArray = new JSONArray(sb.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String itemUserId = obj.optString("userId");
                    String itemUserAvatar = obj.optString("userAvatar");
                    String itemPostId = obj.optString("postId");
                    String itemCommentId = obj.optString("commentId");

                    list.add(new ComNotificationItem(
                            obj.getString("id"),
                            (itemUserId.isEmpty() || itemUserId.equals("null")) ? null : itemUserId,
                            obj.getString("userName"),
                            (itemUserAvatar.isEmpty() || itemUserAvatar.equals("null")) ? null : itemUserAvatar,
                            obj.getString("type"),
                            obj.getString("actionType"),
                            obj.getString("content"),
                            obj.getString("time"),
                            obj.getString("date"),
                            obj.optBoolean("isRead", false),
                            (itemPostId.isEmpty() || itemPostId.equals("null")) ? null : itemPostId,
                            (itemCommentId.isEmpty() || itemCommentId.equals("null")) ? null : itemCommentId
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("notification_com.json"), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                JSONArray jsonArray = new JSONArray(sb.toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String itemUserId = obj.optString("userId");
                    String itemPostId = obj.optString("postId");
                    String itemCommentId = obj.optString("commentId");

                    list.add(new ComNotificationItem(
                            obj.getString("id"),
                            (itemUserId.isEmpty() || itemUserId.equals("null")) ? null : itemUserId,
                            obj.optString("userName", "Hệ thống"),
                            obj.optString("userAvatar", ""),
                            obj.getString("type"),
                            obj.getString("actionType"),
                            obj.getString("content"),
                            obj.getString("time"),
                            obj.getString("date"),
                            obj.optBoolean("isRead", false),
                            (itemPostId.isEmpty() || itemPostId.equals("null")) ? null : itemPostId,
                            (itemCommentId.isEmpty() || itemCommentId.equals("null")) ? null : itemCommentId
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        ComNotificationItem newItem = new ComNotificationItem(
                id,
                userId,
                userName,
                userAvatar != null ? userAvatar : "https://res.cloudinary.com/dpjkzxjl2/image/upload/v1780560866/Rootie_logo.png",
                type,
                actionType,
                content,
                currentTime,
                currentDate,
                false,
                postId,
                commentId
        );

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.remove(i);
                break;
            }
        }
        list.add(0, newItem);

        try {
            JSONArray newArray = new JSONArray();
            for (ComNotificationItem item : list) {
                JSONObject obj = new JSONObject();
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
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(newArray.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
