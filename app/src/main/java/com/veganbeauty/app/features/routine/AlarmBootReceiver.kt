package com.veganbeauty.app.features.routine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.veganbeauty.app.features.weather.DailySkinWeatherScheduler

class AlarmBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmBootReceiver", "Received action: $action")
        
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            try {
                // 1. Reschedule Skincare Routine alarms
                RoutineAlarmScheduler.rescheduleAlarms(context)
                Log.d("AlarmBootReceiver", "Rescheduled skincare routine alarms successfully")
            } catch (e: Exception) {
                Log.e("AlarmBootReceiver", "Failed to reschedule skincare routine alarms", e)
            }

            try {
                // 2. Reschedule Daily Skin/Weather notification
                DailySkinWeatherScheduler.scheduleDailyNotification(context)
                Log.d("AlarmBootReceiver", "Rescheduled daily weather alarm successfully")
            } catch (e: Exception) {
                Log.e("AlarmBootReceiver", "Failed to reschedule daily weather alarm", e)
            }
        }
    }
}
