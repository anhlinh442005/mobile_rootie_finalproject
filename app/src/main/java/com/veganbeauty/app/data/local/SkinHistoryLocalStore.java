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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Lưu lịch sử soi da / quiz trên Room — không cần Firebase. */
public final class SkinHistoryLocalStore {

    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor();

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
            migrateQuizPrefsIfNeeded(context, dao, safeUserId, safeEmail);
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

    /**
     * Lưu lịch sử da. Room luôn chạy trên background thread để tránh crash
     * {@code Cannot access database on the main thread}.
     */
    public static void save(@NonNull Context context, @NonNull JSONObject historyObj,
                            @Nullable String userId, @Nullable String email) {
        try {
            Context appContext = context.getApplicationContext();
            SkinHistoryEntity entity = buildEntity(historyObj, userId, email);
            if (entity == null) {
                return;
            }
            SAVE_EXECUTOR.execute(() -> {
                try {
                    RootieDatabase.getDatabase(appContext).skinHistoryDao().insert(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lưu đồng bộ trên thread hiện tại (gọi từ background thread).
     * Dùng khi cần đọc lại lịch sử ngay sau khi ghi (vd. đồng bộ từ lịch SPA).
     */
    public static void saveSync(@NonNull Context context, @NonNull JSONObject historyObj,
                                @Nullable String userId, @Nullable String email) {
        try {
            SkinHistoryEntity entity = buildEntity(historyObj, userId, email);
            if (entity == null) {
                return;
            }
            RootieDatabase.getDatabase(context.getApplicationContext()).skinHistoryDao().insert(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteSync(@NonNull Context context, @NonNull String historyId) {
        if (historyId.trim().isEmpty()) {
            return;
        }
        try {
            RootieDatabase.getDatabase(context.getApplicationContext())
                    .skinHistoryDao()
                    .deleteById(historyId.trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    private static SkinHistoryEntity buildEntity(@NonNull JSONObject historyObj,
                                                 @Nullable String userId,
                                                 @Nullable String email) {
        try {
            JSONObject copy = new JSONObject(historyObj.toString());
            String safeUserId = userId != null ? userId.trim() : "";
            String safeEmail = email != null ? email.trim() : "";

            String id = copy.optString("id", SkinHistoryIdHelper.generateId());
            copy.put("id", id);
            if (!safeUserId.isEmpty()) {
                copy.put("userId", safeUserId);
            }
            if (!safeEmail.isEmpty()) {
                copy.put("email", safeEmail);
            }
            if (!copy.has("scanType") || copy.optString("scanType").trim().isEmpty()) {
                copy.put("scanType", "Quét AI");
            }

            String date = copy.optString("date", "");
            String time = copy.optString("time", "");
            if (date.contains(" ") && time.isEmpty()) {
                String[] parts = date.split(" ", 2);
                date = parts[0];
                time = parts.length > 1 ? parts[1] : "";
                copy.put("date", date);
                copy.put("time", time);
            }

            return new SkinHistoryEntity(
                    id,
                    safeUserId,
                    safeEmail,
                    copy.toString(),
                    date,
                    time,
                    copy.optString("scanType", "Quét AI")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Đưa các bản ghi quiz trong SharedPreferences vào Room nếu DB còn trống. */
    private static void migrateQuizPrefsIfNeeded(Context context, SkinHistoryDao dao,
                                                 String userId, String email) {
        try {
            String historyStr = ProfileSession.getQuizHistoryList(context);
            if (historyStr == null || historyStr.trim().isEmpty() || "[]".equals(historyStr.trim())) {
                return;
            }
            JSONArray quizHistory = new JSONArray(historyStr);
            if (quizHistory.length() == 0) {
                return;
            }
            List<SkinHistoryEntity> toInsert = new ArrayList<>();
            for (int i = 0; i < quizHistory.length(); i++) {
                JSONObject obj = quizHistory.getJSONObject(i);
                String id = obj.optString("id", SkinHistoryIdHelper.generateId());
                obj.put("id", id);
                if (!userId.isEmpty()) {
                    obj.put("userId", userId);
                }
                if (!email.isEmpty()) {
                    obj.put("email", email);
                }
                if (!obj.has("scanType") || obj.optString("scanType").trim().isEmpty()) {
                    obj.put("scanType", "Test da");
                }
                String date = obj.optString("date", "");
                String time = obj.optString("time", "");
                if (date.contains(" ") && time.isEmpty()) {
                    String[] parts = date.split(" ", 2);
                    date = parts[0];
                    time = parts.length > 1 ? parts[1] : "";
                    obj.put("date", date);
                    obj.put("time", time);
                }
                toInsert.add(new SkinHistoryEntity(
                        id,
                        userId,
                        email,
                        obj.toString(),
                        date,
                        time,
                        obj.optString("scanType", "Test da")
                ));
            }
            if (!toInsert.isEmpty()) {
                dao.insertAll(toInsert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void seedFromAssetsIfNeeded(Context context, SkinHistoryDao dao,
                                               String userId, String email) {
        if (!ProfileSession.isDemoTeamUser(userId)) {
            return;
        }
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

    @Nullable
    public static JSONObject getLatestPayload(@NonNull Context context,
                                              @Nullable String userId,
                                              @Nullable String email) {
        JSONArray history = getHistory(context, userId, email);
        if (history.length() == 0) {
            return null;
        }
        try {
            return history.getJSONObject(0);
        } catch (Exception e) {
            return null;
        }
    }

    /** Nạp hồ sơ da từ lịch sử local (quiz / quét AI) nếu prefs chưa có. */
    public static void hydrateProfileFromHistoryIfNeeded(@NonNull Context context) {
        if (ProfileSession.hasSavedSkinProfile(context)) {
            return;
        }
        JSONObject latest = getLatestPayload(
                context,
                ProfileSession.getUserId(context),
                ProfileSession.getEmail(context)
        );
        if (latest != null) {
            ProfileSession.applySkinProfileFromPayload(context, latest);
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
