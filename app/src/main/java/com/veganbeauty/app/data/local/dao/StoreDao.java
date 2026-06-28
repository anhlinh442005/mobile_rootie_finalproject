package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.StoreEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface StoreDao {
    @Query("SELECT * FROM stores")
    Flow<List<StoreEntity>> getAllStores();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertStores(List<StoreEntity> stores);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM stores WHERE id = :storeId LIMIT 1")
    StoreEntity getStoreById(String storeId);
}
