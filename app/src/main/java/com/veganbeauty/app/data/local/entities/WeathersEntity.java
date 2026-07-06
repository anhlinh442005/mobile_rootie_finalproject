package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weathers")
public class WeathersEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
