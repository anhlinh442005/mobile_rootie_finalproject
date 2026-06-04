package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.UserGiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserGiftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGift(gift: UserGiftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGifts(gifts: List<UserGiftEntity>): List<Long>

    @Query("SELECT * FROM user_gifts ORDER BY acquiredTimestamp DESC")
    fun getAllUserGiftsFlow(): Flow<List<UserGiftEntity>>

    @Query("SELECT COUNT(*) FROM user_gifts")
    suspend fun getUserGiftCount(): Int

    @Query("DELETE FROM user_gifts WHERE id = :id")
    suspend fun deleteUserGiftById(id: Int): Int

    @Update
    suspend fun updateUserGift(gift: UserGiftEntity): Int
}
