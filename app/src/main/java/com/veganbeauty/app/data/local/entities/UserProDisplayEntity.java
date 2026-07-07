package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_pro_display")
public class UserProDisplayEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
