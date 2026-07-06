package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "skin_history")
public class SkinHistoryEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String userId;

    @NonNull
    private String email;

    @NonNull
    private String payload;

    @NonNull
    private String date;

    @NonNull
    private String time;

    @NonNull
    private String scanType;

    public SkinHistoryEntity(
            @NonNull String id,
            @NonNull String userId,
            @NonNull String email,
            @NonNull String payload,
            @NonNull String date,
            @NonNull String time,
            @NonNull String scanType
    ) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.payload = payload;
        this.date = date;
        this.time = time;
        this.scanType = scanType;
    }

    @NonNull public String getId() { return id; }
    @NonNull public String getUserId() { return userId; }
    @NonNull public String getEmail() { return email; }
    @NonNull public String getPayload() { return payload; }
    @NonNull public String getDate() { return date; }
    @NonNull public String getTime() { return time; }
    @NonNull public String getScanType() { return scanType; }
}
