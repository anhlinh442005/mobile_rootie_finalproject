package com.veganbeauty.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.veganbeauty.app.data.local.entities.UserEntity;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM users")
    Flow<List<UserEntity>> getAllUsers();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUserSync(UserEntity user);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    UserEntity getUserByIdSync(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertUsersSync(List<UserEntity> users);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    UserEntity getUserByEmailAndPasswordSync(String email, String password);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM users WHERE phone = :phone AND password = :password LIMIT 1")
    UserEntity getUserByPhoneAndPasswordSync(String phone, String password);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    UserEntity getUserByEmailSync(String email);

    @androidx.annotation.Nullable
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    UserEntity getUserByPhoneSync(String phone);

    @Query("DELETE FROM users WHERE username = :username")
    int deleteUserByUsernameSync(String username);

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    int updatePasswordByEmailSync(String email, String newPassword);

    @Query("UPDATE users SET password = :newPassword WHERE phone = :phone")
    int updatePasswordByPhoneSync(String phone, String newPassword);
}
