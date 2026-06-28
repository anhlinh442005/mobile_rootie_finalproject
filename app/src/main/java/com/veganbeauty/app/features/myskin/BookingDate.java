package com.veganbeauty.app.features.myskin;

import java.util.Calendar;

public class BookingDate {
    private String id;
    private String dayOfWeek;
    private String date;
    private Calendar fullDate;

    public BookingDate(String id, String dayOfWeek, String date, Calendar fullDate) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.date = date;
        this.fullDate = fullDate;
    }

    public String getId() { return id; }
    public String getDayOfWeek() { return dayOfWeek; }
    public String getDate() { return date; }
    public Calendar getFullDate() { return fullDate; }
}
