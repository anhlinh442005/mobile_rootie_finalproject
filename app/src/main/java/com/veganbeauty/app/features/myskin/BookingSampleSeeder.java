package com.veganbeauty.app.features.myskin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.veganbeauty.app.BuildConfig;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tạo 1 đơn SPA mẫu (đã hoàn thành) tại Cơ sở 1 — dùng cho demo admin/user.
 * Chạy một lần trên bản debug, upload Firestore + lưu local.
 */
public final class BookingSampleSeeder {

    private static final String TAG = "BookingSampleSeeder";
    private static final String PREFS = "RootieQuizPrefs";
    private static final String PREF_KEY = "sample_booking_test_user_v2";
    public static final String SAMPLE_BOOKING_ID = "RS20260301CH001";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private BookingSampleSeeder() {
    }

    public static BookingHistoryEntity buildSampleCompletedBooking() {
        BookingHistoryEntity booking = new BookingHistoryEntity(
                SAMPLE_BOOKING_ID,
                "test_001",
                "Test User",
                "097 795 4530",
                "test@example.com",
                "Soi da chuyên sâu",
                "28/02/2026",
                "Tháng 02",
                "Thứ 7",
                "14:00",
                "45 phút",
                "Rootie Cơ sở 1 - Quận 1",
                "235 Nguyễn Thị Minh Khai, P. Nguyễn Cư Trinh, Q.1, TP.HCM",
                "(028) 3822 9988",
                "https://images.unsplash.com/photo-1616394584738-fc6e612e71b9?w=800",
                "Da nhạy cảm, muốn tư vấn routine thuần chay cho da test.",
                "Đã hoàn thành",
                "Hủy trước 2 giờ không mất phí. Đến trễ quá 15 phút có thể đổi lịch.",
                "2026-02-25T10:15:00",
                "28/02/2026 15:35:00",
                Arrays.asList(
                        "Độ ẩm da tăng +18%",
                        "Giảm đỏ da vùng má",
                        "Lỗ chân lông sạch và thông thoáng hơn"
                ),
                "Trần Minh Châu",
                "https://i.pinimg.com/736x/1a/d8/4b/1ad84b9ab4a1e2ab17c7aab37fcff0a5.jpg",
                4.9f,
                5.0f,
                "Chuyên viên rất nhiệt tình, soi da kỹ và gợi ý sản phẩm Bí Đao phù hợp. Rất hài lòng với dịch vụ tại Cơ sở 1.",
                "28/02/2026",
                "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?w=600",
                "https://images.unsplash.com/photo-1596755389378-c31d21fd1273?w=600",
                50,
                350,
                "28/03/2026",
                "Nên tái khám sau 30 ngày",
                "",
                ""
        );
        return booking;
    }

    /** Upload mẫu lên Firestore + local (một lần, bản debug). */
    public static void seedIfNeeded(Context context) {
        if (!BuildConfig.DEBUG || context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(PREF_KEY, false)) {
            return;
        }

        EXECUTOR.execute(() -> {
            try {
                BookingHistoryEntity booking = buildSampleCompletedBooking();
                new LocalJsonReader(appContext).addBooking(booking);
                boolean uploaded = new FirestoreService().uploadBookingMap(
                        SAMPLE_BOOKING_ID, buildFirestoreMap(booking));
                if (uploaded) {
                    prefs.edit().putBoolean(PREF_KEY, true).apply();
                    Log.i(TAG, "Sample booking uploaded: " + SAMPLE_BOOKING_ID);
                } else {
                    Log.w(TAG, "Sample booking upload failed");
                }
            } catch (Exception e) {
                Log.e(TAG, "seedIfNeeded failed", e);
            }
        });
    }

    private static Map<String, Object> buildFirestoreMap(BookingHistoryEntity booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("userId", booking.getUserId());
        map.put("userName", booking.getUserName());
        map.put("userPhone", booking.getUserPhone());
        map.put("userEmail", booking.getUserEmail());
        map.put("email", booking.getUserEmail());
        map.put("serviceName", booking.getServiceName());
        map.put("dateDisplay", booking.getDateDisplay());
        map.put("monthDisplay", booking.getMonthDisplay());
        map.put("dayOfWeek", booking.getDayOfWeek());
        map.put("time", booking.getTime());
        map.put("duration", booking.getDuration());
        map.put("storeName", booking.getStoreName());
        map.put("storeAddress", booking.getStoreAddress());
        map.put("storePhone", booking.getStorePhone());
        map.put("storeImage", booking.getStoreImage());
        map.put("storeID", "CH001");
        map.put("storeId", "CH001");
        map.put("note", booking.getNote());
        map.put("status", booking.getStatus());
        map.put("policy", booking.getPolicy());
        map.put("createdAt", booking.getCreatedAt());
        map.put("completedAt", booking.getCompletedAt());
        map.put("approvedAt", "2026-02-26T09:20:00");
        map.put("visitedAt", "2026-02-28T14:05:00");
        map.put("checkInCode", "CH001-2847");
        map.put("skinResults", booking.getSkinResults());
        map.put("consultantName", booking.getConsultantName());
        map.put("consultantAvatar", booking.getConsultantAvatar());
        map.put("consultantRating", booking.getConsultantRating());
        map.put("userRating", booking.getUserRating());
        map.put("userReview", booking.getUserReview());
        map.put("reviewDate", booking.getReviewDate());
        map.put("beforeImage", booking.getBeforeImage());
        map.put("afterImage", booking.getAfterImage());
        map.put("feedbackImageUrls", Arrays.asList(
                booking.getBeforeImage(),
                booking.getAfterImage()
        ));
        map.put("earnedPoints", booking.getEarnedPoints());
        map.put("totalPoints", booking.getTotalPoints());
        map.put("nextAppointmentDate", booking.getNextAppointmentDate());
        map.put("nextAppointmentText", booking.getNextAppointmentText());
        map.put("cancelledAt", booking.getCancelledAt());
        map.put("cancelReason", booking.getCancelReason());
        return map;
    }

    /** Ép upload lại (debug / test thủ công). */
    public static void forceUpload(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            BookingHistoryEntity booking = buildSampleCompletedBooking();
            new LocalJsonReader(appContext).addBooking(booking);
            boolean ok = new FirestoreService().uploadBookingMap(SAMPLE_BOOKING_ID, buildFirestoreMap(booking));
            Log.i(TAG, "forceUpload " + SAMPLE_BOOKING_ID + " => " + ok);
        });
    }
}
