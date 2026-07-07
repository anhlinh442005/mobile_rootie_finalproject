package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_weather")
public class ProductWeatherEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
