package com.veganbeauty.app.features.account.notification;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationDateHelper {

    public static String getSectionFromTime(String timeStr) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = dateTimeFormat.parse(timeStr);
            if (date == null) return "Cũ hơn";
            Date now = new Date();

            Calendar calNow = Calendar.getInstance();
            calNow.setTime(now);
            Calendar calThen = Calendar.getInstance();
            calThen.setTime(date);

            int yearNow = calNow.get(Calendar.YEAR);
            int dayNow = calNow.get(Calendar.DAY_OF_YEAR);

            int yearThen = calThen.get(Calendar.YEAR);
            int dayThen = calThen.get(Calendar.DAY_OF_YEAR);

            if (yearNow == yearThen && dayNow == dayThen) {
                return "Hôm nay";
            } else {
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.setTime(now);
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);
                
                int yearYest = calYesterday.get(Calendar.YEAR);
                int dayYest = calYesterday.get(Calendar.DAY_OF_YEAR);

                if (yearThen == yearYest && dayThen == dayYest) {
                    return "Hôm qua";
                } else {
                    return "Cũ hơn";
                }
            }
        } catch (Exception e) {
            return "Cũ hơn";
        }
    }

    public static String getDisplayTime(String timeStr) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = dateTimeFormat.parse(timeStr);
            if (date == null) return timeStr;
            Date now = new Date();

            Calendar calNow = Calendar.getInstance();
            calNow.setTime(now);
            Calendar calThen = Calendar.getInstance();
            calThen.setTime(date);

            int yearNow = calNow.get(Calendar.YEAR);
            int dayNow = calNow.get(Calendar.DAY_OF_YEAR);

            int yearThen = calThen.get(Calendar.YEAR);
            int dayThen = calThen.get(Calendar.DAY_OF_YEAR);

            SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timePart = timeFormatter.format(date);

            if (yearNow == yearThen && dayNow == dayThen) {
                return timePart;
            } else {
                Calendar calYesterday = Calendar.getInstance();
                calYesterday.setTime(now);
                calYesterday.add(Calendar.DAY_OF_YEAR, -1);
                
                if (yearThen == calYesterday.get(Calendar.YEAR) && dayThen == calYesterday.get(Calendar.DAY_OF_YEAR)) {
                    return "Hôm qua, " + timePart;
                } else {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    return dateFormatter.format(date);
                }
            }
        } catch (Exception e) {
            return timeStr;
        }
    }
}
