package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.*
import com.veganbeauty.app.data.local.entities.*

@Database(
    entities = [
        UserEntity::class, 
        CommunityPostEntity::class, 
        ReelEntity::class, 
        OrderEntity::class, 
        ProductEntity::class, 
        YtVideoEntity::class, 
        UserMemoryEntity::class, 
        IngredientEntity::class, 
        CommunityBlogEntity::class,
        RewardPointEntity::class,
        UserGiftEntity::class,
        CartItemEntity::class
    ], 
    version = 16
)
@TypeConverters(OrderConverters::class, ProductConverters::class)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun communityDao(): CommunityDao
    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
    abstract fun rewardPointDao(): RewardPointDao
    abstract fun userGiftDao(): UserGiftDao
    abstract fun cartDao(): CartDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: RootieDatabase? = null

        fun getDatabase(context: android.content.Context): RootieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RootieDatabase::class.java,
                    "rootie-db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
