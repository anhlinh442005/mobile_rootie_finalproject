package com.veganbeauty.app.features.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.dao.UserDao;
import com.veganbeauty.app.data.local.entities.UserEntity;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tài khoản demo Nguyễn Thị Demo — luôn bắt đầu sạch.
 * Chỉ giữ thông tin đã import (users.json / hằng số bên dưới), không kế thừa địa chỉ/đơn/xu từ tài khoản cũ.
 */
public final class FreshDemoAccountSeeder {

    public static final String USER_ID = "demo_nb195533";
    public static final String FULL_NAME = "Nguyễn Thị Demo";
    public static final String EMAIL = "nb195533@gmail.com";
    public static final String PHONE = "0901955330";
    /** SHA-256 của {@code password123@} */
    public static final String PASSWORD_HASH =
            "8a4fb166b0079adc4d54957ee05003a4a1af3d7b7e5562a670065b8c27c0c98a";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private FreshDemoAccountSeeder() {
    }

    public static boolean isDemoAccount(@Nullable String userId, @Nullable String email) {
        if (userId != null && USER_ID.equals(userId.trim())) {
            return true;
        }
        return email != null && EMAIL.equalsIgnoreCase(email.trim());
    }

    /** Gọi trên background thread khi đăng nhập demo — xóa data cũ, áp profile import. */
    public static void resetLocalDataBlocking(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        ProfileSession.resetUserScopedData(appContext);
        ProfileSession.clearLocalProfileEdits(appContext);
        ProfileSession.setQuizReminderDismissedWeekly(appContext, false);

        // Xóa sạch địa chỉ / CCCD / bio / guest leftover từ tài khoản trước
        ProfileSession.clearSavedAddresses(appContext);
        ProfileSession.setGuestPhone(appContext, "");
        ProfileSession.setBio(appContext, "");
        ProfileSession.setPrimaryImage(appContext, "");
        ProfileSession.setCCCD(appContext, "");
        ProfileSession.setSkinStreak(appContext, 0);

        // Chỉ ghi lại thông tin đã import cho demo
        applyImportedDemoProfile(appContext);

        RootieDatabase db = RootieDatabase.getDatabase(appContext);
        db.skinHistoryDao().deleteByUser(USER_ID, EMAIL);
        // Chỉ xóa xu/quà của demo — không đụng điểm của user khác
        db.rewardPointDao().deleteByUserId(USER_ID);
        db.userGiftDao().deleteByUserId(USER_ID);
        db.orderDao().deleteByUserIdentity(USER_ID, PHONE);
        db.cartDao().clearCart();
        new LocalJsonReader(appContext).removeBookingsForUser(USER_ID, EMAIL);

        // Inbox phải sạch theo tài khoản — không giữ đơn/lịch cũ từ lần đăng nhập trước
        clearDemoNotifications(appContext);

        // Xóa lịch điểm danh legacy (từng không gắn userId)
        appContext.getSharedPreferences("checkin_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        SharedPreferences prefs = appContext.getSharedPreferences("rootie_profile_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("last_active_user_id", USER_ID)
                .putInt("skin_max_streak", 0)
                .remove("june_2026_seeded_v1")
                // Đảm bảo không còn list địa chỉ checkout cũ
                .remove("saved_addresses_list_json")
                .remove("addr_home_name")
                .remove("addr_home_phone")
                .remove("addr_home_addr")
                .remove("addr_office_name")
                .remove("addr_office_phone")
                .remove("addr_office_addr")
                .remove("addr_default_type")
                .putString("address", "")
                .apply();
    }

    private static void clearDemoNotifications(Context appContext) {
        NotificationRepository.getInstance(appContext).clearInboxForUser(USER_ID);

        File communityNoti = new File(
                appContext.getFilesDir(),
                "local_notifications_" + USER_ID + ".json");
        if (communityNoti.exists()) {
            //noinspection ResultOfMethodCallIgnored
            communityNoti.delete();
        }

        appContext.getSharedPreferences("order_notification_tracker", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    /** Chỉ các field có trong users.json / seed — không địa chỉ, không CCCD. */
    public static void applyImportedDemoProfile(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        ProfileSession.setUserId(appContext, USER_ID);
        ProfileSession.setFullName(appContext, FULL_NAME);
        ProfileSession.setUsername(appContext, FULL_NAME);
        ProfileSession.setEmail(appContext, EMAIL);
        ProfileSession.setPhone(appContext, PHONE);
        ProfileSession.setAddress(appContext, "");
        ProfileSession.setCCCD(appContext, "");
        ProfileSession.setBio(appContext, "");
        ProfileSession.setPrimaryImage(appContext, "");
        ProfileSession.setGuestPhone(appContext, "");
        ProfileSession.clearSavedAddresses(appContext);
        ProfileSession.clearLocalProfileEdits(appContext);
        ProfileSession.setAvatar(appContext, ProfileSessionHelper.DEFAULT_AVATAR_URL);
    }

    public static void seedIfNeeded(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                UserDao userDao = RootieDatabase.getDatabase(appContext).userDao();
                UserEntity byId = userDao.getUserByIdSync(USER_ID);
                UserEntity byEmail = userDao.getUserByEmailSync(EMAIL);

                if (byId != null) {
                    byId.setUsername(FULL_NAME);
                    byId.setFull_name(FULL_NAME);
                    byId.setEmail(EMAIL);
                    byId.setPhone(PHONE);
                    byId.setPassword(PASSWORD_HASH);
                    if (byId.getAvatar() == null || byId.getAvatar().trim().isEmpty()) {
                        byId.setAvatar(ProfileSessionHelper.DEFAULT_AVATAR_URL);
                    }
                    userDao.insertUserSync(byId);
                    return;
                }

                if (byEmail != null) {
                    byEmail.setUsername(FULL_NAME);
                    byEmail.setFull_name(FULL_NAME);
                    byEmail.setPhone(PHONE);
                    byEmail.setPassword(PASSWORD_HASH);
                    if (byEmail.getAvatar() == null || byEmail.getAvatar().trim().isEmpty()) {
                        byEmail.setAvatar(ProfileSessionHelper.DEFAULT_AVATAR_URL);
                    }
                    userDao.insertUserSync(byEmail);
                    return;
                }

                userDao.insertUserSync(new UserEntity(
                        USER_ID,
                        FULL_NAME,
                        FULL_NAME,
                        EMAIL,
                        PHONE,
                        PASSWORD_HASH,
                        ProfileSessionHelper.DEFAULT_AVATAR_URL,
                        null
                ));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
