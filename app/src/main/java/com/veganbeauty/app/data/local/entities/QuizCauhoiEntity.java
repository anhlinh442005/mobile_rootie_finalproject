package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_cauhoi")
public class QuizCauhoiEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
