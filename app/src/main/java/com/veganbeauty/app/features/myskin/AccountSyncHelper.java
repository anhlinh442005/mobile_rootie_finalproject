package com.veganbeauty.app.features.myskin;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.data.repository.OrderRepository;
import com.veganbeauty.app.data.repository.OrderStatusNotifier;
import com.veganbeauty.app.features.auth.FreshDemoAccountSeeder;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AccountSyncHelper {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private AccountSyncHelper() {
    }

    public interface Callback {
        void onComplete();
    }

    public static void sync(Context context, Callback callback) {
        if (context == null) {
            if (callback != null) {
                callback.onComplete();
            }
            return;
        }

        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                if (ProfileSession.isLoggedIn(appContext)
                        && FreshDemoAccountSeeder.isDemoAccount(
                                ProfileSessionHelper.getEffectiveUserId(appContext),
                                ProfileSession.getEmail(appContext))) {
                    return;
                }
                syncBookings(appContext);
                syncOrders(appContext);
                NotificationRepository.getInstance(appContext).refreshNotifications();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (callback != null) {
                    callback.onComplete();
                }
            }
        });
    }

    private static void syncBookings(Context appContext) {
        FirestoreService firestoreService = new FirestoreService();
        java.util.LinkedHashMap<String, BookingHistoryEntity> merged = new java.util.LinkedHashMap<>();

        if (ProfileSession.isLoggedIn(appContext)) {
            String userId = ProfileSessionHelper.getEffectiveUserId(appContext);
            if (userId != null && !userId.trim().isEmpty()) {
                List<BookingHistoryEntity> byUserId =
                        firestoreService.fetchBookingsForUserByUserId(userId.trim());
                if (byUserId != null) {
                    for (BookingHistoryEntity booking : byUserId) {
                        if (booking != null && booking.getId() != null) {
                            merged.put(booking.getId(), booking);
                        }
                    }
                }
            }
        }

        String userEmail = ProfileSession.getEmail(appContext);
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            List<BookingHistoryEntity> byEmail =
                    firestoreService.fetchBookingsForUser(userEmail.trim());
            if (byEmail != null) {
                for (BookingHistoryEntity booking : byEmail) {
                    if (booking != null && booking.getId() != null) {
                        merged.put(booking.getId(), booking);
                    }
                }
            }
        }

        LocalJsonReader localJsonReader = new LocalJsonReader(appContext);
        if (!merged.isEmpty()) {
            localJsonReader.mergeBookingsFromRemote(new java.util.ArrayList<>(merged.values()));
        }
        BookingExpiryHelper.expireOverdueBookings(appContext);
        // Lịch SPA hoàn thành ↔ lịch sử soi da offline (ngày giờ + trạng thái)
        BookingSkinScanResultHelper.syncOfflineHistoryFromCompletedBookings(appContext);
    }

    private static void syncOrders(Context appContext) {
        String userId = null;
        String phone = ProfileSession.getPhone(appContext);
        if (ProfileSession.isLoggedIn(appContext)) {
            userId = ProfileSessionHelper.getEffectiveUserId(appContext);
        } else {
            phone = ProfileSession.getGuestPhone(appContext);
        }

        String safeUserId = userId != null ? userId.trim() : "";
        String safePhone = phone != null ? phone.trim() : "";
        if (safeUserId.isEmpty() && safePhone.isEmpty()) {
            return;
        }

        RootieDatabase db = RootieDatabase.getDatabase(appContext);
        OrderRepository orderRepository = new OrderRepository(
                db.orderDao(),
                db.rewardPointDao(),
                db.userGiftDao(),
                new LocalJsonReader(appContext)
        );
        orderRepository.startListeningToOrders(safeUserId, safePhone);
        OrderStatusNotifier.backfillMissedOrderStatusNotifications(
                appContext,
                db.orderDao(),
                safeUserId,
                safePhone
        );
        OrderStatusNotifier.backfillPendingOrderNotifications(
                appContext,
                db.orderDao(),
                safeUserId
        );
    }
}
