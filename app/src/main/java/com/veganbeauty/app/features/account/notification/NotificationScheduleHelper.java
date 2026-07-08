package com.veganbeauty.app.features.account.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.veganbeauty.app.MainActivity;

/**
 * Đặt alarm chính xác (setAlarmClock) để thông báo đến đúng giờ trên Android 12+.
 */
public final class NotificationScheduleHelper {

    private static final String TAG = "NotiScheduleHelper";
    private static final String PREF_WARNED_EXACT_ALARM = "warned_exact_alarm_v1";

    private NotificationScheduleHelper() {
    }

    public static boolean canScheduleExactAlarms(@NonNull Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static void openExactAlarmSettings(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Cannot open exact alarm settings", e);
            openAppSettings(context);
        }
    }

    public static void openAppSettings(@NonNull Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    /** Nhắc một lần nếu chưa có quyền alarm chính xác. */
    public static void remindExactAlarmIfNeeded(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || canScheduleExactAlarms(context)) {
            return;
        }
        Context app = context.getApplicationContext();
        if (app.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .getBoolean(PREF_WARNED_EXACT_ALARM, false)) {
            return;
        }
        app.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(PREF_WARNED_EXACT_ALARM, true)
                .apply();
        openExactAlarmSettings(context);
    }

    public static void scheduleBroadcastAlarm(
            @NonNull Context context,
            int requestCode,
            @NonNull Class<?> receiverClass,
            @Nullable Bundle extras,
            long triggerAtMillis,
            @Nullable String navigateToForShowIntent
    ) {
        Context app = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent alarmIntent = new Intent(app, receiverClass);
        if (extras != null) {
            alarmIntent.putExtras(extras);
        }
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent operation = PendingIntent.getBroadcast(app, requestCode, alarmIntent, flags);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Intent showIntent = new Intent(app, MainActivity.class);
                showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (navigateToForShowIntent != null && !navigateToForShowIntent.isEmpty()) {
                    showIntent.putExtra("NAVIGATE_TO", navigateToForShowIntent);
                }
                PendingIntent showPi = PendingIntent.getActivity(
                        app,
                        requestCode + 10_000,
                        showIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                AlarmManager.AlarmClockInfo clockInfo =
                        new AlarmManager.AlarmClockInfo(triggerAtMillis, showPi);
                alarmManager.setAlarmClock(clockInfo, operation);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
            }
            Log.d(TAG, "Scheduled alarm req=" + requestCode + " at " + triggerAtMillis);
        } catch (SecurityException e) {
            Log.e(TAG, "Exact alarm denied, using inexact fallback req=" + requestCode, e);
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
        }
    }

    public static void cancelBroadcastAlarm(
            @NonNull Context context,
            int requestCode,
            @NonNull Class<?> receiverClass
    ) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(context, receiverClass);
        int flags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}
