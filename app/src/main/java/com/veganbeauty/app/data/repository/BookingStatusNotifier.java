package com.veganbeauty.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.NotificationItem;
import com.veganbeauty.app.features.account.notification.NotificationPushHelper;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BookingStatusNotifier {

    private static final String TAG = "BookingStatusNotifier";

    private BookingStatusNotifier() {
    }

    public static void notifyIfStatusChanged(
            Context context,
            BookingHistoryEntity booking,
            String previousStatus,
            String newStatus
    ) {
        if (context == null || booking == null || newStatus == null || newStatus.trim().isEmpty()) {
            return;
        }
        if (newStatus.equals(previousStatus)) {
            return;
        }

        // Chỉ đẩy thông báo vào inbox của đúng chủ lịch hẹn đang đăng nhập
        String sessionUserId = ProfileSessionHelper.getEffectiveUserId(context);
        String bookingUserId = booking.getUserId() != null ? booking.getUserId().trim() : "";
        if (sessionUserId == null || sessionUserId.trim().isEmpty()) {
            return;
        }
        if (!bookingUserId.isEmpty() && !sessionUserId.trim().equals(bookingUserId)) {
            Log.i(TAG, "Skip booking notification for " + booking.getId()
                    + " — owner " + bookingUserId + " != session " + sessionUserId);
            return;
        }

        String normalized = normalizeStatus(newStatus);
        String title;
        String content;

        switch (normalized) {
            case "pending":
                title = "Đặt lịch soi da thành công";
                content = buildBookingMessage(
                        booking,
                        "đã được ghi nhận và đang chờ Rootie xác nhận."
                );
                break;
            case "confirmed":
                title = "Lịch hẹn soi da đã được xác nhận";
                content = buildBookingMessage(
                        booking,
                        "đã được xác nhận. Hãy đến đúng giờ nhé!"
                );
                break;
            case "cancelled":
                title = "Lịch hẹn soi da đã bị huỷ";
                content = buildBookingMessage(
                        booking,
                        "đã bị huỷ" + cancelSuffix(booking) + "."
                );
                break;
            case "completed":
                title = "Lịch hẹn soi da đã hoàn thành";
                content = buildBookingMessage(
                        booking,
                        "đã hoàn thành. Cảm ơn bạn đã tin tưởng Rootie!"
                );
                break;
            default:
                title = "Cập nhật lịch hẹn soi da";
                content = buildBookingMessage(booking, "đã được cập nhật: " + newStatus + ".");
                break;
        }

        dispatch(context.getApplicationContext(), booking.getId(), title, content);
    }

    private static void dispatch(Context appContext, String bookingId, String title, String content) {
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String notificationId = "booking_" + bookingId + "_" + System.currentTimeMillis();

        NotificationItem item = new NotificationItem(
                notificationId,
                title,
                content,
                now,
                "Lịch hẹn",
                null,
                null,
                "XEM LỊCH HẸN",
                false,
                "Hôm nay",
                "ic_bell",
                "schedule date",
                null,
                bookingId
        );

        NotificationRepository.getInstance(appContext).addNotification(item);
        NotificationPushHelper.sendPushNotification(
                appContext,
                notificationId,
                title,
                content,
                "schedule date",
                null,
                null,
                bookingId
        );
        Log.i(TAG, "Booking notification dispatched for " + bookingId + ": " + title);
    }

    private static String buildBookingMessage(BookingHistoryEntity booking, String statusPhrase) {
        String service = booking.getServiceName() != null ? booking.getServiceName() : "Soi da";
        String when = formatSchedule(booking);
        String store = booking.getStoreName() != null ? booking.getStoreName() : "Rootie";
        return "Lịch " + service + " (#" + booking.getId() + ") lúc " + when + " tại " + store + " "
                + statusPhrase;
    }

    private static String formatSchedule(BookingHistoryEntity booking) {
        String date = booking.getDateDisplay() != null ? booking.getDateDisplay() : "";
        String time = booking.getTime() != null ? booking.getTime() : "";
        if (!date.isEmpty() && !time.isEmpty()) {
            return time + " ngày " + date;
        }
        if (!date.isEmpty()) {
            return date;
        }
        return time.isEmpty() ? "thời gian đã đặt" : time;
    }

    private static String cancelSuffix(BookingHistoryEntity booking) {
        String reason = booking.getCancelReason();
        if (reason == null || reason.trim().isEmpty()) {
            return "";
        }
        return " (" + reason.trim() + ")";
    }

    private static String normalizeStatus(String status) {
        if (status == null) {
            return "";
        }
        String value = status.trim().toLowerCase(Locale.ROOT);
        if (value.equals("chờ xác nhận") || value.equals("pending")) {
            return "pending";
        }
        if (value.equals("sắp diễn ra") || value.equals("đã xác nhận")) {
            return "confirmed";
        }
        if (value.equals("đã huỷ") || value.equals("đã hủy")) {
            return "cancelled";
        }
        if (value.equals("đã hoàn thành")) {
            return "completed";
        }
        return value;
    }
}
