package com.veganbeauty.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class ProfileSession {

    public static final ProfileSession INSTANCE = new ProfileSession();

    private ProfileSession() {}

    private static final String PREFS_NAME = "rootie_profile_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "fullname";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FAST_LOGIN = "fast_login";
    private static final String KEY_CCCD = "cccd";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_LAST_LOGIN = "last_login";
    private static final String KEY_AVATAR = "avatar";
    private static final String KEY_USER_ID = "user_id";

    // Notification setting keys
    private static final String KEY_NOTI_ENABLED = "noti_enabled";
    private static final String KEY_NOTI_SOUND = "noti_sound";
    private static final String KEY_NOTI_VIBRATE = "noti_vibrate";
    private static final String KEY_NOTI_LOCK_SCREEN = "noti_lock_screen";
    private static final String KEY_NOTI_ORDER_STATUS = "noti_order_status";
    private static final String KEY_NOTI_PROMOTION = "noti_promotion";
    private static final String KEY_NOTI_PROMOTION_FREQUENCY = "noti_promotion_frequency";
    private static final String KEY_NOTI_PROMOTION_TIME_RANGE = "noti_promotion_time_range";
    private static final String KEY_NOTI_STAFF_MESSAGE = "noti_staff_message";
    private static final String KEY_NOTI_COMPLAINT_RESPONSE = "noti_complaint_response";
    private static final String KEY_NOTI_SKIN_WEATHER = "noti_skin_weather";

    public String getUserId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, "test_001");
    }

    public void setUserId(Context context, String userId) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USER_ID, userId)
                .apply();
    }

    public String getUsername(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USERNAME, "mrbeanintheworld");
    }

    public void setUsername(Context context, String username) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public String getFullName(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FULLNAME, "Sông Khánh Bình");
    }

    public void setFullName(Context context, String fullName) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FULLNAME, fullName)
                .apply();
    }

    public String getPhone(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PHONE, "0123456789");
    }

    public void setPhone(Context context, String phone) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PHONE, phone)
                .apply();
    }

    public String getEmail(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_EMAIL, "binhsk23411@st.uel.edu.vn");
    }

    public void setEmail(Context context, String email) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_EMAIL, email)
                .apply();
    }

    public boolean isFastLoginEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FAST_LOGIN, true);
    }

    public void setFastLoginEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FAST_LOGIN, enabled)
                .apply();
    }

    public String getCCCD(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CCCD, "123456789999");
    }

    public void setCCCD(Context context, String cccd) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CCCD, cccd)
                .apply();
    }

    public String getAddress(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ADDRESS, "Số 12 Đường 3, TP. Thủ Đức, TP. Hồ Chí Minh");
    }

    public void setAddress(Context context, String address) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ADDRESS, address)
                .apply();
    }

    public boolean isLoggedIn(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false);
        long lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0L);
        long oneHour = 60 * 60 * 1000L;
        return isLoggedIn && (System.currentTimeMillis() - lastLogin < oneHour);
    }

    public void setLoggedIn(Context context, boolean isLoggedIn) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
                .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                .apply();
    }

    public String getAvatar(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_AVATAR, "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg");
    }

    public void setAvatar(Context context, String avatarUrl) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AVATAR, avatarUrl)
                .apply();
    }

    private static final String KEY_PRIMARY_IMAGE = "primary_image";

    public String getPrimaryImage(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PRIMARY_IMAGE, "https://i.pinimg.com/1200x/21/2e/de/212ede6c525fcf95dbfa0a7d976beaa2.jpg");
    }

    public void setPrimaryImage(Context context, String primaryImage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PRIMARY_IMAGE, primaryImage)
                .apply();
    }

    private static final String KEY_BIO = "bio";

    public String getBio(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_BIO, "Làm đẹp không phải để ai ngắm, mà để mình vui.");
    }

    public void setBio(Context context, String bio) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BIO, bio)
                .apply();
    }

    private static final String KEY_DOB = "dob";
    private static final String KEY_GENDER = "gender";

    public String getDob(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_DOB, "01/01/2000");
    }

    public void setDob(Context context, String dob) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_DOB, dob)
                .apply();
    }

    public String getGender(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_GENDER, "Nữ");
    }

    public void setGender(Context context, String gender) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GENDER, gender)
                .apply();
    }

    // --- Skincare Routine & Streak Helpers ---
    private static final String KEY_COMPLETED_MORNING_DATES = "completed_morning_dates";
    private static final String KEY_COMPLETED_EVENING_DATES = "completed_evening_dates";
    private static final String KEY_SKIN_STREAK = "skin_streak";
    private static final String KEY_SKIN_LAST_COMPLETED_DATE = "skin_last_completed_date";

    public Set<String> getCompletedMorningDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_COMPLETED_MORNING_DATES, new HashSet<>());
    }

    public void addCompletedMorningDate(Context context, String dateStr) {
        Set<String> current = new HashSet<>(getCompletedMorningDates(context));
        current.add(dateStr);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_COMPLETED_MORNING_DATES, current)
                .apply();
    }

    public Set<String> getCompletedEveningDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_COMPLETED_EVENING_DATES, new HashSet<>());
    }

    public void addCompletedEveningDate(Context context, String dateStr) {
        Set<String> current = new HashSet<>(getCompletedEveningDates(context));
        current.add(dateStr);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_COMPLETED_EVENING_DATES, current)
                .apply();
    }

    public int getSkinStreak(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SKIN_STREAK, 0);
    }

    public void setSkinStreak(Context context, int streak) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SKIN_STREAK, streak)
                .apply();
    }

    public String getSkinLastCompletedDate(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SKIN_LAST_COMPLETED_DATE, "");
    }

    public void setSkinLastCompletedDate(Context context, String dateStr) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SKIN_LAST_COMPLETED_DATE, dateStr)
                .apply();
    }

    private static final String KEY_SKIN_SOCIAL_COMPLETED_DATES = "skin_social_completed_dates";

    public Set<String> getSkinSocialCompletedDates(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SKIN_SOCIAL_COMPLETED_DATES, new HashSet<>());
    }

    public void addSkinSocialCompletedDate(Context context, String dateStr) {
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

    public boolean isMorningReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_MORNING_REMINDER, true);
    }

    public void setMorningReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_MORNING_REMINDER, enabled).apply();
    }

    public boolean isEveningReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_EVENING_REMINDER, true);
    }

    public void setEveningReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_EVENING_REMINDER, enabled).apply();
    }

    public boolean isLeadReminderEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ROUTINE_LEAD_REMINDER, false);
    }

    public void setLeadReminderEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ROUTINE_LEAD_REMINDER, enabled).apply();
    }

    public String getMorningReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ROUTINE_MORNING_TIME, "06:30");
    }

    public void setMorningReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROUTINE_MORNING_TIME, time).apply();
    }

    public String getEveningReminderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ROUTINE_EVENING_TIME, "21:45");
    }

    public void setEveningReminderTime(Context context, String time) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString(KEY_ROUTINE_EVENING_TIME, time).apply();
    }

    public Set<String> getMorningSteps(Context context) {
        Set<String> defaultSteps = new HashSet<>();
        defaultSteps.add("0:Cleanser:Sữa rửa mặt dịu nhẹ:true");
        defaultSteps.add("1:Toner:Cân bằng độ pH:true");
        defaultSteps.add("2:Serum:Vitamin C - trắng da:true");
        defaultSteps.add("3:Moisturizer:Kem dưỡng khóa ẩm:false");
        defaultSteps.add("4:Sunscreen:Chống nắng phổ rộng:true");
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_ROUTINE_MORNING_STEPS, defaultSteps);
    }

    public void setMorningSteps(Context context, Set<String> steps) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_ROUTINE_MORNING_STEPS, steps).apply();
    }

    public Set<String> getEveningSteps(Context context) {
        Set<String> defaultSteps = new HashSet<>();
        defaultSteps.add("0:Makeup Remover:Nước tẩy trang:true");
        defaultSteps.add("1:Cleanser:Sữa rửa mặt dịu nhẹ:true");
        defaultSteps.add("2:Toner:Cân bằng độ pH:true");
        defaultSteps.add("3:Serum:Retinol - chống lão hóa:true");
        defaultSteps.add("4:Moisturizer:Kem dưỡng đêm khóa ẩm:true");
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_ROUTINE_EVENING_STEPS, defaultSteps);
    }

    public void setEveningSteps(Context context, Set<String> steps) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_ROUTINE_EVENING_STEPS, steps).apply();
    }

    public Set<String> getCompletedStepIdsForDate(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet("completed_steps_" + date, new HashSet<>());
    }

    public void setCompletedStepIdsForDate(Context context, String date, Set<String> stepIds) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet("completed_steps_" + date, stepIds).apply();
    }

    public void setCompletedMorningDates(Context context, Set<String> dates) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_COMPLETED_MORNING_DATES, dates).apply();
    }

    public void setCompletedEveningDates(Context context, Set<String> dates) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putStringSet(KEY_COMPLETED_EVENING_DATES, dates).apply();
    }

    public boolean isMorningRewardAwarded(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("morning_reward_awarded_" + date, false);
    }

    public void setMorningRewardAwarded(Context context, String date, boolean awarded) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("morning_reward_awarded_" + date, awarded).apply();
    }

    public boolean isEveningRewardAwarded(Context context, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("evening_reward_awarded_" + date, false);
    }

    public void setEveningRewardAwarded(Context context, String date, boolean awarded) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean("evening_reward_awarded_" + date, awarded).apply();
    }

    public boolean isRoutineSubmitted(Context context, String type, String date) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(type + "_routine_submitted_" + date, false);
    }

    public void setRoutineSubmitted(Context context, String type, String date, boolean submitted) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(type + "_routine_submitted_" + date, submitted).apply();
    }

    // Accessors for notification settings
    public boolean isNotiEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_ENABLED, true);
    }

    public void setNotiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_ENABLED, enabled)
                .apply();
    }

    public boolean isSoundEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_SOUND, true);
    }

    public void setSoundEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_SOUND, enabled)
                .apply();
    }

    public boolean isVibrateEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_VIBRATE, true);
    }

    public void setVibrateEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_VIBRATE, enabled)
                .apply();
    }

    public boolean isLockScreenEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_LOCK_SCREEN, false);
    }

    public void setLockScreenEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_LOCK_SCREEN, enabled)
                .apply();
    }

    public boolean isOrderStatusEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_ORDER_STATUS, false);
    }

    public void setOrderStatusEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_ORDER_STATUS, enabled)
                .apply();
    }

    public boolean isPromotionEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_PROMOTION, true);
    }

    public void setPromotionEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_PROMOTION, enabled)
                .apply();
    }

    public String getPromotionFrequency(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NOTI_PROMOTION_FREQUENCY, "Mỗi ngày");
    }

    public void setPromotionFrequency(Context context, String freq) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NOTI_PROMOTION_FREQUENCY, freq)
                .apply();
    }

    public String getPromotionTimeRange(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_NOTI_PROMOTION_TIME_RANGE, "08:00 - 21:00");
    }

    public void setPromotionTimeRange(Context context, String range) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_NOTI_PROMOTION_TIME_RANGE, range)
                .apply();
    }

    public boolean isStaffMessageEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_STAFF_MESSAGE, true);
    }

    public void setStaffMessageEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_STAFF_MESSAGE, enabled)
                .apply();
    }

    public boolean isComplaintResponseEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_COMPLAINT_RESPONSE, true);
    }

    public String getCurrentUserId(Context context) {
        String email = getEmail(context);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("users.json")))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONArray usersJsonArray = new JSONArray(sb.toString());
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

    public boolean isSkinWeatherNotiEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_NOTI_SKIN_WEATHER, true);
    }

    public void setSkinWeatherNotiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_NOTI_SKIN_WEATHER, enabled)
                .apply();
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

    public String getSavedUserSkinType(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_USER_SKIN_TYPE, "Da hỗn hợp thiên dầu");
    }

    public void setSavedUserSkinType(Context context, String skinType) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_USER_SKIN_TYPE, skinType)
                .apply();
    }

    public String getSavedRecommendation(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_RECOMMENDATION, "");
    }

    public void setSavedRecommendation(Context context, String recommendation) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_RECOMMENDATION, recommendation)
                .apply();
    }

    public int getSavedSensitivity(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_SENSITIVITY, 50);
    }

    public void setSavedSensitivity(Context context, int sensitivity) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_SENSITIVITY, sensitivity)
                .apply();
    }

    public int getSavedHydration(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_HYDRATION, 50);
    }

    public void setSavedHydration(Context context, int hydration) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_HYDRATION, hydration)
                .apply();
    }

    public int getSavedElasticity(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_ELASTICITY, 75);
    }

    public void setSavedElasticity(Context context, int elasticity) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_ELASTICITY, elasticity)
                .apply();
    }

    public int getSavedSebum(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SAVED_SEBUM, 50);
    }

    public void setSavedSebum(Context context, int sebum) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SAVED_SEBUM, sebum)
                .apply();
    }

    public String getSavedSkinAreas(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_SAVED_SKIN_AREAS, "Độ ẩm và dầu phân bố không đều.");
    }

    public void setSavedSkinAreas(Context context, String skinAreas) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SAVED_SKIN_AREAS, skinAreas)
                .apply();
    }

    public Set<String> getSavedFlaggedGroups(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SAVED_FLAGGED_GROUPS, new HashSet<>());
    }

    public void setSavedFlaggedGroups(Context context, Set<String> flaggedGroups) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SAVED_FLAGGED_GROUPS, flaggedGroups)
                .apply();
    }

    public String getQuizHistoryList(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_QUIZ_HISTORY_LIST, "[]");
    }

    public void setQuizHistoryList(Context context, String historyListJson) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_QUIZ_HISTORY_LIST, historyListJson)
                .apply();
    }

    private static final String KEY_LAST_SKIN_TEST_TIME = "KEY_LAST_SKIN_TEST_TIME";
    private static final String KEY_HIDE_QUIZ_REMINDER_WEEKLY = "KEY_HIDE_QUIZ_REMINDER_WEEKLY";

    public long getLastSkinTestTime(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SKIN_TEST_TIME, 0L);
    }

    public void setLastSkinTestTime(Context context, long time) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_SKIN_TEST_TIME, time)
                .apply();
    }

    public boolean isQuizReminderDismissedWeekly(Context context) {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, false);
    }

    public void setQuizReminderDismissedWeekly(Context context, boolean dismissed) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, dismissed)
                .apply();
    }

    // --- Product Expiry Notification Settings ---
    public boolean isNotiExpiryEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_enabled", true);
    }

    public void setNotiExpiryEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_enabled", enabled)
                .apply();
    }

    public boolean isNotiExpiryWeek1(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_week1", true);
    }

    public void setNotiExpiryWeek1(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_week1", enabled)
                .apply();
    }

    public boolean isNotiExpiryWeek2(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean("noti_expiry_week2", true);
    }

    public void setNotiExpiryWeek2(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("noti_expiry_week2", enabled)
                .apply();
    }

    // Product-specific overrides (fallback to global defaults if not customized)
    public boolean getProductNotiEnabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_enabled_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryEnabled(context);
        }
    }

    public void setProductNotiEnabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_enabled_" + userId + "_" + productId, enabled)
                .apply();
    }

    public boolean getProductWeek1Enabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_week1_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryWeek1(context);
        }
    }

    public void setProductWeek1Enabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_week1_" + userId + "_" + productId, enabled)
                .apply();
    }

    public boolean getProductWeek2Enabled(Context context, String userId, String productId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String key = "expiry_custom_week2_" + userId + "_" + productId;
        if (prefs.contains(key)) {
            return prefs.getBoolean(key, true);
        } else {
            return isNotiExpiryWeek2(context);
        }
    }

    public void setProductWeek2Enabled(Context context, String userId, String productId, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("expiry_custom_week2_" + userId + "_" + productId, enabled)
                .apply();
    }
}
