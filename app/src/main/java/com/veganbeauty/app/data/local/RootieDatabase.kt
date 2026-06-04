package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.ProductDao
import com.veganbeauty.app.data.local.dao.OrderDao
import com.veganbeauty.app.data.local.dao.RewardPointDao
import com.veganbeauty.app.data.local.dao.UserGiftDao
import com.veganbeauty.app.data.local.dao.CartDao
import com.veganbeauty.app.data.local.dao.UserDao
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.RewardPointEntity
import com.veganbeauty.app.data.local.entities.UserGiftEntity
import com.veganbeauty.app.data.local.entities.CartItemEntity
import com.veganbeauty.app.data.local.entities.UserEntity

@Database(
    entities = [
        ProductEntity::class,
        OrderEntity::class,
        RewardPointEntity::class,
        UserGiftEntity::class,
        CartItemEntity::class
    ],
    version = 10
)
@TypeConverters(OrderConverters::class, ProductConverters::class)
@Database(entities = [ProductEntity::class, UserEntity::class], version = 2)
abstract class RootieDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun rewardPointDao(): RewardPointDao
    abstract fun userGiftDao(): UserGiftDao
    abstract fun cartDao(): CartDao

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
    abstract fun userDao(): UserDao
}
