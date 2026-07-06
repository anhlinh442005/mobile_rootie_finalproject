package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_thanhphan")
public class QuizThanhphanEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
