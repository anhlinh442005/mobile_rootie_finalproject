package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.entities.ProductEntity

@Database(entities = [ProductEntity::class], version = 1)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
