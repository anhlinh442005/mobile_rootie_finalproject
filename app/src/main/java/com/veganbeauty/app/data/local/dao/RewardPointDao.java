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

    @Query("SELECT SUM(points) FROM user_coin")
    Flow<Integer> getTotalPointsFlow();

    @Query("SELECT * FROM user_coin ORDER BY timestamp DESC")
    Flow<List<RewardPointEntity>> getAllRewardHistory();

    @Query("SELECT * FROM user_coin ORDER BY timestamp DESC")
    List<RewardPointEntity> getAllRewardHistoryList();

    @Query("SELECT EXISTS(SELECT 1 FROM user_coin WHERE orderId = :orderId AND reason LIKE '%Đánh giá%')")
    boolean hasReceivedPointsForOrder(String orderId);
}
