package com.veganbeauty.app.features.weather;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.veganbeauty.app.features.account.notification.NotificationScheduleHelper;

import java.util.Calendar;

public class DailySkinWeatherScheduler {
    private static final int ALARM_REQ_CODE = 2001;
    private static final String TAG = "DailySkinWeatherScheduler";
    public static final String EXTRA_FORCE_DELIVERY = "force_delivery";

    public static void scheduleDailyNotification(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        NotificationScheduleHelper.scheduleBroadcastAlarm(
                context,
                ALARM_REQ_CODE,
                DailySkinWeatherReceiver.class,
                null,
                calendar.getTimeInMillis(),
                "WEATHER_FORECAST"
        );
        Log.d(TAG, "Scheduled daily skin weather alarm. Next: " + calendar.getTime());
    }

    public static void cancelDailyNotification(Context context) {
        NotificationScheduleHelper.cancelBroadcastAlarm(
                context, ALARM_REQ_CODE, DailySkinWeatherReceiver.class);
        Log.d(TAG, "Cancelled daily skin weather alarm.");
    }

    public static void enableAndSync(Context context) {
        scheduleDailyNotification(context);
        NotificationScheduleHelper.remindExactAlarmIfNeeded(context);
    }

    /** Chỉ đặt lịch 7:00 — không gửi thông báo ngay. */
    public static void scheduleOnly(Context context) {
        scheduleDailyNotification(context);
    }

    /** Gửi ngay thông báo thời tiết & da (dùng cho gửi thử / bù trong ngày). */
    public static void triggerNow(Context context) {
        triggerNow(context, false);
    }

    /** @param force true = gửi thử ngay, không bị chặn bởi bản ghi đã gửi hôm nay */
    public static void triggerNow(Context context, boolean force) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        Intent intent = new Intent(app, DailySkinWeatherReceiver.class);
        if (force) {
            intent.putExtra(EXTRA_FORCE_DELIVERY, true);
        }
        sendReceiverBroadcast(app, intent);
        Log.d(TAG, "triggerNow force=" + force);
    }

    private static void sendReceiverBroadcast(Context app, Intent intent) {
        try {
            intent.setPackage(app.getPackageName());
            app.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, "sendBroadcast failed", e);
        }
    }
}
