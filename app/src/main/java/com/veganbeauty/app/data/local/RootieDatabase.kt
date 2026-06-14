package com.veganbeauty.app.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        CartItemEntity::class,
        StoreEntity::class,
        UserProductExpiryEntity::class
    ],
    version = 31
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
    abstract fun storeDao(): StoreDao
    abstract fun userProductExpiryDao(): UserProductExpiryDao

    companion object {
        private const val TAG = "RootieDatabase"

        /**
         * v23 -> v24: add guest-checkout support to the `orders` table.
         *  - `userId` becomes a nullable column (NULL means guest checkout).
         *  - `isGuest` boolean flag (0/1) for fast filtering.
         *  - `billingName`, `billingPhone`, `billingEmail` snapshot columns for
         *    preserving the buyer info that was typed at checkout, even if the
         *    user later edits or deletes their profile.
         */
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite cannot add a NOT NULL column without a default. The schema
                // declares `userId` as nullable, so we re-create the table with the
                // updated column set. This is acceptable because order data is
                // re-seeded from the local JSON on the next app launch anyway.
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orders_new` (" +
                        "`orderId` TEXT NOT NULL, " +
                        "`orderDate` TEXT NOT NULL, " +
                        "`orderTime` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`totalAmount` INTEGER NOT NULL, " +
                        "`items` TEXT NOT NULL, " +
                        "`userId` TEXT, " +
                        "`isGuest` INTEGER NOT NULL DEFAULT 0, " +
                        "`shippingName` TEXT NOT NULL, " +
                        "`shippingPhone` TEXT NOT NULL, " +
                        "`shippingAddress` TEXT NOT NULL, " +
                        "`shippingCost` INTEGER NOT NULL, " +
                        "`voucherDiscount` INTEGER NOT NULL, " +
                        "`paymentMethod` TEXT NOT NULL, " +
                        "`expectedDeliveryTime` TEXT, " +
                        "`hasReview` INTEGER NOT NULL, " +
                        "`reviewStars` INTEGER NOT NULL, " +
                        "`reviewText` TEXT, " +
                        "`reviewImage` TEXT, " +
                        "`isAnonymous` INTEGER NOT NULL, " +
                        "`recommendToFriends` INTEGER NOT NULL, " +
                        "`billingName` TEXT, " +
                        "`billingPhone` TEXT, " +
                        "`billingEmail` TEXT, " +
                        "PRIMARY KEY(`orderId`))"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO orders_new (" +
                        "orderId, orderDate, orderTime, status, totalAmount, items, " +
                        "userId, isGuest, " +
                        "shippingName, shippingPhone, shippingAddress, " +
                        "shippingCost, voucherDiscount, paymentMethod, expectedDeliveryTime, " +
                        "hasReview, reviewStars, reviewText, reviewImage, isAnonymous, " +
                        "recommendToFriends, billingName, billingPhone, billingEmail) " +
                        "SELECT " +
                        "orderId, orderDate, orderTime, status, totalAmount, items, " +
                        "NULL, 0, " +
                        "shippingName, shippingPhone, shippingAddress, " +
                        "shippingCost, voucherDiscount, paymentMethod, expectedDeliveryTime, " +
                        "hasReview, reviewStars, reviewText, reviewImage, isAnonymous, " +
                        "recommendToFriends, NULL, NULL, NULL " +
                        "FROM orders"
                )
                db.execSQL("DROP TABLE IF EXISTS orders")
                db.execSQL("ALTER TABLE orders_new RENAME TO orders")
            }
        }

        @Volatile
        private var INSTANCE: RootieDatabase? = null

        @JvmStatic
        fun getDatabase(context: Context): RootieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RootieDatabase::class.java,
                    "rootie-db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
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
                .addMigrations(MIGRATION_23_24)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
