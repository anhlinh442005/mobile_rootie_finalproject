package com.veganbeauty.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.veganbeauty.app.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUserSync(user: UserEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>): List<Long>

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun getUserByEmailAndPassword(email: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone AND password = :password LIMIT 1")
    suspend fun getUserByPhoneAndPassword(phone: String, password: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("DELETE FROM users WHERE username = :username")
    fun deleteUserByUsernameSync(username: String)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmailSync(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    fun getUserByPhoneSync(phone: String): UserEntity?

    @Query("UPDATE users SET password = :newPassword WHERE email = :email")
    fun updatePasswordByEmailSync(email: String, newPassword: String)

    @Query("UPDATE users SET password = :newPassword WHERE phone = :phone")
    fun updatePasswordByPhoneSync(phone: String, newPassword: String)
}
