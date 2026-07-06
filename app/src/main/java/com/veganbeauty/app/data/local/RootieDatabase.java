package com.veganbeauty.app.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.veganbeauty.app.data.local.ProductConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.veganbeauty.app.data.local.dao.CartDao;
import com.veganbeauty.app.data.local.dao.CommunityDao;
import com.veganbeauty.app.data.local.dao.OrderDao;
import com.veganbeauty.app.data.local.dao.ProductDao;
import com.veganbeauty.app.data.local.dao.RewardPointDao;
import com.veganbeauty.app.data.local.dao.SkinHistoryDao;
import com.veganbeauty.app.data.local.dao.StoreDao;
import com.veganbeauty.app.data.local.dao.UserDao;
import com.veganbeauty.app.data.local.dao.UserGiftDao;
import com.veganbeauty.app.data.local.dao.UserProductExpiryDao;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity;
import com.veganbeauty.app.data.local.entities.CommunityPostEntity;
import com.veganbeauty.app.data.local.entities.IngredientEntity;

import com.veganbeauty.app.data.local.entities.OrderEntity;

import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.ReelEntity;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;
import com.veganbeauty.app.data.local.entities.SkinHistoryEntity;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.local.entities.UserGiftEntity;
import com.veganbeauty.app.data.local.entities.UserMemoryEntity;
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;
import com.veganbeauty.app.data.local.entities.YtVideoEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.UUID;

@Database(
        entities = {
                UserEntity.class,
                CommunityPostEntity.class,
                ReelEntity.class,
                OrderEntity.class,
                ProductEntity.class,
                YtVideoEntity.class,
                UserMemoryEntity.class,
                IngredientEntity.class,
                CommunityBlogEntity.class,
                RewardPointEntity.class,
                UserGiftEntity.class,
                CartItemEntity.class,
                StoreEntity.class,
                UserProductExpiryEntity.class,
                SkinHistoryEntity.class
        },
        version = 39
)

@TypeConverters({ProductConverters.class})
public abstract class RootieDatabase extends RoomDatabase {

    public abstract CommunityDao communityDao();
    public abstract OrderDao orderDao();
    public abstract ProductDao productDao();
    public abstract RewardPointDao rewardPointDao();
    public abstract UserGiftDao userGiftDao();
    public abstract CartDao cartDao();
    public abstract UserDao userDao();
    public abstract StoreDao storeDao();
    public abstract UserProductExpiryDao userProductExpiryDao();
    public abstract SkinHistoryDao skinHistoryDao();

    private static final String TAG = "RootieDatabase";

    private static final Migration MIGRATION_23_24 = new Migration(23, 24) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
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
            );
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
            );
            db.execSQL("DROP TABLE IF EXISTS orders");
            db.execSQL("ALTER TABLE orders_new RENAME TO orders");
        }
    };

    private static volatile RootieDatabase INSTANCE;

    private static RootieDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                RootieDatabase.class,
                "rootie-db"
        )
        .addCallback(new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                super.onCreate(db);
                seedUsersFromAssets(context, db);
            }
        })
        .addMigrations(MIGRATION_23_24)
        .fallbackToDestructiveMigration()
        .build();
    }

    private static void seedUsersFromAssets(Context context, SupportSQLiteDatabase db) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("users.json")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();
                values.put("user_id", obj.optString("user_id", UUID.randomUUID().toString()));
                values.put("username", obj.optString("username", ""));
                values.put("full_name", obj.optString("full_name", ""));
                values.put("email", obj.optString("email", ""));
                values.put("phone", obj.optString("phone", ""));
                values.put("password", obj.optString("password", ""));
                values.put("avatar", obj.optString("avatar", ""));

                db.insert("users", SQLiteDatabase.CONFLICT_IGNORE, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isIntegrityError(Throwable error) {
        while (error != null) {
            String message = error.getMessage();
            if (message != null && message.toLowerCase().contains("integrity")) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    public static void resetDatabase(Context context) {
        synchronized (RootieDatabase.class) {
            if (INSTANCE != null) {
                if (INSTANCE.isOpen()) {
                    INSTANCE.close();
                }
                INSTANCE = null;
            }
            context.getApplicationContext().deleteDatabase("rootie-db");
        }
    }

    public static RootieDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RootieDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        INSTANCE = buildDatabase(context);
                        INSTANCE.getOpenHelper().getWritableDatabase();
                    } catch (RuntimeException e) {
                        if (isIntegrityError(e)) {
                            resetDatabase(context);
                            INSTANCE = buildDatabase(context);
                            INSTANCE.getOpenHelper().getWritableDatabase();
                        } else {
                            throw e;
                        }
                    }
                }
            }
        }
        return INSTANCE;
    }
}
