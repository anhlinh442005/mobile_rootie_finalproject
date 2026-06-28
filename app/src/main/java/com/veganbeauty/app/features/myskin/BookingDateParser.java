package com.veganbeauty.app.features.myskin;

import kotlin.Pair;

public class BookingDateParser {
    public static Pair<String, String> parseDateDisplay(String dateDisplay, String monthDisplay, String dayOfWeek) {
        String dayNum = "";
        String monthDayStr = "";
        
        try {
            if (dateDisplay != null && dateDisplay.contains(" ")) {
                dayNum = dateDisplay.split(" ")[0];
            }
            monthDayStr = (dayOfWeek != null ? dayOfWeek : "") + ", " + (monthDisplay != null ? monthDisplay : "");
        } catch (Exception e) {
            dayNum = "01";
            monthDayStr = "Thứ Hai, Tháng 1";
        }
        
        return new Pair<>(dayNum, monthDayStr);
    }
}
