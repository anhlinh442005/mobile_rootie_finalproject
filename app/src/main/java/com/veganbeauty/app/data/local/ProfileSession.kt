package com.veganbeauty.app.data.local

import android.content.Context

object ProfileSession {
    private const val PREFS_NAME = "rootie_profile_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_FULLNAME = "fullname"
    private const val KEY_PHONE = "phone"
    private const val KEY_EMAIL = "email"
    private const val KEY_FAST_LOGIN = "fast_login"
    private const val KEY_CCCD = "cccd"
    private const val KEY_ADDRESS = "address"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LAST_LOGIN = "last_login"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_USER_ID = "user_id"
    
    // Notification setting keys
    private const val KEY_NOTI_ENABLED = "noti_enabled"
    private const val KEY_NOTI_SOUND = "noti_sound"
    private const val KEY_NOTI_VIBRATE = "noti_vibrate"
    private const val KEY_NOTI_LOCK_SCREEN = "noti_lock_screen"
    private const val KEY_NOTI_ORDER_STATUS = "noti_order_status"
    private const val KEY_NOTI_PROMOTION = "noti_promotion"
    private const val KEY_NOTI_PROMOTION_FREQUENCY = "noti_promotion_frequency"
    private const val KEY_NOTI_PROMOTION_TIME_RANGE = "noti_promotion_time_range"
    private const val KEY_NOTI_STAFF_MESSAGE = "noti_staff_message"
    private const val KEY_NOTI_COMPLAINT_RESPONSE = "noti_complaint_response"
    private const val KEY_NOTI_SKIN_WEATHER = "noti_skin_weather"

    fun getUserId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_ID, "test_001") ?: "test_001"
    }

    fun setUserId(context: Context, userId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUsername(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USERNAME, "mrbeanintheworld") ?: "mrbeanintheworld"
    }

    fun setUsername(context: Context, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getFullName(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FULLNAME, "Sông Khánh Bình") ?: "Sông Khánh Bình"
    }

    fun setFullName(context: Context, fullName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FULLNAME, fullName)
            .apply()
    }

    fun getPhone(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PHONE, "0123456789") ?: "0123456789"
    }

    fun setPhone(context: Context, phone: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PHONE, phone)
            .apply()
    }

    fun getEmail(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EMAIL, "binhsk23411@st.uel.edu.vn") ?: "binhsk23411@st.uel.edu.vn"
    }

    fun setEmail(context: Context, email: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_EMAIL, email)
            .apply()
    }

    fun isFastLoginEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FAST_LOGIN, true)
    }

    fun setFastLoginEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FAST_LOGIN, enabled)
            .apply()
    }

    fun getCCCD(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CCCD, "123456789999") ?: "123456789999"
    }

    fun setCCCD(context: Context, cccd: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CCCD, cccd)
            .apply()
    }

    fun getAddress(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADDRESS, "Số 12 Đường 3, TP. Thủ Đức, TP. Hồ Chí Minh") ?: "Số 12 Đường 3, TP. Thủ Đức, TP. Hồ Chí Minh"
    }

    fun setAddress(context: Context, address: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADDRESS, address)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val lastLogin = prefs.getLong(KEY_LAST_LOGIN, 0L)
        val oneHour = 60 * 60 * 1000L
        return isLoggedIn && (System.currentTimeMillis() - lastLogin < oneHour)
    }

    fun setLoggedIn(context: Context, isLoggedIn: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
            .apply()
    }

    fun getAvatar(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_AVATAR, "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg") ?: "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg"
    }

    fun setAvatar(context: Context, avatarUrl: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AVATAR, avatarUrl)
            .apply()
    }

    private const val KEY_PRIMARY_IMAGE = "primary_image"

    fun getPrimaryImage(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRIMARY_IMAGE, "https://i.pinimg.com/1200x/21/2e/de/212ede6c525fcf95dbfa0a7d976beaa2.jpg") ?: "https://i.pinimg.com/1200x/21/2e/de/212ede6c525fcf95dbfa0a7d976beaa2.jpg"
    }

    fun setPrimaryImage(context: Context, primaryImage: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRIMARY_IMAGE, primaryImage)
            .apply()
    }

    private const val KEY_BIO = "bio"

    fun getBio(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BIO, "Làm đẹp không phải để ai ngắm, mà để mình vui.") ?: "Làm đẹp không phải để ai ngắm, mà để mình vui."
    }

    fun setBio(context: Context, bio: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BIO, bio)
            .apply()
    }

    // --- Skincare Routine & Streak Helpers ---
    private const val KEY_COMPLETED_MORNING_DATES = "completed_morning_dates"
    private const val KEY_COMPLETED_EVENING_DATES = "completed_evening_dates"
    private const val KEY_SKIN_STREAK = "skin_streak"
    private const val KEY_SKIN_LAST_COMPLETED_DATE = "skin_last_completed_date"

    fun getCompletedMorningDates(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_COMPLETED_MORNING_DATES, emptySet()) ?: emptySet()
    }

    fun addCompletedMorningDate(context: Context, dateStr: String) {
        val current = getCompletedMorningDates(context).toMutableSet()
        current.add(dateStr)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_COMPLETED_MORNING_DATES, current)
            .apply()
    }

    fun getCompletedEveningDates(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_COMPLETED_EVENING_DATES, emptySet()) ?: emptySet()
    }

    fun addCompletedEveningDate(context: Context, dateStr: String) {
        val current = getCompletedEveningDates(context).toMutableSet()
        current.add(dateStr)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_COMPLETED_EVENING_DATES, current)
            .apply()
    }

    fun getSkinStreak(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SKIN_STREAK, 0)
    }

    fun setSkinStreak(context: Context, streak: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SKIN_STREAK, streak)
            .apply()
    }

    fun getSkinLastCompletedDate(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SKIN_LAST_COMPLETED_DATE, "") ?: ""
    }

    fun setSkinLastCompletedDate(context: Context, dateStr: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SKIN_LAST_COMPLETED_DATE, dateStr)
            .apply()
    }

    private const val KEY_SKIN_SOCIAL_COMPLETED_DATES = "skin_social_completed_dates"

    fun getSkinSocialCompletedDates(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SKIN_SOCIAL_COMPLETED_DATES, emptySet()) ?: emptySet()
    }

    fun addSkinSocialCompletedDate(context: Context, dateStr: String) {
        val current = getSkinSocialCompletedDates(context).toMutableSet()
        current.add(dateStr)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SKIN_SOCIAL_COMPLETED_DATES, current)
            .apply()
    }

    // --- Routine Settings Helpers ---
    private const val KEY_ROUTINE_MORNING_REMINDER = "routine_morning_reminder"
    private const val KEY_ROUTINE_EVENING_REMINDER = "routine_evening_reminder"
    private const val KEY_ROUTINE_LEAD_REMINDER = "routine_lead_reminder"
    private const val KEY_ROUTINE_MORNING_TIME = "routine_morning_time"
    private const val KEY_ROUTINE_EVENING_TIME = "routine_evening_time"
    private const val KEY_ROUTINE_MORNING_STEPS = "routine_morning_steps"
    private const val KEY_ROUTINE_EVENING_STEPS = "routine_evening_steps"

    fun isMorningReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ROUTINE_MORNING_REMINDER, true)
    }

    fun setMorningReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ROUTINE_MORNING_REMINDER, enabled).apply()
    }

    fun isEveningReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ROUTINE_EVENING_REMINDER, true)
    }

    fun setEveningReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ROUTINE_EVENING_REMINDER, enabled).apply()
    }

    fun isLeadReminderEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ROUTINE_LEAD_REMINDER, false)
    }

    fun setLeadReminderEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ROUTINE_LEAD_REMINDER, enabled).apply()
    }

    fun getMorningReminderTime(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROUTINE_MORNING_TIME, "06:30") ?: "06:30"
    }

    fun setMorningReminderTime(context: Context, time: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ROUTINE_MORNING_TIME, time).apply()
    }

    fun getEveningReminderTime(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROUTINE_EVENING_TIME, "21:45") ?: "21:45"
    }

    fun setEveningReminderTime(context: Context, time: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ROUTINE_EVENING_TIME, time).apply()
    }

    fun getMorningSteps(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ROUTINE_MORNING_STEPS, setOf(
                "0:Cleanser:Sữa rửa mặt dịu nhẹ:true",
                "1:Toner:Cân bằng độ pH:true",
                "2:Serum:Vitamin C - trắng da:true",
                "3:Moisturizer:Kem dưỡng khóa ẩm:false",
                "4:Sunscreen:Chống nắng phổ rộng:true"
            )) ?: emptySet()
    }

    fun setMorningSteps(context: Context, steps: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_ROUTINE_MORNING_STEPS, steps).apply()
    }

    fun getEveningSteps(context: Context): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ROUTINE_EVENING_STEPS, setOf(
                "0:Makeup Remover:Nước tẩy trang:true",
                "1:Cleanser:Sữa rửa mặt dịu nhẹ:true",
                "2:Toner:Cân bằng độ pH:true",
                "3:Serum:Retinol - chống lão hóa:true",
                "4:Moisturizer:Kem dưỡng đêm khóa ẩm:true"
            )) ?: emptySet()
    }

    fun setEveningSteps(context: Context, steps: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_ROUTINE_EVENING_STEPS, steps).apply()
    }

    fun getCompletedStepIdsForDate(context: Context, date: String): Set<String> {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet("completed_steps_$date", emptySet()) ?: emptySet()
    }

    fun setCompletedStepIdsForDate(context: Context, date: String, stepIds: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet("completed_steps_$date", stepIds).apply()
    }

    fun setCompletedMorningDates(context: Context, dates: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_COMPLETED_MORNING_DATES, dates).apply()
    }

    fun setCompletedEveningDates(context: Context, dates: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putStringSet(KEY_COMPLETED_EVENING_DATES, dates).apply()
    }

    fun isMorningRewardAwarded(context: Context, date: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("morning_reward_awarded_$date", false)
    }

    fun setMorningRewardAwarded(context: Context, date: String, awarded: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("morning_reward_awarded_$date", awarded).apply()
    }

    fun isEveningRewardAwarded(context: Context, date: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("evening_reward_awarded_$date", false)
    }

    fun setEveningRewardAwarded(context: Context, date: String, awarded: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("evening_reward_awarded_$date", awarded).apply()
    }

    fun isRoutineSubmitted(context: Context, type: String, date: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("${type}_routine_submitted_$date", false)
    }

    fun setRoutineSubmitted(context: Context, type: String, date: String, submitted: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("${type}_routine_submitted_$date", submitted).apply()
    }

    // Accessors for notification settings
    fun isNotiEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_ENABLED, true)
    }

    fun setNotiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_ENABLED, enabled)
            .apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_SOUND, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_SOUND, enabled)
            .apply()
    }

    fun isVibrateEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_VIBRATE, true)
    }

    fun setVibrateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_VIBRATE, enabled)
            .apply()
    }

    fun isLockScreenEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_LOCK_SCREEN, false)
    }

    fun setLockScreenEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_LOCK_SCREEN, enabled)
            .apply()
    }

    fun isOrderStatusEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_ORDER_STATUS, false)
    }

    fun setOrderStatusEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_ORDER_STATUS, enabled)
            .apply()
    }

    fun isPromotionEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_PROMOTION, true)
    }

    fun setPromotionEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_PROMOTION, enabled)
            .apply()
    }

    fun getPromotionFrequency(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTI_PROMOTION_FREQUENCY, "Mỗi ngày") ?: "Mỗi ngày"
    }

    fun setPromotionFrequency(context: Context, freq: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTI_PROMOTION_FREQUENCY, freq)
            .apply()
    }

    fun getPromotionTimeRange(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NOTI_PROMOTION_TIME_RANGE, "08:00 - 21:00") ?: "08:00 - 21:00"
    }

    fun setPromotionTimeRange(context: Context, range: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTI_PROMOTION_TIME_RANGE, range)
            .apply()
    }

    fun isStaffMessageEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_STAFF_MESSAGE, true)
    }

    fun setStaffMessageEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_STAFF_MESSAGE, enabled)
            .apply()
    }

    fun isComplaintResponseEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_COMPLAINT_RESPONSE, true)
    }

    fun getCurrentUserId(context: Context): String {
        val email = getEmail(context)
        try {
            val usersJsonStr = context.assets.open("users.json").bufferedReader().use { it.readText() }
            val usersJsonArray = org.json.JSONArray(usersJsonStr)
            for (i in 0 until usersJsonArray.length()) {
                val obj = usersJsonArray.getJSONObject(i)
                if (obj.optString("email") == email) {
                    return obj.optString("user_id", "test_001")
                }
            }
        } catch(e: Exception) {}
        return "test_001"
    }

    fun isSkinWeatherNotiEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTI_SKIN_WEATHER, true)
    }

    fun setSkinWeatherNotiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_SKIN_WEATHER, enabled)
            .apply()
    }

    // --- Skincare Quiz & Skin Profile Storage ---
    private const val QUIZ_PREFS_NAME = "RootieQuizPrefs"
    private const val KEY_SAVED_USER_SKIN_TYPE = "SAVED_USER_SKIN_TYPE"
    private const val KEY_SAVED_RECOMMENDATION = "SAVED_RECOMMENDATION"
    private const val KEY_SAVED_SENSITIVITY = "SAVED_SENSITIVITY"
    private const val KEY_SAVED_HYDRATION = "SAVED_HYDRATION"
    private const val KEY_SAVED_ELASTICITY = "SAVED_ELASTICITY"
    private const val KEY_SAVED_SEBUM = "SAVED_SEBUM"
    private const val KEY_SAVED_SKIN_AREAS = "SAVED_SKIN_AREAS"
    private const val KEY_SAVED_FLAGGED_GROUPS = "SAVED_FLAGGED_GROUPS"
    private const val KEY_QUIZ_HISTORY_LIST = "QUIZ_HISTORY_LIST"

    fun getSavedUserSkinType(context: Context): String {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED_USER_SKIN_TYPE, "Da hỗn hợp thiên dầu") ?: "Da hỗn hợp thiên dầu"
    }

    fun setSavedUserSkinType(context: Context, skinType: String) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_USER_SKIN_TYPE, skinType)
            .apply()
    }

    fun getSavedRecommendation(context: Context): String {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED_RECOMMENDATION, "") ?: ""
    }

    fun setSavedRecommendation(context: Context, recommendation: String) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_RECOMMENDATION, recommendation)
            .apply()
    }

    fun getSavedSensitivity(context: Context): Int {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SAVED_SENSITIVITY, 50)
    }

    fun setSavedSensitivity(context: Context, sensitivity: Int) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SAVED_SENSITIVITY, sensitivity)
            .apply()
    }

    fun getSavedHydration(context: Context): Int {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SAVED_HYDRATION, 50)
    }

    fun setSavedHydration(context: Context, hydration: Int) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SAVED_HYDRATION, hydration)
            .apply()
    }

    fun getSavedElasticity(context: Context): Int {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SAVED_ELASTICITY, 75)
    }

    fun setSavedElasticity(context: Context, elasticity: Int) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SAVED_ELASTICITY, elasticity)
            .apply()
    }

    fun getSavedSebum(context: Context): Int {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_SAVED_SEBUM, 50)
    }

    fun setSavedSebum(context: Context, sebum: Int) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_SAVED_SEBUM, sebum)
            .apply()
    }

    fun getSavedSkinAreas(context: Context): String {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED_SKIN_AREAS, "Độ ẩm và dầu phân bố không đều.") ?: "Độ ẩm và dầu phân bố không đều."
    }

    fun setSavedSkinAreas(context: Context, skinAreas: String) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SAVED_SKIN_AREAS, skinAreas)
            .apply()
    }

    fun getSavedFlaggedGroups(context: Context): Set<String> {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_SAVED_FLAGGED_GROUPS, emptySet()) ?: emptySet()
    }

    fun setSavedFlaggedGroups(context: Context, flaggedGroups: Set<String>) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_SAVED_FLAGGED_GROUPS, flaggedGroups)
            .apply()
    }

    fun getQuizHistoryList(context: Context): String {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_QUIZ_HISTORY_LIST, "[]") ?: "[]"
    }

    fun setQuizHistoryList(context: Context, historyListJson: String) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_QUIZ_HISTORY_LIST, historyListJson)
            .apply()
    }

    private const val KEY_LAST_SKIN_TEST_TIME = "KEY_LAST_SKIN_TEST_TIME"
    private const val KEY_HIDE_QUIZ_REMINDER_WEEKLY = "KEY_HIDE_QUIZ_REMINDER_WEEKLY"

    fun getLastSkinTestTime(context: Context): Long {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SKIN_TEST_TIME, 0L)
    }

    fun setLastSkinTestTime(context: Context, time: Long) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SKIN_TEST_TIME, time)
            .apply()
    }

    fun isQuizReminderDismissedWeekly(context: Context): Boolean {
        return context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, false)
    }

    fun setQuizReminderDismissedWeekly(context: Context, dismissed: Boolean) {
        context.getSharedPreferences(QUIZ_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIDE_QUIZ_REMINDER_WEEKLY, dismissed)
            .apply()
    }

    // --- Product Expiry Notification Settings ---
    fun isNotiExpiryEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("noti_expiry_enabled", true)
    }

    fun setNotiExpiryEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("noti_expiry_enabled", enabled)
            .apply()
    }

    fun isNotiExpiryWeek1(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("noti_expiry_week1", true)
    }

    fun setNotiExpiryWeek1(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("noti_expiry_week1", enabled)
            .apply()
    }

    fun isNotiExpiryWeek2(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("noti_expiry_week2", true)
    }

    fun setNotiExpiryWeek2(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("noti_expiry_week2", enabled)
            .apply()
    }

    // Product-specific overrides (fallback to global defaults if not customized)
    fun getProductNotiEnabled(context: Context, userId: String, productId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "expiry_custom_enabled_${userId}_${productId}"
        return if (prefs.contains(key)) {
            prefs.getBoolean(key, true)
        } else {
            isNotiExpiryEnabled(context)
        }
    }

    fun setProductNotiEnabled(context: Context, userId: String, productId: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("expiry_custom_enabled_${userId}_${productId}", enabled)
            .apply()
    }

    fun getProductWeek1Enabled(context: Context, userId: String, productId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "expiry_custom_week1_${userId}_${productId}"
        return if (prefs.contains(key)) {
            prefs.getBoolean(key, true)
        } else {
            isNotiExpiryWeek1(context)
        }
    }

    fun setProductWeek1Enabled(context: Context, userId: String, productId: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("expiry_custom_week1_${userId}_${productId}", enabled)
            .apply()
    }

    fun getProductWeek2Enabled(context: Context, userId: String, productId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "expiry_custom_week2_${userId}_${productId}"
        return if (prefs.contains(key)) {
            prefs.getBoolean(key, true)
        } else {
            isNotiExpiryWeek2(context)
        }
    }

    fun setProductWeek2Enabled(context: Context, userId: String, productId: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("expiry_custom_week2_${userId}_${productId}", enabled)
            .apply()
    }

    // --- Guest checkout phone for order tracking ---
    // Stores the last guest's phone number so they can track their orders
    // without being logged in. Cleared on login.
    private const val KEY_GUEST_PHONE = "guest_phone"

    fun getGuestPhone(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_GUEST_PHONE, "") ?: ""
    }

    fun setGuestPhone(context: Context, phone: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GUEST_PHONE, phone)
            .apply()
    }

    fun clearGuestPhone(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_GUEST_PHONE)
            .apply()
    }
}
