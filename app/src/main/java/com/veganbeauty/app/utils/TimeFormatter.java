package com.veganbeauty.app.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeFormatter {
    public static String getTimeAgo(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return "";
        try {
            long pastTimeMs = 0;
            try {
                long asLong = Long.parseLong(dateString);
                pastTimeMs = (asLong < 10000000000L) ? asLong * 1000 : asLong;
            } catch (NumberFormatException nfe) {
                String[] formats = {
                    "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'"
                };
                Date parsedDate = null;
                for (String f : formats) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        parsedDate = sdf.parse(dateString);
                        if (parsedDate != null) break;
                    } catch (Exception e) {
                        // Continue to next format
                    }
                }
                if (parsedDate == null) return dateString;
                pastTimeMs = parsedDate.getTime();
            }
            
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
