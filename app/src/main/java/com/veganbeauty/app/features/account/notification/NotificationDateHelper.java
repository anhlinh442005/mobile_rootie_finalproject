package com.veganbeauty.app.features.account.notification;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class NotificationDateHelper {

    private static final String[] PARSE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm"
    };

    @Nullable
    public static Date parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }
        String value = timeStr.trim();
        for (String pattern : PARSE_PATTERNS) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.getDefault());
                format.setLenient(false);
                Date parsed = format.parse(value);
                if (parsed != null) {
                    return parsed;
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    public static String getSectionFromTime(String timeStr) {
        Date date = parseTime(timeStr);
        if (date == null) {
            return "Cũ hơn";
        }

        Calendar calNow = Calendar.getInstance();
        Calendar calThen = Calendar.getInstance();
        calThen.setTime(date);

        if (isSameDay(calNow, calThen)) {
            return "Hôm nay";
        }

        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(calYesterday, calThen)) {
            return "Hôm qua";
        }

        return "Cũ hơn";
    }

    public static String getDisplayTime(String timeStr) {
        Date date = parseTime(timeStr);
        if (date == null) {
            return timeStr != null ? timeStr : "";
        }

        Calendar calNow = Calendar.getInstance();
        Calendar calThen = Calendar.getInstance();
        calThen.setTime(date);

        SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timePart = timeFormatter.format(date);

        if (isSameDay(calNow, calThen)) {
            return timePart;
        }

        Calendar calYesterday = Calendar.getInstance();
        calYesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(calYesterday, calThen)) {
            return "Hôm qua, " + timePart;
        }

        SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return dateFormatter.format(date);
    }

    public static long getTimeMillis(String timeStr) {
        Date date = parseTime(timeStr);
        return date != null ? date.getTime() : 0L;
    }

    @Nullable
    public static String toStorageTimeFromOrder(@Nullable String orderDate, @Nullable String orderTime) {
        if (orderDate == null || orderDate.trim().isEmpty()) {
            return null;
        }
        try {
            String datePart = orderDate.trim();
            String timePart = orderTime != null ? orderTime.trim() : "";
            SimpleDateFormat inputFormat;
            String raw;
            if (!timePart.isEmpty()) {
                inputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                raw = datePart + " " + timePart;
            } else {
                inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                raw = datePart;
            }
            Date parsed = inputFormat.parse(raw);
            if (parsed == null) {
                return null;
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(parsed);
        } catch (ParseException ignored) {
            return null;
        }
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
