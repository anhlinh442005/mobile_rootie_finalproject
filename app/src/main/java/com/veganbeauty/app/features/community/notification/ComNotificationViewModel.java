package com.veganbeauty.app.features.community.notification;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.utils.RootieBrandHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ComNotificationViewModel extends ViewModel {

    private final MutableLiveData<List<ComNotificationItem>> notifications = new MutableLiveData<>();
    private final MutableLiveData<List<ComNotificationListItem>> filteredNotifications = new MutableLiveData<>();
    private final MutableLiveData<String> activeTab = new MutableLiveData<>("ALL");
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<List<ComNotificationListItem>> getFilteredNotifications() {
        return filteredNotifications;
    }

    public LiveData<String> getActiveTab() {
        return activeTab;
    }

    public void initData(Context context) {
        executor.execute(() -> {
            List<ComNotificationItem> loadedList = loadNotificationsFromLocal(context);
            boolean needsRegen = loadedList != null && !loadedList.isEmpty();
            if (needsRegen) {
                for (ComNotificationItem item : loadedList) {
                    if ((item.getType().equals("POST") || item.getType().equals("INTERACTION"))
                            && (item.getPostId() == null || item.getPostId().isEmpty())) {
                        needsRegen = false;
                        break;
                    }
                }
            }

            if (loadedList == null || loadedList.isEmpty() || !needsRegen) {
                LocalJsonReader jsonReader = new LocalJsonReader(context);
                List<UserEntity> users = jsonReader.getUsers();
                Map<String, UserEntity> usersMap = new HashMap<>();
                for (UserEntity u : users) {
                    usersMap.put(u.getUser_id(), u);
                }
                loadedList = generateNotifications(context, usersMap);
                saveNotificationsToLocal(context, loadedList);
            }

            final List<ComNotificationItem> finalList = loadedList;
            notifications.postValue(finalList);
            applyFilter(finalList);
        });
    }

    public String getSectionName(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date == null) return "Cũ hơn";

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            Calendar itemCal = Calendar.getInstance();
            itemCal.setTime(date);
            itemCal.set(Calendar.HOUR_OF_DAY, 0);
            itemCal.set(Calendar.MINUTE, 0);
            itemCal.set(Calendar.SECOND, 0);
            itemCal.set(Calendar.MILLISECOND, 0);

            int todayYear = todayCal.get(Calendar.YEAR);
            int todayDayOfYear = todayCal.get(Calendar.DAY_OF_YEAR);
            int itemYear = itemCal.get(Calendar.YEAR);
            int itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR);

            if (todayYear == itemYear && todayDayOfYear == itemDayOfYear) {
                return "Hôm nay";
            }

            Calendar yesterdayCal = (Calendar) todayCal.clone();
            yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
            int yesterdayYear = yesterdayCal.get(Calendar.YEAR);
            int yesterdayDayOfYear = yesterdayCal.get(Calendar.DAY_OF_YEAR);

            if (yesterdayYear == itemYear && yesterdayDayOfYear == itemDayOfYear) {
                return "Hôm qua";
            }
            return "Cũ hơn";
        } catch (Exception e) {
            return "Cũ hơn";
        }
    }

    private void saveNotificationsToLocal(Context context, List<ComNotificationItem> items) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (ComNotificationItem item : items) {
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
                jsonArray.put(obj);
            }
            File file = new File(context.getFilesDir(), "local_notifications.json");
            FileWriter writer = new FileWriter(file);
            writer.write(jsonArray.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<ComNotificationItem> loadNotificationsFromLocal(Context context) {
        try {
            File file = new File(context.getFilesDir(), "local_notifications.json");
            if (!file.exists()) return null;
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            List<ComNotificationItem> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String uIdRaw = obj.optString("userId");
                String userId = (!uIdRaw.isEmpty() && !uIdRaw.equals("null")) ? uIdRaw : null;
                String avatarRaw = obj.optString("userAvatar");
                String avatar = (!avatarRaw.isEmpty() && !avatarRaw.equals("null")) ? avatarRaw : null;
                String pIdRaw = obj.optString("postId");
                String postId = (!pIdRaw.isEmpty() && !pIdRaw.equals("null")) ? pIdRaw : null;
                String cIdRaw = obj.optString("commentId");
                String commentId = (!cIdRaw.isEmpty() && !cIdRaw.equals("null")) ? cIdRaw : null;
                String itemDate = obj.getString("date");

                ComNotificationItem item = new ComNotificationItem(
                        obj.getString("id"),
                        userId,
                        obj.getString("userName"),
                        avatar,
                        obj.getString("type"),
                        obj.getString("actionType"),
                        obj.getString("content"),
                        obj.getString("time"),
                        itemDate,
                        obj.getBoolean("isRead"),
                        postId,
                        commentId,
                        getSectionName(itemDate)
                );
                list.add(item);
            }
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<ComNotificationItem> generateNotifications(Context context, Map<String, UserEntity> usersMap) {
        List<ComNotificationItem> list = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(context.getAssets().open("notification_com.json")));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String userIdRaw = obj.optString("userId");
                String userId = (!userIdRaw.isEmpty() && !userIdRaw.equals("null")) ? userIdRaw : null;
                String type = obj.getString("type");
                String actionType = obj.getString("actionType");
                String content = obj.getString("content");
                String time = obj.getString("time");
                String date = obj.getString("date");
                boolean isRead = obj.getBoolean("isRead");
                String id = obj.getString("id");
                String pIdRaw = obj.optString("postId");
                String postId = (!pIdRaw.isEmpty() && !pIdRaw.equals("null")) ? pIdRaw : null;
                String cIdRaw = obj.optString("commentId");
                String commentId = (!cIdRaw.isEmpty() && !cIdRaw.equals("null")) ? cIdRaw : null;

                UserEntity user = userId != null ? usersMap.get(userId) : null;
                String finalName;
                String finalAvatar;

                if (user != null) {
                    finalName = user.getFull_name() != null ? user.getFull_name() : (user.getUsername() != null ? user.getUsername() : "Người dùng");
                    finalAvatar = user.getAvatar() != null ? user.getAvatar() : "";
                } else {
                    String nameRaw = obj.optString("userName");
                    finalName = (!nameRaw.isEmpty() && !nameRaw.equals("null")) ? nameRaw : "Hệ thống";
                    String avtRaw = obj.optString("userAvatar");
                    finalAvatar = (!avtRaw.isEmpty() && !avtRaw.equals("null")) ? avtRaw : "";
                }

                if (userId == null) {
                    if ("WITHDRAW".equals(actionType)) {
                        finalName = "Hệ thống Rootie";
                    } else {
                        finalName = "Hệ thống";
                    }
                    finalAvatar = RootieBrandHelper.AVATAR_URL;
                } else if (finalName.equals("Hệ thống") || finalName.equals("Hệ thống Rootie")) {
                    finalAvatar = RootieBrandHelper.AVATAR_URL;
                } else if (finalAvatar.isEmpty()) {
                    switch (userId) {
                        case "75675216": finalAvatar = "https://i.pinimg.com/736x/b1/f4/f1/b1f4f17046008cee09ece025370ebae3.jpg"; break;
                        case "42788949": finalAvatar = "https://i.pinimg.com/736x/32/fa/81/32fa81622bd6f125026938d2f6fb39f6.jpg"; break;
                        case "85924906": finalAvatar = "https://i.pinimg.com/736x/19/67/e2/1967e25a3aac9452bace230397d15d1a.jpg"; break;
                        case "34260569": finalAvatar = "https://i.pinimg.com/474x/de/ed/45/deed45e4e0bbaa78991e1779dc87d417.jpg"; break;
                        case "49058200": finalAvatar = "https://i.pinimg.com/736x/12/96/30/1296309708221c882e57eefae42bf46b.jpg"; break;
                        case "39692528": finalAvatar = "https://i.pinimg.com/736x/e1/23/49/e12349977ea0127c7481887fafcb23f5.jpg"; break;
                        case "51884402": finalAvatar = "https://i.pinimg.com/736x/ca/36/57/ca36573c75ab03bb970f9cc9afc3a2ef.jpg"; break;
                        case "xuannk_001": finalAvatar = "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg"; break;
                        case "50899342": finalAvatar = "https://i.pinimg.com/736x/52/f9/a0/52f9a02638cbaa0610a0a7afb61f4db0.jpg"; break;
                        default: finalAvatar = "https://i.pinimg.com/736x/b1/f4/f1/b1f4f17046008cee09ece025370ebae3.jpg"; break;
                    }
                }

                list.add(new ComNotificationItem(
                        id, userId, finalName, finalAvatar,
                        type, actionType, content, time, date,
                        isRead, postId, commentId, getSectionName(date)
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void selectTab(String tab) {
        activeTab.setValue(tab);
        applyFilter();
    }

    public void setSearchQuery(String query) {
        searchQuery.setValue(query);
        applyFilter();
    }

    public void markAllRead(Context context) {
        List<ComNotificationItem> current = notifications.getValue();
        if (current == null) return;
        for (ComNotificationItem item : current) {
            item.setRead(true);
        }
        notifications.setValue(current);
        saveNotificationsToLocal(context, current);
        applyFilter();
    }

    public void markAsRead(Context context, String id) {
        List<ComNotificationItem> current = notifications.getValue();
        if (current == null) return;
        for (ComNotificationItem item : current) {
            if (item.getId().equals(id)) {
                item.setRead(true);
                break;
            }
        }
        notifications.setValue(current);
        saveNotificationsToLocal(context, current);
    }

    public void deleteNotification(Context context, String id) {
        List<ComNotificationItem> current = notifications.getValue();
        if (current == null) return;
        List<ComNotificationItem> updated = new ArrayList<>();
        for (ComNotificationItem item : current) {
            if (!item.getId().equals(id)) updated.add(item);
        }
        notifications.setValue(updated);
        saveNotificationsToLocal(context, updated);
        applyFilter();
    }

    public void deleteAllNotifications(Context context) {
        List<ComNotificationItem> current = notifications.getValue();
        if (current == null) return;
        String active = activeTab.getValue() != null ? activeTab.getValue() : "POST";
        List<ComNotificationItem> updated = new ArrayList<>();
        for (ComNotificationItem item : current) {
            if (!item.getType().equals(active)) updated.add(item);
        }
        notifications.setValue(updated);
        saveNotificationsToLocal(context, updated);
        applyFilter();
    }

    private void applyFilter() {
        applyFilter(null);
    }

    private void applyFilter(List<ComNotificationItem> allNotis) {
        if (allNotis == null) {
            allNotis = notifications.getValue();
        }
        if (allNotis == null) allNotis = new ArrayList<>();
        String tab = activeTab.getValue() != null ? activeTab.getValue() : "ALL";
        String query = searchQuery.getValue() != null ? searchQuery.getValue() : "";

        List<ComNotificationItem> tabFiltered = new ArrayList<>();
        for (ComNotificationItem item : allNotis) {
            if ("ORDER".equals(item.getType())) {
                continue;
            }
            if ("ALL".equals(tab) || item.getType().equals(tab)) {
                tabFiltered.add(item);
            }
        }

        List<ComNotificationItem> searchFiltered;
        if (query.trim().isEmpty()) {
            searchFiltered = tabFiltered;
        } else {
            searchFiltered = new ArrayList<>();
            String lq = query.toLowerCase();
            for (ComNotificationItem item : tabFiltered) {
                if (item.getUserName().toLowerCase().contains(lq) || item.getContent().toLowerCase().contains(lq)) {
                    searchFiltered.add(item);
                }
            }
        }

        for (ComNotificationItem item : searchFiltered) {
            item.setSection(getSectionName(item.getDate()));
        }

        Map<String, List<ComNotificationItem>> grouped = new LinkedHashMap<>();
        for (ComNotificationItem item : searchFiltered) {
            grouped.computeIfAbsent(item.getSection(), k -> new ArrayList<>()).add(item);
        }

        List<ComNotificationListItem> flatList = new ArrayList<>();
        for (String sectionName : Arrays.asList("Hôm nay", "Hôm qua", "Cũ hơn")) {
            List<ComNotificationItem> items = grouped.get(sectionName);
            if (items != null && !items.isEmpty()) {
                flatList.add(new ComNotificationListItem.Header(sectionName));
                for (ComNotificationItem item : items) {
                    flatList.add(new ComNotificationListItem.Notification(item));
                }
            }
        }

        filteredNotifications.postValue(flatList);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
