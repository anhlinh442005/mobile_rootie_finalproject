package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.UserDao
import com.veganbeauty.app.data.local.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuthRepository(private val userDao: UserDao) {

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun login(emailOrPhone: String, password: String): UserEntity? {
        val hashedPassword = hashPassword(password)
        val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()
        return if (isEmail) {
            userDao.getUserByEmailAndPassword(emailOrPhone, hashedPassword)
        } else {
            userDao.getUserByPhoneAndPassword(emailOrPhone, hashedPassword)
        }
    }

    suspend fun register(fullName: String, emailOrPhone: String, password: String): Result<UserEntity> {
        val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(emailOrPhone).matches()
        
        // Check if user exists
        val existingUser = if (isEmail) {
            userDao.getUserByEmail(emailOrPhone)
        } else {
            userDao.getUserByPhone(emailOrPhone)
        }

        if (existingUser != null) {
            return Result.failure(Exception("Tài khoản đã tồn tại."))
        }

        val userId = if (emailOrPhone == "test@example.com") "test_001" else java.util.UUID.randomUUID().toString()
        val email = if (isEmail) emailOrPhone else ""
        val phone = if (!isEmail) emailOrPhone else ""
        val hashedPassword = hashPassword(password)

        val newUser = UserEntity(
            user_id = userId,
            username = fullName,
            full_name = fullName,
            email = email,
            phone = phone,
            password = hashedPassword
        )

        userDao.insertUser(newUser)
        
        // Sync to Firebase in the background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                com.veganbeauty.app.data.remote.FirestoreService().saveUser(newUser)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return Result.success(newUser)
    }
}
