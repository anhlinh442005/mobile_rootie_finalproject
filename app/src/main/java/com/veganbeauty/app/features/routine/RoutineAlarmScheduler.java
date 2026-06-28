package com.veganbeauty.app.features.routine;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.veganbeauty.app.data.local.ProfileSession;

import java.util.Calendar;

public class RoutineAlarmScheduler {

    public static void scheduleRoutineAlarm(Context context, String type, int hour, int minute, boolean isLead) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RoutineAlarmReceiver.class);
        intent.putExtra("REMINDER_TYPE", type);
        intent.putExtra("IS_LEAD_REMINDER", isLead);
        
        int requestCode = "MORNING".equals(type) ? 1001 : 1002;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (SecurityException e) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    public static void cancelRoutineAlarm(Context context, String type) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, RoutineAlarmReceiver.class);
        int requestCode = "MORNING".equals(type) ? 1001 : 1002;
        int flags = PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static void rescheduleAlarms(Context context) {
        boolean morningEnabled = ProfileSession.isMorningReminderEnabled(context);
        boolean eveningEnabled = ProfileSession.isEveningReminderEnabled(context);
        boolean leadEnabled = ProfileSession.isLeadReminderEnabled(context);

        if (morningEnabled) {
            String morningTime = ProfileSession.getMorningReminderTime(context);
            String[] morningParts = morningTime.split(":");
            int mHour = 6;
            int mMinute = 30;
            try {
                if (morningParts.length > 0) mHour = Integer.parseInt(morningParts[0]);
                if (morningParts.length > 1) mMinute = Integer.parseInt(morningParts[1]);
            } catch (NumberFormatException ignored) {}

            if (leadEnabled) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, mHour);
                cal.set(Calendar.MINUTE, mMinute);
                cal.add(Calendar.MINUTE, -15);
                mHour = cal.get(Calendar.HOUR_OF_DAY);
                mMinute = cal.get(Calendar.MINUTE);
            }
            scheduleRoutineAlarm(context, "MORNING", mHour, mMinute, leadEnabled);
        } else {
            cancelRoutineAlarm(context, "MORNING");
        }

        if (eveningEnabled) {
            String eveningTime = ProfileSession.getEveningReminderTime(context);
            String[] eveningParts = eveningTime.split(":");
            int eHour = 21;
            int eMinute = 45;
            try {
                if (eveningParts.length > 0) eHour = Integer.parseInt(eveningParts[0]);
                if (eveningParts.length > 1) eMinute = Integer.parseInt(eveningParts[1]);
            } catch (NumberFormatException ignored) {}

            if (leadEnabled) {
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, eHour);
                cal.set(Calendar.MINUTE, eMinute);
                cal.add(Calendar.MINUTE, -15);
                eHour = cal.get(Calendar.HOUR_OF_DAY);
                eMinute = cal.get(Calendar.MINUTE);
            }
            scheduleRoutineAlarm(context, "EVENING", eHour, eMinute, leadEnabled);
        } else {
            cancelRoutineAlarm(context, "EVENING");
        }
    }
}
