package com.veganbeauty.app.features.myskin;

import kotlin.Pair;

public class BookingDateParser {
    public static Pair<String, String> parseDateDisplay(String dateDisplay, String monthDisplay, String dayOfWeek) {
        String dayNum = "01";
        String monthDayStr = (dayOfWeek != null ? dayOfWeek : "") + ", " + (monthDisplay != null ? monthDisplay : "");

        try {
            if (dateDisplay == null) {
                return new Pair<>(dayNum, monthDayStr);
            }
            String clean = dateDisplay.trim();

            if (clean.contains("/")) {
                String[] parts = clean.split("/");
                if (parts.length > 0) {
                    dayNum = parts[0];
                    int monthNum = 0;
                    if (parts.length > 1) {
                        try { monthNum = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {}
                    }
                    String monthStr = monthNum > 0 ? "Tháng " + monthNum : "Tháng --";
                    String dow = dayOfWeek != null ? dayOfWeek : "";
                    if (dow.equalsIgnoreCase("Chủ Nhật") || dow.equalsIgnoreCase("CN") || dow.equalsIgnoreCase("Chủ nhật")) dow = "CN";
                    else if (dow.startsWith("Thứ ")) dow = "T." + dow.substring(4);
                    monthDayStr = monthStr + "\n" + dow;
                    return new Pair<>(dayNum, monthDayStr);
                }
            }

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)\\s+tháng\\s+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(clean);
            if (matcher.find()) {
                dayNum = matcher.group(1);
                String month = matcher.group(2);
                String dow = dayOfWeek != null ? dayOfWeek : "";
                if (dow.equalsIgnoreCase("Chủ Nhật") || dow.equalsIgnoreCase("CN") || dow.equalsIgnoreCase("Chủ nhật")) dow = "CN";
                else if (dow.startsWith("Thứ ")) dow = "T." + dow.substring(4);
                monthDayStr = "Tháng " + month + "\n" + dow;
                return new Pair<>(dayNum, monthDayStr);
            }

            if (clean.contains(" ")) {
                dayNum = clean.split(" ")[0];
            } else {
                dayNum = clean;
            }
        } catch (Exception e) {
            // Keep safe fallback values.
        }

        return new Pair<>(dayNum, monthDayStr);
    }
}
