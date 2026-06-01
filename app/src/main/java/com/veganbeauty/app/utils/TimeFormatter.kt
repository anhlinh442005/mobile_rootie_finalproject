package com.veganbeauty.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeFormatter {
    fun getTimeAgo(dateString: String): String {
        try {
            // Parse typical JSON date format "2026-05-21T01:26:55.000Z"
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            val past = format.parse(dateString) ?: return ""
            
            val now = Date()
            val seconds = abs(now.time - past.time) / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                seconds < 60 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days < 30 -> "$days ngày trước"
                days < 365 -> "${days / 30} tháng trước"
                else -> "${days / 365} năm trước"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}
