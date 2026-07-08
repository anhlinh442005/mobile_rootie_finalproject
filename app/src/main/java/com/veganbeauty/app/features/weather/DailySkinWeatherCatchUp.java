package com.veganbeauty.app.features.weather;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper;

import java.util.Calendar;

/**
 * Gửi bù thông báo thời tiết & da nếu đã qua 07:00 hôm nay mà alarm chưa kịp chạy.
 */
public final class DailySkinWeatherCatchUp {

    private static final String TAG = "DailySkinWeatherCatchUp";
    private static final int CATCH_UP_START_HOUR = 7;
    private static final int CATCH_UP_END_HOUR = 22;

    private DailySkinWeatherCatchUp() {
    }

    public static void tryDeliverMissedToday(Context context) {
        if (context == null) {
            return;
        }
        Context app = context.getApplicationContext();
        if (!ProfileSession.isSkinWeatherNotiEnabled(app)
                || !ProfileSession.isNotiEnabled(app)
                || !LocalSystemNotificationHelper.canPost(app)) {
            return;
        }

        String stableId = LocalSystemNotificationHelper.dailyStableId("weather_daily");
        if (NotificationRepository.getInstance(app).hasNotificationId(stableId)) {
            return;
        }

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour < CATCH_UP_START_HOUR || hour >= CATCH_UP_END_HOUR) {
            return;
        }

        Log.i(TAG, "Delivering catch-up weather notification for today");
        DailySkinWeatherScheduler.triggerNow(app, false);
    }
}
