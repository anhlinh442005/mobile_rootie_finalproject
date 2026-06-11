package com.veganbeauty.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veganbeauty.app.data.local.dao.*
import com.veganbeauty.app.data.local.entities.*
import kotlinx.coroutines.launch

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
    version = 20
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

        @JvmStatic
        fun getDatabase(context: android.content.Context): RootieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    RootieDatabase::class.java,
                    "rootie-db"
                )
                .addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        try {
                            val jsonString = context.assets.open("users.json").bufferedReader().use { it.readText() }
                            val jsonArray = org.json.JSONArray(jsonString)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val values = android.content.ContentValues().apply {
                                    put("user_id", obj.optString("user_id", java.util.UUID.randomUUID().toString()))
                                    put("username", obj.optString("username", ""))
                                    put("full_name", obj.optString("full_name", ""))
                                    put("email", obj.optString("email", ""))
                                    put("phone", obj.optString("phone", ""))
                                    put("password", obj.optString("password", ""))
                                    put("avatar", obj.optString("avatar", ""))
                                }
                                db.insert("users", android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE, values)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
