package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_com_friend")
public class UserComFriendEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
