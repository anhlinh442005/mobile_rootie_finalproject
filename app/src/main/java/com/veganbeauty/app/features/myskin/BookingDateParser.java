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
                if (parts.length >= 2) {
                    dayNum = parts[0];
                    String month = parts[1];
                    String dow = dayOfWeek != null ? dayOfWeek : "";
                    monthDayStr = "Tháng " + month + "\n" + dow;
                    return new Pair<>(dayNum, monthDayStr);
                }
            }

            if (clean.contains(" ")) {
                dayNum = clean.split(" ")[0];
            }
        } catch (Exception e) {
            // Keep safe fallback values.
        }

        return new Pair<>(dayNum, monthDayStr);
    }
}
