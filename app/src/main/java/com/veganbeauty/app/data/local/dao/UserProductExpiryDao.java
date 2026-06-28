package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

@Dao
public interface UserProductExpiryDao {
    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId")
    Flow<List<UserProductExpiryEntity>> getProductsByUserIdFlow(String userId);

    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId")
    List<UserProductExpiryEntity> getProductsByUserId(String userId);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM user_product_expiry WHERE userId = :userId AND productId = :productId LIMIT 1")
    UserProductExpiryEntity getProductById(String userId, String productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertUserProducts(List<UserProductExpiryEntity> products);

    @Query("SELECT COUNT(*) FROM user_product_expiry WHERE userId = :userId")
    int getProductCountByUserId(String userId);

    @Query("DELETE FROM user_product_expiry WHERE userId = :userId AND productId = :productId")
    int deleteUserProductExpiry(String userId, String productId);
}
