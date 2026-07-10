package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.RewardPointEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface RewardPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRewardPoints(RewardPointEntity reward);

    public static class TotalPoints {
        public int total;
    }

    @Query("SELECT COALESCE(SUM(points), 0) as total FROM user_coin WHERE userId = :userId")
    Flow<List<TotalPoints>> getTotalPointsFlow(String userId);

    @Query("SELECT COALESCE(SUM(points), 0) FROM user_coin WHERE userId = :userId")
    int getTotalPointsSync(String userId);

    @Query("SELECT * FROM user_coin WHERE userId = :userId ORDER BY timestamp DESC")
    Flow<List<RewardPointEntity>> getAllRewardHistory(String userId);

    @Query("SELECT * FROM user_coin WHERE userId = :userId ORDER BY timestamp DESC")
    List<RewardPointEntity> getAllRewardHistoryList(String userId);

    @Query("SELECT COALESCE(SUM(points), 0) FROM user_coin WHERE userId = :userId AND timestamp >= :sinceMs AND points > 0")
    int getPointsEarnedSince(String userId, long sinceMs);

    @Query("SELECT * FROM user_coin WHERE userId = :userId AND timestamp >= :sinceMs ORDER BY timestamp DESC")
    List<RewardPointEntity> getHistorySince(String userId, long sinceMs);

    @Query("SELECT EXISTS(SELECT 1 FROM user_coin WHERE userId = :userId AND orderId = :orderId AND reason LIKE '%Đánh giá%')")
    boolean hasReceivedPointsForOrder(String userId, String orderId);

    @Query("DELETE FROM user_coin WHERE userId = :userId AND orderId = :orderId AND timestamp >= :sinceMs")
    int deleteRewardsByOrderIdSince(String userId, String orderId, long sinceMs);

    @Query("DELETE FROM user_coin WHERE userId = :userId")
    void deleteByUserId(String userId);

    @Query("DELETE FROM user_coin")
    void clearAllSync();
}
