package com.veganbeauty.app.data.local.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quiz_ketqua")
public class QuizKetquaEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String rawData;
}
