package com.veganbeauty.app.data.local.entities;

public class BookingEntity {
    private String id;
    private String userId;
    private String serviceName;
    private String status;
    private String type;
    private long timestamp;
    private String doctorName;
    private String contactPhone;
    private String note;
    private long createdAt;
    private String location;
    private String meetLink;

    public BookingEntity(String id, String userId, String serviceName, String status, String type, long timestamp, String doctorName, String contactPhone, String note, long createdAt, String location, String meetLink) {
        this.id = id;
        this.userId = userId;
        this.serviceName = serviceName;
        this.status = status;
        this.type = type;
        this.timestamp = timestamp;
        this.doctorName = doctorName;
        this.contactPhone = contactPhone;
        this.note = note;
        this.createdAt = createdAt;
        this.location = location;
        this.meetLink = meetLink;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getServiceName() { return serviceName; }
    public String getStatus() { return status; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public String getDoctorName() { return doctorName; }
    public String getContactPhone() { return contactPhone; }
    public String getNote() { return note; }
    public long getCreatedAt() { return createdAt; }
    public String getLocation() { return location; }
    public String getMeetLink() { return meetLink; }
}
