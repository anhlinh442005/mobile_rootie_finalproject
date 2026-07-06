package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notification_com")
public class NotificationComEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
