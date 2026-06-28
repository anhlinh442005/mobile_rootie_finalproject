package com.veganbeauty.app.utils;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public final class MessageTimeFormatter {

    private static final Pattern TIME_ONLY = Pattern.compile("^\\d{1,2}:\\d{2}$");

    private MessageTimeFormatter() {
    }

    public static String formatConversationTime(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "";
        }
        String value = raw.trim();
        if ("Hôm qua".equalsIgnoreCase(value)) {
            return "Hôm qua";
        }
        if (TIME_ONLY.matcher(value).matches()) {
            return value.length() == 4 ? "0" + value : value;
        }

        Date parsed = parseDate(value);
        if (parsed == null) {
            return value.length() > 12 ? value.substring(0, 12) : value;
        }

        Calendar messageCal = Calendar.getInstance();
        messageCal.setTime(parsed);
        Calendar today = Calendar.getInstance();

        if (isSameDay(messageCal, today)) {
            return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(parsed);
        }

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(messageCal, yesterday)) {
            return "Hôm qua";
        }

        int diffDays = dayDiff(messageCal, today);
        if (diffDays > 0 && diffDays < 7) {
            return "T" + dayOfWeekShort(messageCal);
        }

        return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(parsed);
    }

    @Nullable
    private static Date parseDate(String value) {
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                if (pattern.contains("'Z'") || pattern.contains("X")) {
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                return format.parse(value);
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static int dayDiff(Calendar earlier, Calendar later) {
        Calendar start = (Calendar) later.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) earlier.clone();
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        long diffMs = start.getTimeInMillis() - end.getTimeInMillis();
        return (int) (diffMs / (24L * 60L * 60L * 1000L));
    }

    private static int dayOfWeekShort(Calendar calendar) {
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.MONDAY:
                return 2;
            case Calendar.TUESDAY:
                return 3;
            case Calendar.WEDNESDAY:
                return 4;
            case Calendar.THURSDAY:
                return 5;
            case Calendar.FRIDAY:
                return 6;
            case Calendar.SATURDAY:
                return 7;
            case Calendar.SUNDAY:
            default:
                return 1;
        }
    }
}
