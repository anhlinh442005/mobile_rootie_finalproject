package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.*
import com.veganbeauty.app.data.local.entities.*

@Database(entities = [UserEntity::class, CommunityPostEntity::class, ReelEntity::class, OrderEntity::class, ProductEntity::class, YtVideoEntity::class, UserMemoryEntity::class, IngredientEntity::class, CommunityBlogEntity::class], version = 15)
@TypeConverters(OrderConverters::class)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
}
