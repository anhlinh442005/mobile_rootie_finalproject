package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoriesEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
