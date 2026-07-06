package com.veganbeauty.app.data.local.dao;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.SkinHistoryEntity;

import java.util.List;

@Dao
public interface SkinHistoryDao {

    @Query("SELECT * FROM skin_history WHERE userId = :userId OR email = :email ORDER BY date DESC, time DESC")
    List<SkinHistoryEntity> getByUser(String userId, String email);

    @Nullable
    @Query("SELECT * FROM skin_history WHERE id = :id LIMIT 1")
    SkinHistoryEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SkinHistoryEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SkinHistoryEntity> entities);

    @Query("DELETE FROM skin_history WHERE id = :id")
    void deleteById(String id);
}
