package com.veganbeauty.app.features.myskin;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Expires overdue spa bookings using data already loaded during sync.
 * No extra Firestore reads — only writes when a booking actually changes status.
 */
public final class BookingExpiryHelper {

    private static final String TAG = "BookingExpiryHelper";
    private static final String STATUS_CANCELLED = "Đã huỷ";
    private static final String SYSTEM_CANCEL_PREFIX = "Hệ thống tự động";
    private static final String REASON_UNCONFIRMED =
            "Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.";
    private static final String REASON_NO_SHOW =
            "Hệ thống tự động huỷ do đã quá thời gian hẹn mà khách không đến Spa.";

    private BookingExpiryHelper() {
    }

    /**
     * Call after {@link LocalJsonReader#mergeBookingsFromRemote(List)} during sync.
     * Safe to call repeatedly — skips bookings already cancelled or already processed.
     */
    public static void expireOverdueBookings(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        LocalJsonReader reader = new LocalJsonReader(appContext);
        List<BookingHistoryEntity> bookings = collectCurrentUserBookings(appContext, reader);
        if (bookings.isEmpty()) {
            return;
        }

        Calendar now = Calendar.getInstance();
        FirestoreService firestoreService = new FirestoreService();
        int expiredCount = 0;

        for (BookingHistoryEntity booking : bookings) {
            if (!shouldExpire(booking, now)) {
                continue;
            }

            String bookingId = booking.getId();
            if (bookingId == null || bookingId.trim().isEmpty()) {
                continue;
            }

            String previousStatus = booking.getStatus();
            String cancelReason = resolveCancelReason(previousStatus);

            if (firestoreService.updateBookingStatus(bookingId, STATUS_CANCELLED, cancelReason)) {
                reader.updateBookingStatus(bookingId, STATUS_CANCELLED, cancelReason);
                expiredCount++;
                Log.i(TAG, "Expired booking " + bookingId + " (" + previousStatus + " -> " + STATUS_CANCELLED + ")");
            } else {
                Log.w(TAG, "Failed to expire booking " + bookingId + " on Firestore; local state unchanged");
            }
        }

        if (expiredCount > 0) {
            Log.i(TAG, "Expired " + expiredCount + " overdue booking(s)");
        }
    }

    static boolean shouldExpire(BookingHistoryEntity booking, Calendar now) {
        if (booking == null || now == null) {
            return false;
        }
        if (!isActiveBookingStatus(booking.getStatus())) {
            return false;
        }
        if (isAlreadySystemCancelled(booking)) {
            return false;
        }
        Calendar appointmentTime = parseAppointmentTime(booking);
        return appointmentTime != null && appointmentTime.before(now);
    }

    static boolean isActiveBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        String value = status.trim();
        return "Sắp diễn ra".equals(value)
                || "Chờ xác nhận".equals(value)
                || "pending".equalsIgnoreCase(value);
    }

    static boolean isAlreadySystemCancelled(BookingHistoryEntity booking) {
        if (booking == null) {
            return false;
        }
        if (isCancelledStatus(booking.getStatus())) {
            return true;
        }
        String reason = booking.getCancelReason();
        return reason != null && reason.startsWith(SYSTEM_CANCEL_PREFIX);
    }

    static boolean isCancelledStatus(String status) {
        if (status == null) {
            return false;
        }
        String value = status.trim();
        return STATUS_CANCELLED.equals(value) || "Đã hủy".equals(value);
    }

    static String resolveCancelReason(String previousStatus) {
        if (previousStatus != null
                && ("Chờ xác nhận".equals(previousStatus.trim())
                || "pending".equalsIgnoreCase(previousStatus.trim()))) {
            return REASON_UNCONFIRMED;
        }
        return REASON_NO_SHOW;
    }

    static Calendar parseAppointmentTime(BookingHistoryEntity booking) {
        if (booking == null) {
            return null;
        }
        try {
            Calendar cal = Calendar.getInstance();
            int currentYear = cal.get(Calendar.YEAR);

            String dateDisplay = booking.getDateDisplay();
            String time = booking.getTime();

            int day = 1;
            int month = cal.get(Calendar.MONTH);
            int year = currentYear;

            if (dateDisplay != null && !dateDisplay.isEmpty()) {
                if (dateDisplay.contains("/")) {
                    String[] parts = dateDisplay.split("/");
                    if (parts.length >= 1) {
                        day = Integer.parseInt(parts[0].trim());
                    }
                    if (parts.length >= 2) {
                        month = Integer.parseInt(parts[1].trim()) - 1;
                    }
                    if (parts.length >= 3) {
                        year = Integer.parseInt(parts[2].trim());
                    }
                } else {
                    day = Integer.parseInt(dateDisplay.trim().split(" ")[0]);
                    String monthDisplay = booking.getMonthDisplay();
                    if (monthDisplay != null && monthDisplay.contains("Tháng")) {
                        String firstLine = monthDisplay.split("\n")[0];
                        String monthDigits = firstLine.replaceAll("[^0-9]", "");
                        if (!monthDigits.isEmpty()) {
                            month = Integer.parseInt(monthDigits) - 1;
                        }
                    }
                }
            }

            int hour = 0;
            int minute = 0;
            if (time != null && !time.isEmpty()) {
                String startTime = time.split("-")[0].trim();
                String[] timeParts = startTime.split(":");
                if (timeParts.length >= 2) {
                    hour = Integer.parseInt(timeParts[0].trim());
                    minute = Integer.parseInt(timeParts[1].trim());
                }
            }

            cal.set(year, month, day, hour, minute, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }

    private static List<BookingHistoryEntity> collectCurrentUserBookings(
            Context appContext,
            LocalJsonReader reader
    ) {
        Map<String, BookingHistoryEntity> byId = new LinkedHashMap<>();

        if (ProfileSession.isLoggedIn(appContext)) {
            String userId = ProfileSessionHelper.getEffectiveUserId(appContext);
            if (userId != null && !userId.trim().isEmpty()) {
                addBookings(byId, reader.getUserBookingHistory(userId.trim()));
            }
        }

        String email = ProfileSession.getEmail(appContext);
        if (email != null && !email.trim().isEmpty()) {
            addBookings(byId, reader.getUserBookingHistory(email.trim()));
        }

        return new ArrayList<>(byId.values());
    }

    private static void addBookings(Map<String, BookingHistoryEntity> byId, List<BookingHistoryEntity> bookings) {
        if (bookings == null) {
            return;
        }
        for (BookingHistoryEntity booking : bookings) {
            if (booking == null || booking.getId() == null || booking.getId().trim().isEmpty()) {
                continue;
            }
            byId.put(booking.getId(), booking);
        }
    }
}
