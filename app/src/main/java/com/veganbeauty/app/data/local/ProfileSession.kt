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
}
