package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "affiliate_product")
public class AffiliateProductEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
