package com.veganbeauty.app.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "raw_json_assets")
public class RawJsonAssetEntity {
    @PrimaryKey
    @NonNull
    private String fileName;
    
    @NonNull
    private String jsonContent;
    
    private long lastUpdated;

    public RawJsonAssetEntity(@NonNull String fileName, @NonNull String jsonContent, long lastUpdated) {
        this.fileName = fileName;
        this.jsonContent = jsonContent;
        this.lastUpdated = lastUpdated;
    }

    @NonNull
    public String getFileName() {
        return fileName;
    }

    public void setFileName(@NonNull String fileName) {
        this.fileName = fileName;
    }

    @NonNull
    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(@NonNull String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
