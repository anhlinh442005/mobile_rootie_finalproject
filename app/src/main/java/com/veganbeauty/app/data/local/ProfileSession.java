package com.veganbeauty.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileSession {

    public static final ProfileSession INSTANCE = new ProfileSession();

    private ProfileSession() {}

    private static final String PREFS_NAME = "rootie_profile_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_GUEST_PHONE = "guest_phone";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FAST_LOGIN = "fast_login";
    private static final String KEY_CCCD = "cccd";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LAST_LOGIN = "last_login";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_PROFILE_HAS_LOCAL_EDITS = "profile_has_local_edits";
    private static final String KEY_LAST_ACTIVE_USER_ID = "last_active_user_id";

    private static final Set<String> DEMO_TEAM_USER_IDS = new HashSet<>(Arrays.asList(
            "test_001",
            "39751498",
            "87962440",
            "68751659",
            "85097162",
            "48228004",
            "rootie_vn",
            "xuannk_001"
    ));

    // Notification setting keys
    private static final String KEY_NOTI_ENABLED = "noti_enabled";
    private static final String KEY_NOTI_SOUND = "noti_sound";
    private static final String KEY_NOTI_VIBRATE = "noti_vibrate";

    public static String getUserId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, "");
    }

    public static void setUserId(Context context, String userId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public static String getUsername(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USERNAME, "");
    }

    public static void setUsername(Context context, String username) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public static String getFullName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FULLNAME, "");
    }

    public static void setFullName(Context context, String fullName) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FULLNAME, fullName)
                .apply();
    }

    public static String getPhone(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PHONE, "");
    }

    public static void setPhone(Context context, String phone) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PHONE, phone)
                .apply();
    }

    public static String getGuestPhone(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GUEST_PHONE, "");
    }

    public static void setGuestPhone(Context context, String phone) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GUEST_PHONE, phone)
                .apply();
    }

    public static String getEmail(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, "");
    }

    public static void setEmail(Context context, String email) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public static boolean isFastLoginEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FAST_LOGIN, true);
    }

    public static void setFastLoginEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FAST_LOGIN, enabled)
                .apply();
    }

    public static String getCCCD(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CCCD, "");
    }

    public static void setCCCD(Context context, String cccd) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CCCD, cccd)
                .apply();
    }

    public static String getAddress(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ADDRESS, "");
    }

    public static void setAddress(Context context, String address) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ADDRESS, address)
                .apply();
    }

    public static boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static void setLoggedIn(Context context, boolean isLoggedIn) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
                .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                .apply();
    }

    /** Gia hạn phiên khi app đang dùng — tránh bị coi là guest giữa chừng. */
    public static void touchSession(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_LOGIN, System.currentTimeMillis()).apply();
    }

    public static String getAvatar(Context context) {
        String avatar = getAvatarStored(context);
        if (avatar.isEmpty()) {
            return "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg";
        }
        return avatar;
    }

    /** Giá trị avatar thực trong prefs, không fallback mặc định. */
    public static String getAvatarStored(Context context) {
        String avatar = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AVATAR, "");
        return avatar != null ? avatar.trim() : "";
    }

    public static void setAvatar(Context context, String avatarUrl) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AVATAR, avatarUrl)
                .apply();
    }

    private static final String KEY_PRIMARY_IMAGE = "primary_image";

    public static String getPrimaryImage(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PRIMARY_IMAGE, "https://i.pinimg.com/1200x/21/2e/de/212ede6c525fcf95dbfa0a7d976beaa2.jpg");
    }

    public static void setPrimaryImage(Context context, String primaryImage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PRIMARY_IMAGE, primaryImage)
                .apply();
    }

    private static final String KEY_BIO = "bio";

    public static String getBio(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_BIO, "Làm đẹp không phải để ai ngắm, mà để mình vui.");
    }

    public static void setBio(Context context, String bio) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BIO, bio)
                .apply();
    }

    private static final String KEY_DOB = "dob";
    private static final String KEY_GENDER = "gender";

    public static String getDob(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DOB, "01/01/2000");
    }

    public static void setDob(Context context, String dob) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DOB, dob)
                .apply();
    }

    public static String getGender(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GENDER, "Nữ");
    }

    public static void setGender(Context context, String gender) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GENDER, gender)
                .apply();
    }

    /**
     * Persists profile edits synchronously so background sync reads the latest values.
     */
    public static boolean saveProfileEdits(Context context,
                                           String fullName,
                                           String username,
                                           String email,
                                           String phone,
                                           String dob,
                                           String gender) {
        boolean committed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FULLNAME, fullName)
                .putString(KEY_USERNAME, username)
                .putString(KEY_EMAIL, email)
                .putString(KEY_PHONE, phone)
                .putString(KEY_DOB, dob)
                .putString(KEY_GENDER, gender)
                .putBoolean(KEY_PROFILE_HAS_LOCAL_EDITS, true)
                .commit();
        return committed;
    }

    public static boolean hasLocalProfileEdits(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PROFILE_HAS_LOCAL_EDITS, false);
    }

    public static void clearLocalProfileEdits(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PROFILE_HAS_LOCAL_EDITS, false)
                .apply();
    }

    public static void markLocalProfileEdited(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PROFILE_HAS_LOCAL_EDITS, true)
                .apply();
    }

    // --- Skincare Routine & Streak Helpers ---
    private static final String KEY_COMPLETED_MORNING_DATES = "completed_morning_dates";
    private static final String KEY_COMPLETED_EVENING_DATES = "completed_evening_dates";
    private static final String KEY_SKIN_STREAK = "skin_streak";
    private static final String KEY_SKIN_LAST_COMPLETED_DATE = "skin_last_completed_date";

    public static Set<String> getCompletedMorningDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_COMPLETED_MORNING_DATES, new HashSet<>());
    }

    public static void addCompletedMorningDate(Context context, String dateStr) {
        Set<String> current = new HashSet<>(getCompletedMorningDates(context));
        current.add(dateStr);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_COMPLETED_MORNING_DATES, current)
                .apply();
    }

    public static Set<String> getCompletedEveningDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_COMPLETED_EVENING_DATES, new HashSet<>());
    }

    public static void addCompletedEveningDate(Context context, String dateStr) {
        Set<String> current = new HashSet<>(getCompletedEveningDates(context));
        current.add(dateStr);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_COMPLETED_EVENING_DATES, current)
                .apply();
    }

    public static int getSkinStreak(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SKIN_STREAK, 0);
    }

    public static void setSkinStreak(Context context, int streak) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SKIN_STREAK, streak)
                .apply();
    }

    public static String getSkinLastCompletedDate(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SKIN_LAST_COMPLETED_DATE, "");
    }

    public static void setSkinLastCompletedDate(Context context, String dateStr) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SKIN_LAST_COMPLETED_DATE, dateStr)
                .apply();
    }

    private static final String KEY_SKIN_SOCIAL_COMPLETED_DATES = "skin_social_completed_dates";

    public static Set<String> getSkinSocialCompletedDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SKIN_SOCIAL_COMPLETED_DATES, new HashSet<>());
    }

    public static void addSkinSocialCompletedDate(Context context, String dateStr) {
        Set<String> current = new HashSet<>(getSkinSocialCompletedDates(context));
        current.add(dateStr);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SKIN_SOCIAL_COMPLETED_DATES, current)
                .apply();
    }

    // --- Routine Settings Helpers ---
    private static final String KEY_ROUTINE_MORNING_REMINDER = "routine_morning_reminder";
    private static final String KEY_ROUTINE_EVENING_REMINDER = "routine_evening_reminder";
    private static final String KEY_ROUTINE_LEAD_REMINDER = "routine_lead_reminder";
    private static final String KEY_ROUTINE_MORNING_TIME = "routine_morning_time";
    private static final String KEY_ROUTINE_EVENING_TIME = "routine_evening_time";
    private static final String KEY_ROUTINE_MORNING_STEPS = "routine_morning_steps";
    private static final String KEY_ROUTINE_EVENING_STEPS = "routine_evening_steps";

    public static boolean isMorningReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_MORNING_REMINDER, true);
    }

    public static void setMorningReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_MORNING_REMINDER, enabled).apply();
    }

    public static boolean isEveningReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_EVENING_REMINDER, true);
    }

    public static void setEveningReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_EVENING_REMINDER, enabled).apply();
    }

    public static boolean isLeadReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_LEAD_REMINDER, false);
    }

    public static void setLeadReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_LEAD_REMINDER, enabled).apply();
    }

    public static String getMorningReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ROUTINE_MORNING_TIME, "06:30");
    }

    public static void setMorningReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROUTINE_MORNING_TIME, time).apply();
    }

    public static String getEveningReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ROUTINE_EVENING_TIME, "21:45");
    }

    public static void setEveningReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROUTINE_EVENING_TIME, time).apply();
    }

    public static Set<String> getMorningSteps(Context context) {
        Set<String> defaultSteps = new HashSet<>();
        defaultSteps.add("0:Cleanser:Sữa rửa mặt dịu nhẹ:true");
        defaultSteps.add("1:Toner:Cân bằng độ pH:true");
        defaultSteps.add("2:Serum:Vitamin C - trắng da:true");
        defaultSteps.add("3:Moisturizer:Kem dưỡng khóa ẩm:false");
        defaultSteps.add("4:Sunscreen:Chống nắng phổ rộng:true");
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_ROUTINE_MORNING_STEPS, defaultSteps);
    }

    public static void setMorningSteps(Context context, Set<String> steps) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_ROUTINE_MORNING_STEPS, steps).apply();
    }

    public static Set<String> getEveningSteps(Context context) {
        Set<String> defaultSteps = new HashSet<>();
        defaultSteps.add("0:Makeup Remover:Nước tẩy trang:true");
        defaultSteps.add("1:Cleanser:Sữa rửa mặt dịu nhẹ:true");
        defaultSteps.add("2:Toner:Cân bằng độ pH:true");
        defaultSteps.add("3:Serum:Retinol - chống lão hóa:true");
        defaultSteps.add("4:Moisturizer:Kem dưỡng đêm khóa ẩm:true");
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_ROUTINE_EVENING_STEPS, defaultSteps);
    }

    public static void setEveningSteps(Context context, Set<String> steps) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_ROUTINE_EVENING_STEPS, steps).apply();
    }

    public static Set<String> getCompletedStepIdsForDate(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet("completed_steps_" + date, new HashSet<>());
    }

    public static void setCompletedStepIdsForDate(Context context, String date, Set<String> stepIds) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet("completed_steps_" + date, stepIds).apply();
    }

    public static void setCompletedMorningDates(Context context, Set<String> dates) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_COMPLETED_MORNING_DATES, dates).apply();
    }

    public static void setCompletedEveningDates(Context context, Set<String> dates) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_COMPLETED_EVENING_DATES, dates).apply();
    }

    public static boolean isMorningRewardAwarded(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("morning_reward_awarded_" + date, false);
    }

    public static void setMorningRewardAwarded(Context context, String date, boolean awarded) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("morning_reward_awarded_" + date, awarded).apply();
    }

    public static boolean isEveningRewardAwarded(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("evening_reward_awarded_" + date, false);
    }

    public static void setEveningRewardAwarded(Context context, String date, boolean awarded) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("evening_reward_awarded_" + date, awarded).apply();
    }

    public static boolean isRoutineSubmitted(Context context, String type, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(type + "_routine_submitted_" + date, false);
    }

    public static void setRoutineSubmitted(Context context, String type, String date, boolean submitted) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(type + "_routine_submitted_" + date, submitted).apply();
    }

    /** Clears submission, reward flag, and completed steps for one routine session on a date. */
    public static void resetRoutineSession(Context context, String type, String date) {
        if (context == null || type == null || date == null || date.trim().isEmpty()) {
            return;
        }
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(type + "_routine_submitted_" + date, false);
        if ("morning".equals(type)) {
            editor.putBoolean("morning_reward_awarded_" + date, false);
            Set<String> mornings = new HashSet<>(getCompletedMorningDates(context));
            mornings.remove(date);
            editor.putStringSet(KEY_COMPLETED_MORNING_DATES, mornings);
        } else {
            editor.putBoolean("evening_reward_awarded_" + date, false);
            Set<String> evenings = new HashSet<>(getCompletedEveningDates(context));
            evenings.remove(date);
            editor.putStringSet(KEY_COMPLETED_EVENING_DATES, evenings);
        }
        Set<String> steps = new HashSet<>(getCompletedStepIdsForDate(context, date));
        String prefix = type + "_";
        steps.removeIf(stepId -> stepId != null && stepId.startsWith(prefix));
        editor.putStringSet("completed_steps_" + date, steps);
        editor.apply();

        String orderId = ("morning".equals(type) ? "MORNING_ROUTINE_" : "EVENING_ROUTINE_") + date;
        com.veganbeauty.app.utils.RewardPointsHelper.removeRewardsByOrderIdToday(context, orderId);
        // Legacy orderId (không gắn ngày) — xóa luôn nếu còn từ bản cũ
        com.veganbeauty.app.utils.RewardPointsHelper.removeRewardsByOrderIdToday(
                context, "morning".equals(type) ? "MORNING_ROUTINE" : "EVENING_ROUTINE");
    }

    // --- Skincare Quiz & Skin Profile Storage ---
    private static final String QUIZ_PREFS_NAME = "RootieQuizPrefs";
    private static final String KEY_SAVED_USER_SKIN_TYPE = "SAVED_USER_SKIN_TYPE";
    private static final String KEY_SAVED_RECOMMENDATION = "SAVED_RECOMMENDATION";
    private static final String KEY_SAVED_SENSITIVITY = "SAVED_SENSITIVITY";
    private static final String KEY_SAVED_HYDRATION = "SAVED_HYDRATION";
    private static final String KEY_SAVED_ELASTICITY = "SAVED_ELASTICITY";
    private static final String KEY_SAVED_SEBUM = "SAVED_SEBUM";
    private static final String KEY_SAVED_SKIN_AREAS = "SAVED_SKIN_AREAS";
    private static final String KEY_SAVED_FLAGGED_GROUPS = "SAVED_FLAGGED_GROUPS";
    private static final String KEY_QUIZ_HISTORY_LIST = "QUIZ_HISTORY_LIST";

    public static String getSavedUserSkinType(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_USER_SKIN_TYPE, "Da hỗn hợp thiên dầu");
    }

    public static void setSavedUserSkinType(Context context, String skinType) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_USER_SKIN_TYPE, skinType)
                .apply();
    }

    public static String getSavedRecommendation(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_RECOMMENDATION, "");
    }

    public static void setSavedRecommendation(Context context, String recommendation) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_RECOMMENDATION, recommendation)
                .apply();
    }

    public static int getSavedSensitivity(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_SENSITIVITY, 50);
    }

    public static void setSavedSensitivity(Context context, int sensitivity) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_SENSITIVITY, sensitivity)
                .apply();
    }

    public static int getSavedHydration(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_HYDRATION, 50);
    }

    public static void setSavedHydration(Context context, int hydration) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_HYDRATION, hydration)
                .apply();
    }

    public static int getSavedElasticity(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_ELASTICITY, 75);
    }

    public static void setSavedElasticity(Context context, int elasticity) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_ELASTICITY, elasticity)
                .apply();
    }

    public static int getSavedSebum(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_SEBUM, 50);
    }

    public static void setSavedSebum(Context context, int sebum) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_SEBUM, sebum)
                .apply();
    }

    public static String getSavedSkinAreas(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_SKIN_AREAS, "Độ ẩm và dầu phân bố không đều.");
    }

    public static void setSavedSkinAreas(Context context, String skinAreas) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_SKIN_AREAS, skinAreas)
                .apply();
    }

    public static Set<String> getSavedFlaggedGroups(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SAVED_FLAGGED_GROUPS, new HashSet<>());
    }

    public static void setSavedFlaggedGroups(Context context, Set<String> flaggedGroups) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SAVED_FLAGGED_GROUPS, flaggedGroups)
                .apply();
    }

    public static String getQuizHistoryList(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_QUIZ_HISTORY_LIST, "[]");
    }

    public static void setQuizHistoryList(Context context, String historyListJson) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_QUIZ_HISTORY_LIST, historyListJson)
                .apply();
    }

    private static final String KEY_SKIN_PROFILE_FROM_SCAN = "SKIN_PROFILE_FROM_SCAN";

    /** True khi user đã làm quiz hoặc đã có kết quả soi da / lịch sử được đồng bộ vào prefs. */
    public static boolean hasSavedSkinProfile(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE);
        if (getLastSkinTestTime(context) > 0L) {
            return true;
        }
        if (prefs.getBoolean(KEY_SKIN_PROFILE_FROM_SCAN, false)) {
            return true;
        }
        return prefs.contains(KEY_SAVED_USER_SKIN_TYPE)
                && (prefs.contains(KEY_SAVED_SENSITIVITY) || prefs.contains(KEY_SAVED_HYDRATION));
    }

    /**
     * Đồng bộ hồ sơ da từ payload quiz hoặc kết quả quét AI vào prefs — dùng chung cho mọi AI recommend.
     */
    public static void applySkinProfileFromPayload(Context context, JSONObject obj) {
        if (context == null || obj == null) {
            return;
        }
        try {
            SharedPreferences.Editor editor = context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE).edit();
            boolean fromScan = false;

            JSONObject skinCondition = obj.optJSONObject("skinCondition");
            JSONObject detailed = obj.optJSONObject("detailedEvaluation");

            String skinType = obj.optString("skinType", "");
            if (skinType.isEmpty() && skinCondition != null) {
                skinType = skinCondition.optString("skinType", "");
            }
            if (!skinType.isEmpty()) {
                editor.putString(KEY_SAVED_USER_SKIN_TYPE, skinType);
            }

            if (detailed != null) {
                fromScan = true;
                putEvalScore(editor, detailed, "moisture", KEY_SAVED_HYDRATION);
                putEvalScore(editor, detailed, "oil", KEY_SAVED_SEBUM);
                putEvalScore(editor, detailed, "sensitivity", KEY_SAVED_SENSITIVITY);
            }
            if (obj.has("hydration")) {
                editor.putInt(KEY_SAVED_HYDRATION, obj.optInt("hydration"));
            }
            if (obj.has("sebum")) {
                editor.putInt(KEY_SAVED_SEBUM, obj.optInt("sebum"));
            }
            if (obj.has("sensitivity")) {
                editor.putInt(KEY_SAVED_SENSITIVITY, obj.optInt("sensitivity"));
            }
            if (obj.has("elasticity")) {
                editor.putInt(KEY_SAVED_ELASTICITY, obj.optInt("elasticity"));
            }

            String recommendation = obj.optString("recommendation", "");
            if (recommendation.isEmpty()) {
                recommendation = obj.optString("summaryText", "");
            }
            if (recommendation.isEmpty()) {
                JSONArray suggestions = obj.optJSONArray("suggestions");
                if (suggestions != null && suggestions.length() > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < suggestions.length(); i++) {
                        if (i > 0) sb.append(' ');
                        sb.append(suggestions.optString(i, "").trim());
                    }
                    recommendation = sb.toString().trim();
                }
            }
            if (!recommendation.isEmpty()) {
                editor.putString(KEY_SAVED_RECOMMENDATION, recommendation);
            }

            if (skinCondition != null) {
                fromScan = true;
                StringBuilder areas = new StringBuilder();
                appendSkinArea(areas, "Mụn", skinCondition.optString("acne", ""));
                appendSkinArea(areas, "Sắc tố", skinCondition.optString("pigmentationStatus", ""));
                appendSkinArea(areas, "Nếp nhăn", skinCondition.optString("wrinkles", ""));
                appendSkinArea(areas, "Đều màu", skinCondition.optString("evenness", ""));
                if (areas.length() > 0) {
                    editor.putString(KEY_SAVED_SKIN_AREAS, areas.toString().trim());
                }
            } else if (obj.has("skinAreas")) {
                editor.putString(KEY_SAVED_SKIN_AREAS, obj.optString("skinAreas", ""));
            }

            if (fromScan) {
                editor.putBoolean(KEY_SKIN_PROFILE_FROM_SCAN, true);
            }
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void putEvalScore(SharedPreferences.Editor editor, JSONObject detailed,
                                     String evalKey, String prefKey) {
        JSONObject node = detailed.optJSONObject(evalKey);
        if (node != null && node.has("score")) {
            editor.putInt(prefKey, node.optInt("score", 50));
        }
    }

    private static void appendSkinArea(StringBuilder sb, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(". ");
        }
        sb.append(label).append(": ").append(value.trim());
    }

    private static final String KEY_LAST_SKIN_TEST_TIME = "KEY_LAST_SKIN_TEST_TIME";
    private static final String KEY_QUIZ_REWARD_FOR_TEST_TIME = "KEY_QUIZ_REWARD_FOR_TEST_TIME";
    private static final String KEY_HIDE_QUIZ_REMINDER_WEEKLY = "KEY_HIDE_QUIZ_REMINDER_WEEKLY";

    public static long getLastSkinTestTime(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SKIN_TEST_TIME, 0L);
    }

    public static final long QUIZ_REWARD_COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000;

    /**
     * Đủ điều kiện cộng xu khi hoàn thành quiz.
     * Cho phép làm lại + nhận xu mỗi lần hoàn thành (không khóa 7 ngày).
     */
    public static boolean isQuizRewardEligible(Context context) {
        return true;
    }

    /** Banner nhắc test định kỳ trên Home — vẫn theo chu kỳ 7 ngày. */
    public static boolean isWeeklyQuizReminderDue(Context context) {
        long lastTestTime = getLastSkinTestTime(context);
        if (lastTestTime == 0L) {
            return false;
        }
        return System.currentTimeMillis() - lastTestTime >= QUIZ_REWARD_COOLDOWN_MS;
    }

    /** Gọi sau khi cộng xu quiz thành công. */
    public static void markQuizRewardGranted(Context context) {
        if (context == null) return;
        long lastTestTime = getLastSkinTestTime(context);
        if (lastTestTime <= 0L) {
            lastTestTime = System.currentTimeMillis();
        }
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_QUIZ_REWARD_FOR_TEST_TIME, lastTestTime)
                .apply();
    }

    public static int getDaysUntilQuizReward(Context context) {
        long lastTestTime = getLastSkinTestTime(context);
        if (lastTestTime == 0L) {
            return 0;
        }
        long remainingMs = QUIZ_REWARD_COOLDOWN_MS - (System.currentTimeMillis() - lastTestTime);
        if (remainingMs <= 0L) {
            return 0;
        }
        return (int) Math.ceil(remainingMs / (24.0 * 60 * 60 * 1000));
    }

    public static void setLastSkinTestTime(Context context, long time) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SKIN_TEST_TIME, time)
                .apply();
    }

    public static boolean isQuizReminderDismissedWeekly(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, false);
    }

    public static void setQuizReminderDismissedWeekly(Context context, boolean dismissed) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, dismissed)
                .apply();
    }

    // --- Product Expiry Notification Settings ---
    public static boolean isNotiExpiryEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_enabled", true);
    }

    public static void setNotiExpiryEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_enabled", enabled)
                .apply();
    }

    public static boolean isNotiExpiryWeek1(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_week1", true);
    }

    public static void setNotiExpiryWeek1(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_week1", enabled)
                .apply();
    }

    public static boolean isNotiExpiryWeek2(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_week2", true);
    }

    public static void setNotiExpiryWeek2(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_week2", enabled)
                .apply();
    }

    public static boolean isNotiEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_ENABLED, true);
    }

    public static void setNotiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_ENABLED, enabled)
                .apply();
    }

    public static boolean isSkinWeatherNotiEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("skin_weather_noti", true);
    }
    public static void setSkinWeatherNotiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("skin_weather_noti", enabled).apply();
    }
    public static boolean isLockScreenEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("lock_screen_enabled", true);
    }
    public static void setLockScreenEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("lock_screen_enabled", enabled).apply();
    }
    public static boolean isOrderStatusEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("order_status_enabled", true);
    }
    public static void setOrderStatusEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("order_status_enabled", enabled).apply();
    }
    public static boolean isStaffMessageEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("staff_message_enabled", true);
    }
    public static void setStaffMessageEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("staff_message_enabled", enabled).apply();
    }
    public static boolean isPromotionEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean("promotion_enabled", true);
    }
    public static void setPromotionEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean("promotion_enabled", enabled).apply();
    }

    public static boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_SOUND, true);
    }

    public static void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_SOUND, enabled)
                .apply();
    }

    public static boolean isVibrateEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_VIBRATE, true);
    }

    public static void setVibrateEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_VIBRATE, enabled)
                .apply();
    }

    public static boolean getProductNotiEnabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_enabled_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryEnabled(context);
        }
    }

    public static void setProductNotiEnabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_enabled_" + userId + "_" + productId, enabled)
                .apply();
    }

    public static boolean getProductWeek1Enabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_week1_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryWeek1(context);
        }
    }

    public static void setProductWeek1Enabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_week1_" + userId + "_" + productId, enabled)
                .apply();
    }

    public static boolean getProductWeek2Enabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_week2_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryWeek2(context);
        }
    }

    public static void setProductWeek2Enabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_week2_" + userId + "_" + productId, enabled)
                .apply();
    }

    public static boolean isDemoTeamUser(@androidx.annotation.Nullable String userId) {
        return userId != null && DEMO_TEAM_USER_IDS.contains(userId.trim());
    }

    /**
     * Gọi khi đăng nhập thành công. Đổi user → xóa prefs da/quiz/routine cũ để tài khoản mới bắt đầu sạch.
     * Xu/quà đã gắn userId nên giữ nguyên khi đổi tài khoản (mỗi user có lịch sử riêng).
     */
    public static void activateUserSession(Context context, String newUserId) {
        if (context == null || newUserId == null || newUserId.trim().isEmpty()) {
            return;
        }
        String trimmedUserId = newUserId.trim();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String previousUserId = prefs.getString(KEY_LAST_ACTIVE_USER_ID, "");
        if (!trimmedUserId.equals(previousUserId)) {
            resetUserScopedData(context);
            prefs.edit().putString(KEY_LAST_ACTIVE_USER_ID, trimmedUserId).apply();
        }
    }

    /** Xóa hồ sơ da, quiz, routine và streak — không đụng flag seed app-level trong RootieQuizPrefs. */
    public static void resetUserScopedData(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences quizPrefs = context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor quizEditor = quizPrefs.edit();
        quizEditor
                .remove(KEY_LAST_SKIN_TEST_TIME)
                .remove(KEY_QUIZ_REWARD_FOR_TEST_TIME)
                .remove(KEY_HIDE_QUIZ_REMINDER_WEEKLY)
                .remove(KEY_SAVED_USER_SKIN_TYPE)
                .remove(KEY_SAVED_RECOMMENDATION)
                .remove(KEY_SAVED_SENSITIVITY)
                .remove(KEY_SAVED_HYDRATION)
                .remove(KEY_SAVED_ELASTICITY)
                .remove(KEY_SAVED_SEBUM)
                .remove(KEY_SAVED_SKIN_AREAS)
                .remove(KEY_SAVED_FLAGGED_GROUPS)
                .remove(KEY_QUIZ_HISTORY_LIST)
                .remove("SKIN_TYPE_RESULT")
                .remove("RECOMMENDATION")
                .remove("FLAGGED_GROUPS")
                .remove("SENSITIVITY_PERCENT")
                .remove("HYDRATION_PERCENT")
                .remove("ELASTICITY_PERCENT")
                .remove("SEBUM_PERCENT")
                .remove("SKIN_AREAS_DESC")
                .remove(KEY_SKIN_PROFILE_FROM_SCAN);
        for (String key : quizPrefs.getAll().keySet()) {
            if (key.startsWith("skin_weather_snapshot_")) {
                quizEditor.remove(key);
            }
        }
        quizEditor.apply();

        SharedPreferences profilePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor profileEditor = profilePrefs.edit();
        profileEditor
                .remove(KEY_COMPLETED_MORNING_DATES)
                .remove(KEY_COMPLETED_EVENING_DATES)
                .remove(KEY_SKIN_SOCIAL_COMPLETED_DATES)
                .putInt(KEY_SKIN_STREAK, 0)
                .remove(KEY_SKIN_LAST_COMPLETED_DATE)
                .remove("june_2026_seeded_v1")
                .remove("skin_max_streak")
                .putBoolean(KEY_PROFILE_HAS_LOCAL_EDITS, false);
        clearSavedAddresses(profileEditor);
        for (Map.Entry<String, ?> entry : profilePrefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("morning_reward_awarded_")
                    || key.startsWith("evening_reward_awarded_")
                    || key.startsWith("morning_routine_submitted_")
                    || key.startsWith("evening_routine_submitted_")
                    || key.startsWith("completed_steps_")) {
                profileEditor.remove(key);
            }
        }
        profileEditor.apply();

        // Điểm danh từng lưu global (không theo user) — xóa để không dính sang tài khoản mới
        context.getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    /** Xóa địa chỉ giao hàng đã lưu (Nhà/Văn phòng + list checkout). */
    public static void clearSavedAddresses(Context context) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        clearSavedAddresses(editor);
        editor.apply();
    }

    private static void clearSavedAddresses(SharedPreferences.Editor editor) {
        editor.remove(KEY_ADDRESS)
                .remove("addr_home_name")
                .remove("addr_home_phone")
                .remove("addr_home_addr")
                .remove("addr_office_name")
                .remove("addr_office_phone")
                .remove("addr_office_addr")
                .remove("addr_default_type")
                .remove("saved_addresses_list_json");
    }

    public static String getCurrentUserId(Context context) {
        String email = getEmail(context);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("users.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray usersJsonArray = new JSONArray(sb.toString().replace("\uFEFF", ""));
            for (int i = 0; i < usersJsonArray.length(); i++) {
                JSONObject obj = usersJsonArray.getJSONObject(i);
                if (email.equals(obj.optString("email"))) {
                    return obj.optString("user_id", "test_001");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "test_001";
    }
}
