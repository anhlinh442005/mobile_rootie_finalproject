package com.veganbeauty.app.features.routine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object RoutineAlarmScheduler {
    fun scheduleRoutineAlarm(context: Context, type: String, hour: Int, minute: Int, isLead: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", type)
            putExtra("IS_LEAD_REMINDER", isLead)
        }
        val requestCode = if (type == "MORNING") 1001 else 1002
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if SCHEDULE_EXACT_ALARM permission isn't granted yet
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelRoutineAlarm(context: Context, type: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, RoutineAlarmReceiver::class.java)
        val requestCode = if (type == "MORNING") 1001 else 1002
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rescheduleAlarms(context: Context) {
        val morningEnabled = com.veganbeauty.app.data.local.ProfileSession.isMorningReminderEnabled(context)
        val eveningEnabled = com.veganbeauty.app.data.local.ProfileSession.isEveningReminderEnabled(context)
        val leadEnabled = com.veganbeauty.app.data.local.ProfileSession.isLeadReminderEnabled(context)

        if (morningEnabled) {
            val morningTime = com.veganbeauty.app.data.local.ProfileSession.getMorningReminderTime(context)
            val morningParts = morningTime.split(":")
            var mHour = morningParts.getOrNull(0)?.toIntOrNull() ?: 6
            var mMinute = morningParts.getOrNull(1)?.toIntOrNull() ?: 30
            if (leadEnabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, mHour)
                    set(Calendar.MINUTE, mMinute)
                }
                cal.add(Calendar.MINUTE, -15)
                mHour = cal.get(Calendar.HOUR_OF_DAY)
                mMinute = cal.get(Calendar.MINUTE)
            }
            scheduleRoutineAlarm(context, "MORNING", mHour, mMinute, leadEnabled)
        } else {
            cancelRoutineAlarm(context, "MORNING")
        }

        if (eveningEnabled) {
            val eveningTime = com.veganbeauty.app.data.local.ProfileSession.getEveningReminderTime(context)
            val eveningParts = eveningTime.split(":")
            var eHour = eveningParts.getOrNull(0)?.toIntOrNull() ?: 21
            var eMinute = eveningParts.getOrNull(1)?.toIntOrNull() ?: 45
            if (leadEnabled) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, eHour)
                    set(Calendar.MINUTE, eMinute)
                }
                cal.add(Calendar.MINUTE, -15)
                eHour = cal.get(Calendar.HOUR_OF_DAY)
                eMinute = cal.get(Calendar.MINUTE)
            }
            scheduleRoutineAlarm(context, "EVENING", eHour, eMinute, leadEnabled)
        } else {
            cancelRoutineAlarm(context, "EVENING")
        }
    }
}
