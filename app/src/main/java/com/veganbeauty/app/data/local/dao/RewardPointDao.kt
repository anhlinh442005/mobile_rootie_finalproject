package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardPoints(reward: RewardPointEntity): Long

    @Query("SELECT SUM(points) FROM user_coin")
    fun getTotalPointsFlow(): Flow<Int?>

    @Query("SELECT * FROM user_coin ORDER BY timestamp DESC")
    fun getAllRewardHistory(): Flow<List<RewardPointEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_coin WHERE orderId = :orderId AND reason LIKE '%Đánh giá%')")
    suspend fun hasReceivedPointsForOrder(orderId: String): Boolean
}
