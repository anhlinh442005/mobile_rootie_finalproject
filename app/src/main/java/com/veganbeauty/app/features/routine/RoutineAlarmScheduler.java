package com.veganbeauty.app.features.routine;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.account.notification.NotificationScheduleHelper;

import java.util.Calendar;

public class RoutineAlarmScheduler {

    private static final String TAG = "RoutineAlarmScheduler";
    private static final int REQUEST_MORNING_MAIN = 1001;
    private static final int REQUEST_EVENING_MAIN = 1002;
    private static final int REQUEST_MORNING_LEAD = 1003;
    private static final int REQUEST_EVENING_LEAD = 1004;

    public static void scheduleRoutineAlarm(Context context, String type, int hour, int minute, boolean isLead) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        Bundle extras = new Bundle();
        extras.putString("REMINDER_TYPE", type);
        extras.putBoolean("IS_LEAD_REMINDER", isLead);

        NotificationScheduleHelper.scheduleBroadcastAlarm(
                context,
                requestCodeFor(type, isLead),
                RoutineAlarmReceiver.class,
                extras,
                calendar.getTimeInMillis(),
                "SKIN_REMINDER"
        );
        Log.d(TAG, "Scheduled " + type + (isLead ? " lead" : " main")
                + " at " + hour + ":" + String.format("%02d", minute)
                + " next=" + calendar.getTime());
    }

    public static void cancelRoutineAlarm(Context context, String type) {
        cancelSingleAlarm(context, type, false);
        cancelSingleAlarm(context, type, true);
    }

    private static void cancelSingleAlarm(Context context, String type, boolean isLead) {
        NotificationScheduleHelper.cancelBroadcastAlarm(
                context, requestCodeFor(type, isLead), RoutineAlarmReceiver.class);
    }

    private static int requestCodeFor(String type, boolean isLead) {
        if ("MORNING".equals(type)) {
            return isLead ? REQUEST_MORNING_LEAD : REQUEST_MORNING_MAIN;
        }
        return isLead ? REQUEST_EVENING_LEAD : REQUEST_EVENING_MAIN;
    }

    public static void rescheduleAlarms(Context context) {
        if (!ProfileSession.isNotiEnabled(context)) {
            cancelRoutineAlarm(context, "MORNING");
            cancelRoutineAlarm(context, "EVENING");
            return;
        }

        NotificationScheduleHelper.remindExactAlarmIfNeeded(context);

        boolean morningEnabled = ProfileSession.isMorningReminderEnabled(context);
        boolean eveningEnabled = ProfileSession.isEveningReminderEnabled(context);
        boolean leadEnabled = ProfileSession.isLeadReminderEnabled(context);

        if (morningEnabled) {
            int[] morning = parseTime(ProfileSession.getMorningReminderTime(context), 6, 30);
            scheduleRoutineAlarm(context, "MORNING", morning[0], morning[1], false);
            if (leadEnabled) {
                int[] lead = subtractMinutes(morning[0], morning[1], 15);
                scheduleRoutineAlarm(context, "MORNING", lead[0], lead[1], true);
            } else {
                cancelSingleAlarm(context, "MORNING", true);
            }
        } else {
            cancelRoutineAlarm(context, "MORNING");
        }

        if (eveningEnabled) {
            int[] evening = parseTime(ProfileSession.getEveningReminderTime(context), 21, 45);
            scheduleRoutineAlarm(context, "EVENING", evening[0], evening[1], false);
            if (leadEnabled) {
                int[] lead = subtractMinutes(evening[0], evening[1], 15);
                scheduleRoutineAlarm(context, "EVENING", lead[0], lead[1], true);
            } else {
                cancelSingleAlarm(context, "EVENING", true);
            }
        } else {
            cancelRoutineAlarm(context, "EVENING");
        }
    }

    private static int[] parseTime(String time, int defaultHour, int defaultMinute) {
        int hour = defaultHour;
        int minute = defaultMinute;
        if (time != null) {
            String[] parts = time.split(":");
            try {
                if (parts.length > 0) hour = Integer.parseInt(parts[0].trim());
                if (parts.length > 1) minute = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return new int[]{hour, minute};
    }

    private static int[] subtractMinutes(int hour, int minute, int delta) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.MINUTE, -delta);
        return new int[]{cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)};
    }
}
