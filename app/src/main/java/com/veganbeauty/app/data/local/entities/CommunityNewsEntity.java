package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "community_news")
public class CommunityNewsEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
