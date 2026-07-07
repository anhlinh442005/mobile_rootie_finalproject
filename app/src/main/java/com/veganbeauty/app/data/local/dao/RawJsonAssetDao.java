package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.RawJsonAssetEntity;

import java.util.List;

@Dao
public interface RawJsonAssetDao {
    @Query("SELECT * FROM raw_json_assets")
    List<RawJsonAssetEntity> getAllAssets();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAsset(RawJsonAssetEntity asset);
}
