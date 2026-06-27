package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_coin")
public class RewardPointEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    private String orderId;
    private int points;
    @NonNull
    private String reason;
    private long timestamp;

    public RewardPointEntity(int id, @NonNull String orderId, int points, @NonNull String reason, long timestamp) {
        this.id = id;
        this.orderId = orderId;
        this.points = points;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @NonNull
    public String getOrderId() { return orderId; }
    public void setOrderId(@NonNull String orderId) { this.orderId = orderId; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    @NonNull
    public String getReason() { return reason; }
    public void setReason(@NonNull String reason) { this.reason = reason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
