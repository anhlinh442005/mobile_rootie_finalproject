package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.dao.UserDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.UserEntity

@Database(entities = [ProductEntity::class, UserEntity::class], version = 2)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun userDao(): UserDao
}
