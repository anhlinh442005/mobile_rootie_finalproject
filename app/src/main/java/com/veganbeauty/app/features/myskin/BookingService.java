package com.veganbeauty.app.features.myskin;

public class BookingService {
    private String id;
    private String name;
    private String priceInfo;
    private String duration;

    public BookingService(String id, String name, String priceInfo, String duration) {
        this.id = id;
        this.name = name;
        this.priceInfo = priceInfo;
        this.duration = duration;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPriceInfo() { return priceInfo; }
    public String getDuration() { return duration; }
}
