package com.veganbeauty.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object TimeFormatter {
    fun getTimeAgo(dateString: String): String {
        if (dateString.isBlank()) return ""
        try {
            var pastTimeMs: Long = 0
            // Try to parse as Unix timestamp first
            val asLong = dateString.toLongOrNull()
            if (asLong != null) {
                // Determine if it's seconds or milliseconds
                pastTimeMs = if (asLong < 10000000000L) asLong * 1000 else asLong
            } else {
                // Try to parse ISO 8601 formats
                val formats = arrayOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                )
                var parsedDate: Date? = null
                for (f in formats) {
                    try {
                        val sdf = SimpleDateFormat(f, Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        parsedDate = sdf.parse(dateString)
                        if (parsedDate != null) break
                    } catch (e: Exception) {
                        // Continue to next format
                    }
                }
                pastTimeMs = parsedDate?.time ?: return dateString // Return raw string if parsing totally fails
            }
            
            val now = Date().time
            val seconds = abs(now - pastTimeMs) / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            
            return when {
                seconds < 60 -> "Vừa xong"
                minutes < 60 -> "$minutes phút trước"
                hours < 24 -> "$hours giờ trước"
                days < 7 -> "$days ngày trước"
                days < 30 -> "${days / 7} tuần trước"
                days < 365 -> "${days / 30} tháng trước"
                else -> "${days / 365} năm trước"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return dateString
        }
    }
}
