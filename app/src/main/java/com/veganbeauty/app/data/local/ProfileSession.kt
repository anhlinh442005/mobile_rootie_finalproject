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

    fun setComplaintResponseEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTI_COMPLAINT_RESPONSE, enabled)
            .apply()
    }
}
