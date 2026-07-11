package com.veganbeauty.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/**
 * Lưu / khôi phục hồ sơ quiz & da theo từng user — tránh mất kết quả khi đăng nhập lại.
 */
public final class UserQuizStateHelper {

    private static final String PROFILE_PREFS = "rootie_profile_prefs";
    private static final String QUIZ_PREFS = "RootieQuizPrefs";
    private static final String KEY_PREFIX = "user_quiz_state_json_";

    private static final String[] SKIN_PROFILE_KEYS = {
            "KEY_LAST_SKIN_TEST_TIME",
            "KEY_QUIZ_REWARD_FOR_TEST_TIME",
            "KEY_HIDE_QUIZ_REMINDER_WEEKLY",
            "SAVED_USER_SKIN_TYPE",
            "SAVED_RECOMMENDATION",
            "SAVED_SENSITIVITY",
            "SAVED_HYDRATION",
            "SAVED_ELASTICITY",
            "SAVED_SEBUM",
            "SAVED_SKIN_AREAS",
            "QUIZ_HISTORY_LIST",
            "SKIN_TYPE_RESULT",
            "RECOMMENDATION",
            "FLAGGED_GROUPS",
            "SENSITIVITY_PERCENT",
            "HYDRATION_PERCENT",
            "ELASTICITY_PERCENT",
            "SEBUM_PERCENT",
            "SKIN_AREAS_DESC",
            "SKIN_PROFILE_FROM_SCAN"
    };

    private UserQuizStateHelper() {
    }

    public static void saveForUser(@Nullable Context context, @Nullable String userId) {
        if (context == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        SharedPreferences quizPrefs = context.getApplicationContext()
                .getSharedPreferences(QUIZ_PREFS, Context.MODE_PRIVATE);
        try {
            JSONObject snapshot = new JSONObject();
            for (String key : SKIN_PROFILE_KEYS) {
                if (!quizPrefs.contains(key)) {
                    continue;
                }
                putPrefValue(snapshot, key, quizPrefs);
            }
            if (quizPrefs.contains("SAVED_FLAGGED_GROUPS")) {
                putPrefValue(snapshot, "SAVED_FLAGGED_GROUPS", quizPrefs);
            }
            context.getApplicationContext()
                    .getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PREFIX + userId.trim(), snapshot.toString())
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restoreForUser(@Nullable Context context, @Nullable String userId) {
        if (context == null || userId == null || userId.trim().isEmpty()) {
            return;
        }
        String json = context.getApplicationContext()
                .getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PREFIX + userId.trim(), null);
        if (json == null || json.trim().isEmpty()) {
            return;
        }
        try {
            JSONObject snapshot = new JSONObject(json);
            SharedPreferences.Editor editor = context.getApplicationContext()
                    .getSharedPreferences(QUIZ_PREFS, Context.MODE_PRIVATE)
                    .edit();
            JSONArray names = snapshot.names();
            if (names == null) {
                return;
            }
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                applyPrefValue(editor, key, snapshot);
            }
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void putPrefValue(@NonNull JSONObject target, @NonNull String key,
                                     @NonNull SharedPreferences prefs) throws Exception {
        Object value = prefs.getAll().get(key);
        if (value == null) {
            return;
        }
        if (value instanceof String) {
            target.put(key, (String) value);
        } else if (value instanceof Integer) {
            target.put(key, (Integer) value);
        } else if (value instanceof Long) {
            target.put(key, (Long) value);
        } else if (value instanceof Boolean) {
            target.put(key, (Boolean) value);
        } else if (value instanceof Float) {
            target.put(key, (double) (Float) value);
        } else if (value instanceof Set) {
            JSONArray array = new JSONArray();
            for (Object item : (Set<?>) value) {
                array.put(String.valueOf(item));
            }
            target.put(key, array);
        }
    }

    private static void applyPrefValue(@NonNull SharedPreferences.Editor editor,
                                       @NonNull String key, @NonNull JSONObject snapshot) throws Exception {
        if (!snapshot.has(key)) {
            return;
        }
        Object raw = snapshot.get(key);
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            Set<String> values = new java.util.HashSet<>();
            for (int i = 0; i < array.length(); i++) {
                values.add(array.getString(i));
            }
            editor.putStringSet(key, values);
        } else if (raw instanceof Boolean) {
            editor.putBoolean(key, (Boolean) raw);
        } else if (raw instanceof Number) {
            Number number = (Number) raw;
            if (key.contains("TIME")) {
                editor.putLong(key, number.longValue());
            } else {
                editor.putInt(key, number.intValue());
            }
        } else {
            editor.putString(key, snapshot.optString(key, ""));
        }
    }
}
