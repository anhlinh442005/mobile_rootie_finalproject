package com.veganbeauty.app.features.weather;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Chặn gửi trùng thông báo thời tiết & da trong cùng một ngày.
 */
final class WeatherNotificationGuard {

    private static final String PREFS = "rootie_prefs";
    private static final String KEY_SENT_DAY = "weather_daily_sent_day";
    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);

    private WeatherNotificationGuard() {
    }

    static boolean wasDeliveredToday(Context context) {
        if (context == null) {
            return false;
        }
        String today = todayKey();
        return today.equals(context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SENT_DAY, ""));
    }

    static boolean tryBeginDelivery(Context context, boolean force) {
        if (force) {
            return true;
        }
        if (wasDeliveredToday(context)) {
            return false;
        }
        return IN_FLIGHT.compareAndSet(false, true);
    }

    static void markDeliveredToday(Context context) {
        if (context == null) {
            return;
        }
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SENT_DAY, todayKey())
                .apply();
    }

    static void endDelivery() {
        IN_FLIGHT.set(false);
    }

    private static String todayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }
}
