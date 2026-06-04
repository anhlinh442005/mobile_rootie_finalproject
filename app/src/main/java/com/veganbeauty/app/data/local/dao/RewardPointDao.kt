package com.veganbeauty.app.data.local.dao

import androidx.room.*
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRewardPoints(reward: RewardPointEntity): Long

    @Query("SELECT SUM(points) FROM reward_points")
    fun getTotalPointsFlow(): Flow<Int?>

    @Query("SELECT * FROM reward_points ORDER BY timestamp DESC")
    fun getAllRewardHistory(): Flow<List<RewardPointEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM reward_points WHERE orderId = :orderId AND reason LIKE '%Đánh giá%')")
    suspend fun hasReceivedPointsForOrder(orderId: String): Boolean
}
