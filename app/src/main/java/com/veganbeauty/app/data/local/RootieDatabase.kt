package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.dao.RewardPointDao
import com.veganbeauty.app.data.local.dao.UserGiftDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.data.local.entities.UserGiftEntity

@Database(entities = [ProductEntity::class, OrderEntity::class, RewardPointEntity::class, UserGiftEntity::class], version = 9)
@TypeConverters(OrderConverters::class)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun rewardPointDao(): RewardPointDao
    abstract fun userGiftDao(): UserGiftDao
}
