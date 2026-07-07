package com.veganbeauty.app.features.routine;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.weather.DailySkinWeatherScheduler;

public class RoutineAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "RoutineAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("REMINDER_TYPE");
        if (type == null) type = "MORNING";
        
        boolean isLead = intent.getBooleanExtra("IS_LEAD_REMINDER", false);

        Log.d(TAG, "onReceive triggered. Type: " + type + ", isLead: " + isLead);

        boolean notiAllowed = ProfileSession.isNotiEnabled(context);
        boolean routineEnabled = type.equals("MORNING") ? 
                ProfileSession.isMorningReminderEnabled(context) : 
                ProfileSession.isEveningReminderEnabled(context);

        if (!notiAllowed || !routineEnabled || !com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.canPost(context)) {
            Log.d(TAG, "Notifications are disabled or cannot be posted. Rescheduling and returning.");
            RoutineAlarmScheduler.rescheduleAlarms(context);
            return;
        }

        String titleText;
        String messageText;

        if (type.equals("MORNING")) {
            titleText = isLead ? "Routine buổi sáng sắp đến" : "Đến giờ chăm da buổi sáng rồi✨";
            messageText = isLead ? "Routine chăm da buổi sáng sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!" : 
                                   "Mở Rootie và bắt đầu quy trình giúp làn da của bạn luôn rạng rỡ hôm nay.";
        } else {
            titleText = isLead ? "Routine buổi tối sắp đến" : "Đến giờ chăm da buổi tối rồi✨";
            messageText = isLead ? "Routine chăm da buổi tối sẽ bắt đầu sau 15 phút. Đừng quên chuẩn bị nhé!" : 
                                   "Mở Rootie và bắt đầu quy trình giúp làn da của bạn phục hồi hôm nay.";
        }

        String stablePrefix = type.equals("MORNING")
                ? (isLead ? "routine_morning_lead" : "routine_morning")
                : (isLead ? "routine_evening_lead" : "routine_evening");

        try {
            com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.dispatch(
                    context,
                    com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.dailyStableId(stablePrefix),
                    titleText,
                    messageText,
                    "Khác",
                    "skin care",
                    "SKIN_REMINDER",
                    "CHĂM DA NGAY"
            );
            Log.d(TAG, "Routine notification posted successfully: " + stablePrefix);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show routine notification", e);
        } finally {
            RoutineAlarmScheduler.rescheduleAlarms(context);
        }
    }
}
