package com.veganbeauty.app.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.dao.SkinHistoryDao;
import com.veganbeauty.app.data.local.entities.SkinHistoryEntity;
import com.veganbeauty.app.utils.SkinHistoryIdHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Lưu lịch sử soi da / quiz trên Room — không cần Firebase. */
public final class SkinHistoryLocalStore {

    private SkinHistoryLocalStore() {
    }

    @NonNull
    public static JSONArray getHistory(@NonNull Context context, @Nullable String userId, @Nullable String email) {
        String safeUserId = userId != null ? userId.trim() : "";
        String safeEmail = email != null ? email.trim() : "";
        SkinHistoryDao dao = RootieDatabase.getDatabase(context).skinHistoryDao();
        List<SkinHistoryEntity> rows = dao.getByUser(safeUserId, safeEmail);
        if (rows.isEmpty()) {
            seedFromAssetsIfNeeded(context, dao, safeUserId, safeEmail);
            rows = dao.getByUser(safeUserId, safeEmail);
        }
        JSONArray array = new JSONArray();
        for (SkinHistoryEntity row : sortRows(rows)) {
            try {
                JSONObject obj = new JSONObject(row.getPayload());
                if (!obj.has("id")) {
                    obj.put("id", row.getId());
                }
                array.put(obj);
            } catch (Exception ignored) {
            }
        }
        return array;
    }

    public static void save(@NonNull Context context, @NonNull JSONObject historyObj,
                            @Nullable String userId, @Nullable String email) {
        try {
            String id = historyObj.optString("id", SkinHistoryIdHelper.generateId());
            historyObj.put("id", id);
            if (userId != null && !userId.trim().isEmpty()) {
                historyObj.put("userId", userId.trim());
            }
            if (email != null && !email.trim().isEmpty()) {
                historyObj.put("email", email.trim());
            }
            if (!historyObj.has("scanType")) {
                historyObj.put("scanType", "Quét AI");
            }

            String date = historyObj.optString("date", "");
            String time = historyObj.optString("time", "");
            if (date.isEmpty()) {
                String combined = historyObj.optString("date", "");
                if (combined.contains(" ")) {
                    String[] parts = combined.split(" ", 2);
                    date = parts[0];
                    if (time.isEmpty() && parts.length > 1) {
                        time = parts[1];
                    }
                }
            }

            SkinHistoryEntity entity = new SkinHistoryEntity(
                    id,
                    userId != null ? userId.trim() : "",
                    email != null ? email.trim() : "",
                    historyObj.toString(),
                    date,
                    time,
                    historyObj.optString("scanType", "Quét AI")
            );
            RootieDatabase.getDatabase(context).skinHistoryDao().insert(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void seedFromAssetsIfNeeded(Context context, SkinHistoryDao dao,
                                               String userId, String email) {
        try {
            JSONArray assetHistory = new LocalJsonReader(context).getSkinHistory();
            if (assetHistory.length() == 0) {
                return;
            }
            List<SkinHistoryEntity> toInsert = new ArrayList<>();
            for (int i = 0; i < assetHistory.length(); i++) {
                JSONObject obj = assetHistory.getJSONObject(i);
                String id = obj.optString("id", SkinHistoryIdHelper.generateId());
                obj.put("id", id);
                if (!userId.isEmpty()) {
                    obj.put("userId", userId);
                }
                if (!email.isEmpty()) {
                    obj.put("email", email);
                }
                toInsert.add(new SkinHistoryEntity(
                        id,
                        userId,
                        email,
                        obj.toString(),
                        obj.optString("date", ""),
                        obj.optString("time", ""),
                        obj.optString("scanType", "Quét AI")
                ));
            }
            dao.insertAll(toInsert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<SkinHistoryEntity> sortRows(List<SkinHistoryEntity> rows) {
        List<SkinHistoryEntity> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing((SkinHistoryEntity row) ->
                        (row.getDate() + " " + row.getTime()).trim())
                .reversed());
        return sorted;
    }
}
