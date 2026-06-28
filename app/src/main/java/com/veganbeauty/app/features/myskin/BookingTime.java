package com.veganbeauty.app.features.myskin;

public class BookingTime {
    private String id;
    private String time;
    private boolean locked;

    public BookingTime(String id, String time, boolean locked) {
        this.id = id;
        this.time = time;
        this.locked = locked;
    }

    public String getId() { return id; }
    public String getTime() { return time; }
    public boolean isLocked() { return locked; }
}
