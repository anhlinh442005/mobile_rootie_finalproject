package com.veganbeauty.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeFormatter {
    public static long parseCreatedAtMillis(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return 0L;
        try {
            long asLong = Long.parseLong(dateString.trim());
            return (asLong < 10000000000L) ? asLong * 1000 : asLong;
        } catch (NumberFormatException ignored) {
            String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
            };
            for (String f : formats) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date parsedDate = sdf.parse(dateString);
                    if (parsedDate != null) return parsedDate.getTime();
                } catch (Exception e) {
                    // try next format
                }
            }
        }
        return 0L;
    }

    public static int compareCreatedAtDesc(String first, String second) {
        return Long.compare(parseCreatedAtMillis(second), parseCreatedAtMillis(first));
    }

    public static String getTimeAgo(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return "";
        try {
            long pastTimeMs = parseCreatedAtMillis(dateString);
            if (pastTimeMs <= 0L) return dateString;
            
            long now = new Date().getTime();
            long seconds = Math.abs(now - pastTimeMs) / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            
            if (seconds < 60) return "Vừa xong";
            if (minutes < 60) return minutes + " phút trước";
            if (hours < 24) return hours + " giờ trước";
            if (days < 7) return days + " ngày trước";
            if (days < 30) return (days / 7) + " tuần trước";
            if (days < 365) return (days / 30) + " tháng trước";
            return (days / 365) + " năm trước";
        } catch (Exception e) {
            e.printStackTrace();
            return dateString;
        }
    }
}
