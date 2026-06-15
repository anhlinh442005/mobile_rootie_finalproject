package com.veganbeauty.app.features.account.notification

import java.text.SimpleDateFormat
import java.util.*

object NotificationDateHelper {

    fun getSectionFromTime(timeStr: String): String {
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateTimeFormat.parse(timeStr) ?: return "Cũ hơn"
            val now = Date()

            val calNow = Calendar.getInstance().apply { time = now }
            val calThen = Calendar.getInstance().apply { time = date }

            val yearNow = calNow.get(Calendar.YEAR)
            val dayNow = calNow.get(Calendar.DAY_OF_YEAR)

            val yearThen = calThen.get(Calendar.YEAR)
            val dayThen = calThen.get(Calendar.DAY_OF_YEAR)

            if (yearNow == yearThen && dayNow == dayThen) {
                "Hôm nay"
            } else {
                val calYesterday = Calendar.getInstance().apply {
                    time = now
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val yearYest = calYesterday.get(Calendar.YEAR)
                val dayYest = calYesterday.get(Calendar.DAY_OF_YEAR)

                if (yearThen == yearYest && dayThen == dayYest) {
                    "Hôm qua"
                } else {
                    "Cũ hơn"
                }
            }
        } catch (e: Exception) {
            // Fallback for old HH:mm format or parse error
            "Cũ hơn"
        }
    }

    fun getDisplayTime(timeStr: String): String {
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = dateTimeFormat.parse(timeStr) ?: return timeStr
            val now = Date()

            val calNow = Calendar.getInstance().apply { time = now }
            val calThen = Calendar.getInstance().apply { time = date }

            val yearNow = calNow.get(Calendar.YEAR)
            val dayNow = calNow.get(Calendar.DAY_OF_YEAR)

            val yearThen = calThen.get(Calendar.YEAR)
            val dayThen = calThen.get(Calendar.DAY_OF_YEAR)

            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timePart = timeFormatter.format(date)

            if (yearNow == yearThen && dayNow == dayThen) {
                timePart
            } else {
                val calYesterday = Calendar.getInstance().apply {
                    time = now
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                if (yearThen == calYesterday.get(Calendar.YEAR) &&
                    dayThen == calYesterday.get(Calendar.DAY_OF_YEAR)) {
                    "Hôm qua, $timePart"
                } else {
                    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    dateFormatter.format(date)
                }
            }
        } catch (e: Exception) {
            timeStr
        }
    }
}
