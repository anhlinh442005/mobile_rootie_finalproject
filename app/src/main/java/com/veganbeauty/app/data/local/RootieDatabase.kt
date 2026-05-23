package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.OrderEntity

@Database(entities = [ProductEntity::class, OrderEntity::class], version = 4)
@TypeConverters(OrderConverters::class)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
}
