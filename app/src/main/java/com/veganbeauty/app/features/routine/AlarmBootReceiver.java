package com.veganbeauty.app.features.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.veganbeauty.app.features.weather.DailySkinWeatherScheduler;

public class AlarmBootReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            try {
                // 1. Reschedule Skincare Routine alarms
                RoutineAlarmScheduler.rescheduleAlarms(context);
                Log.d(TAG, "Rescheduled skincare routine alarms successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to reschedule skincare routine alarms", e);
            }

            try {
                // 2. Reschedule Daily Skin/Weather notification
                if (com.veganbeauty.app.data.local.ProfileSession.isSkinWeatherNotiEnabled(context)) {
                    DailySkinWeatherScheduler.enableAndSync(context);
                }
                Log.d(TAG, "Rescheduled daily weather alarm successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to reschedule daily weather alarm", e);
            }
        }
    }
}
