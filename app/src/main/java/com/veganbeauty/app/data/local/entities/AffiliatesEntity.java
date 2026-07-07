package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "affiliates")
public class AffiliatesEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
