package com.veganbeauty.app.features.myskin;

import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BookingExpiryHelperTest {

    @Test
    public void shouldExpire_pendingBookingAfterAppointmentTime() {
        BookingHistoryEntity booking = sampleBooking("Chờ xác nhận", "07/07/2020", "10:00");
        Calendar now = Calendar.getInstance();
        now.set(2020, Calendar.JULY, 7, 15, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        assertTrue(BookingExpiryHelper.shouldExpire(booking, now));
        assertEquals(
                "Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.",
                BookingExpiryHelper.resolveCancelReason(booking.getStatus())
        );
    }

    @Test
    public void shouldNotExpire_whenAppointmentIsStillInFuture() {
        BookingHistoryEntity booking = sampleBooking("Chờ xác nhận", "07/07/2030", "15:00");
        Calendar now = Calendar.getInstance();
        now.set(2030, Calendar.JULY, 7, 14, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        assertFalse(BookingExpiryHelper.shouldExpire(booking, now));
    }

    @Test
    public void shouldNotExpire_whenAlreadySystemCancelled() {
        BookingHistoryEntity booking = sampleBooking("Đã huỷ", "07/07/2020", "10:00");
        booking.setCancelReason("Hệ thống tự động huỷ do quá thời gian hẹn nhưng chưa được Admin xác nhận lịch.");
        Calendar now = Calendar.getInstance();
        now.set(2020, Calendar.JULY, 7, 15, 0, 0);
        now.set(Calendar.MILLISECOND, 0);

        assertFalse(BookingExpiryHelper.shouldExpire(booking, now));
    }

    private static BookingHistoryEntity sampleBooking(String status, String dateDisplay, String time) {
        BookingHistoryEntity booking = new BookingHistoryEntity(
                "RS12345678",
                "user-1",
                "Test User",
                "0900000000",
                "test@example.com",
                "Soi da",
                dateDisplay,
                "Thứ 2",
                time,
                "30 phút",
                "Rootie Q1",
                "123 Street",
                status
        );
        booking.setMonthDisplay("Tháng 7");
        return booking;
    }
}
