package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.veganbeauty.app.data.local.entities.UserGiftEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface UserGiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUserGift(UserGiftEntity gift);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertUserGifts(List<UserGiftEntity> gifts);

    @Query("SELECT * FROM user_gifts WHERE userId = :userId ORDER BY acquiredTimestamp DESC")
    Flow<List<UserGiftEntity>> getAllUserGiftsFlow(String userId);

    @Query("SELECT COUNT(*) FROM user_gifts WHERE userId = :userId")
    int getUserGiftCount(String userId);

    @Query("SELECT * FROM user_gifts WHERE userId = :userId ORDER BY acquiredTimestamp DESC")
    List<UserGiftEntity> getAllUserGiftsSync(String userId);

    @Query("DELETE FROM user_gifts WHERE id = :id")
    int deleteUserGiftById(int id);

    @Update
    int updateUserGift(UserGiftEntity gift);

    @Query("DELETE FROM user_gifts WHERE userId = :userId")
    void deleteByUserId(String userId);

    @Query("DELETE FROM user_gifts")
    void clearAllSync();
}
