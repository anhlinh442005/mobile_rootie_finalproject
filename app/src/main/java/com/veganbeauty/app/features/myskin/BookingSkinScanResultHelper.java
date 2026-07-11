package com.veganbeauty.app.features.myskin;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.SkinHistoryLocalStore;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.utils.ProfileSessionHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kết quả soi da gắn với lịch SPA, và đồng bộ sang lịch sử "Soi da offline".
 */
public final class BookingSkinScanResultHelper {

    private static final String TAG = "BookingSkinScanSync";
    private static final String SCAN_TYPE_OFFLINE = "Soi da offline";
    private static final String STATUS_COMPLETED = "Đã hoàn thành";
    private static final String OFFLINE_ID_PREFIX = "sh_offline_";

    private BookingSkinScanResultHelper() {
    }

    /**
     * Đồng bộ lịch SPA đã hoàn thành ↔ lịch sử soi da offline (ngày giờ + trạng thái).
     * Gọi trên background thread.
     */
    public static void syncOfflineHistoryFromCompletedBookings(Context context) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (!ProfileSession.isLoggedIn(appContext)) {
            return;
        }

        String userId = ProfileSessionHelper.getEffectiveUserId(appContext);
        String email = ProfileSession.getEmail(appContext);
        if ((userId == null || userId.trim().isEmpty())
                && (email == null || email.trim().isEmpty())) {
            return;
        }

        List<BookingHistoryEntity> bookings = collectCurrentUserBookings(appContext);
        if (bookings.isEmpty()) {
            return;
        }

        JSONArray existingHistory = SkinHistoryLocalStore.getHistory(appContext, userId, email);
        Set<String> completedBookingIds = new HashSet<>();
        int upserted = 0;
        int removed = 0;

        for (BookingHistoryEntity booking : bookings) {
            if (booking == null || TextUtils.isEmpty(booking.getId())) {
                continue;
            }
            String bookingId = booking.getId().trim();
            boolean completed = isCompletedStatus(booking.getStatus());

            if (completed) {
                completedBookingIds.add(bookingId);
                if (upsertOfflineHistory(appContext, booking, existingHistory, userId, email)) {
                    upserted++;
                }
            }
        }

        // Xóa bản ghi offline gắn booking không còn "Đã hoàn thành"
        for (int i = 0; i < existingHistory.length(); i++) {
            try {
                JSONObject item = existingHistory.getJSONObject(i);
                if (!SCAN_TYPE_OFFLINE.equals(item.optString("scanType", ""))) {
                    continue;
                }
                String linkedId = item.optString("bookingId", item.optString("booking_id", "")).trim();
                if (linkedId.isEmpty()) {
                    continue;
                }
                if (completedBookingIds.contains(linkedId)) {
                    continue;
                }
                // Chỉ xóa bản ghi do sync tạo (hoặc còn gắn booking đã huỷ/đổi trạng thái)
                boolean ownedByBooking = false;
                for (BookingHistoryEntity booking : bookings) {
                    if (booking != null && linkedId.equals(booking.getId())) {
                        ownedByBooking = true;
                        break;
                    }
                }
                if (!ownedByBooking) {
                    continue;
                }
                String historyId = item.optString("id", "");
                if (!historyId.isEmpty()) {
                    SkinHistoryLocalStore.deleteSync(appContext, historyId);
                    removed++;
                }
            } catch (Exception ignored) {
            }
        }

        if (upserted > 0 || removed > 0) {
            Log.i(TAG, "Synced offline skin history: upserted=" + upserted + ", removed=" + removed);
        }
    }

    private static boolean upsertOfflineHistory(
            Context context,
            BookingHistoryEntity booking,
            JSONArray existingHistory,
            String userId,
            String email
    ) {
        try {
            String bookingId = booking.getId().trim();
            String date = normalizeDate(booking.getDateDisplay());
            String time = extractTime(booking.getTime());
            String historyId = offlineHistoryId(bookingId);

            JSONObject existing = findLinkedOfflineHistory(existingHistory, bookingId);
            JSONObject data;
            if (existing != null) {
                data = new JSONObject(existing.toString());
            } else {
                data = buildFromBooking(context, booking);
            }

            if (data == null) {
                return false;
            }

            // Giữ id ổn định theo booking để không tạo trùng
            if (existing != null && !TextUtils.isEmpty(existing.optString("id", ""))) {
                historyId = existing.optString("id");
            }
            data.put("id", historyId);
            data.put("bookingId", bookingId);
            data.put("date", date);
            data.put("time", time);
            data.put("scanType", SCAN_TYPE_OFFLINE);
            data.put("bookingStatus", STATUS_COMPLETED);
            if (!TextUtils.isEmpty(booking.getServiceName())) {
                data.put("serviceName", booking.getServiceName());
            }
            if (!TextUtils.isEmpty(booking.getStoreName())) {
                data.put("storeName", booking.getStoreName());
            }

            boolean needsWrite = existing == null
                    || !date.equals(normalizeDate(existing.optString("date", "")))
                    || !time.equals(extractTime(existing.optString("time", "")))
                    || !SCAN_TYPE_OFFLINE.equals(existing.optString("scanType", ""))
                    || !STATUS_COMPLETED.equals(existing.optString("bookingStatus", ""))
                    || !bookingId.equals(existing.optString("bookingId", existing.optString("booking_id", "")));

            if (!needsWrite) {
                return false;
            }

            SkinHistoryLocalStore.saveSync(context, data, userId, email);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to upsert offline history for booking " + booking.getId(), e);
            return false;
        }
    }

    private static String offlineHistoryId(String bookingId) {
        return OFFLINE_ID_PREFIX + bookingId;
    }

    private static boolean isCompletedStatus(String status) {
        return status != null && STATUS_COMPLETED.equalsIgnoreCase(status.trim());
    }

    private static List<BookingHistoryEntity> collectCurrentUserBookings(Context appContext) {
        Map<String, BookingHistoryEntity> byId = new LinkedHashMap<>();
        LocalJsonReader reader = new LocalJsonReader(appContext);

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

    static JSONObject resolveScanResult(Context context, BookingHistoryEntity booking) {
        if (booking == null) {
            return null;
        }

        try {
            JSONObject fromBookingDoc = new FirestoreService().getBookingSkinScanResult(booking.getId());
            if (isValidScanResult(fromBookingDoc)) {
                return fromBookingDoc;
            }
        } catch (Exception ignored) {
        }

        String userId = ProfileSession.getUserId(context);
        String email = ProfileSession.getEmail(context);

        JSONObject fromLocalDbHistory = findInHistory(
                SkinHistoryLocalStore.getHistory(context, userId, email), booking);
        if (isValidScanResult(fromLocalDbHistory)) {
            return fromLocalDbHistory;
        }

        JSONObject fromLocalHistory = findInHistory(
                new LocalJsonReader(context).getSkinHistory(), booking);
        if (isValidScanResult(fromLocalHistory)) {
            return fromLocalHistory;
        }

        return buildFromBooking(context, booking);
    }

    static JSONObject buildFromBooking(Context context, BookingHistoryEntity booking) {
        try {
            JSONObject data = new JSONObject(getTemplate(context).toString());
            data.put("date", booking.getDateDisplay());
            data.put("time", extractTime(booking.getTime()));
            data.put("scanType", "Soi da offline");
            data.put("bookingId", booking.getId());

            String imageUrl = booking.getBeforeImage();
            if (TextUtils.isEmpty(imageUrl)) {
                imageUrl = booking.getAfterImage();
            }
            if (!TextUtils.isEmpty(imageUrl)) {
                data.put("imageUrl", imageUrl);
            }

            JSONArray suggestions = new JSONArray();
            StringBuilder summary = new StringBuilder();
            if (booking.getSkinResults() != null && !booking.getSkinResults().isEmpty()) {
                for (int i = 0; i < booking.getSkinResults().size(); i++) {
                    String item = booking.getSkinResults().get(i);
                    if (TextUtils.isEmpty(item)) {
                        continue;
                    }
                    suggestions.put(item);
                    if (summary.length() > 0) {
                        summary.append("\n");
                    }
                    summary.append("• ").append(item);
                }
            }

            if (suggestions.length() > 0) {
                data.put("suggestions", suggestions);
                data.put("summaryText", summary.toString());
                data.put("overallCondition", "Kết quả sau soi da");
            } else {
                data.put("summaryText", "Chưa có ghi nhận chi tiết cho lịch hẹn này.");
                data.put("overallCondition", "Chưa có dữ liệu");
            }

            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private static JSONObject findLinkedOfflineHistory(JSONArray history, String bookingId) {
        if (history == null || history.length() == 0 || TextUtils.isEmpty(bookingId)) {
            return null;
        }
        String expectedId = offlineHistoryId(bookingId);
        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject item = history.getJSONObject(i);
                String id = item.optString("id", "");
                if (expectedId.equals(id)) {
                    return item;
                }
                String linkedBookingId = item.optString("bookingId", item.optString("booking_id", ""));
                if (bookingId.equals(linkedBookingId)
                        && SCAN_TYPE_OFFLINE.equals(item.optString("scanType", ""))) {
                    return item;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static JSONObject findInHistory(JSONArray history, BookingHistoryEntity booking) {
        if (history == null || history.length() == 0 || booking == null) {
            return null;
        }

        JSONObject dateTimeMatch = null;
        String bookingDate = normalizeDate(booking.getDateDisplay());
        String bookingTime = extractTime(booking.getTime());

        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject item = history.getJSONObject(i);
                if (!isValidScanResult(item)) {
                    continue;
                }

                String linkedBookingId = item.optString("bookingId", item.optString("booking_id", ""));
                if (!TextUtils.isEmpty(linkedBookingId) && linkedBookingId.equals(booking.getId())) {
                    return item;
                }

                if (dateTimeMatch == null
                        && bookingDate.equals(normalizeDate(item.optString("date", "")))
                        && bookingTime.equals(extractTime(item.optString("time", "")))) {
                    dateTimeMatch = item;
                }
            } catch (Exception ignored) {
            }
        }

        return dateTimeMatch;
    }

    private static JSONObject getTemplate(Context context) {
        if (context != null) {
            try {
                JSONArray history = new LocalJsonReader(context).getSkinHistory();
                if (history.length() > 0) {
                    return history.getJSONObject(0);
                }
            } catch (Exception ignored) {
            }
        }

        try {
            return new JSONObject(
                    "{"
                            + "\"score\":78,"
                            + "\"overallCondition\":\"Da tốt\","
                            + "\"summaryText\":\"Kết quả soi da tại spa.\","
                            + "\"scanType\":\"Soi da offline\","
                            + "\"detailedEvaluation\":{"
                            + "\"moisture\":{\"score\":72,\"level\":\"Trung bình\",\"description\":\"Da cần bổ sung thêm độ ẩm.\"},"
                            + "\"oil\":{\"score\":81,\"level\":\"Tốt\",\"description\":\"Tuyến dầu hoạt động ổn định.\"},"
                            + "\"pores\":{\"score\":85,\"level\":\"Khá\",\"description\":\"Lỗ chân lông thông thoáng.\"},"
                            + "\"pigmentation\":{\"score\":70,\"level\":\"Trung bình\",\"description\":\"Sắc tố da tương đối đều.\"},"
                            + "\"sensitivity\":{\"score\":75,\"level\":\"Khá\",\"description\":\"Da ít kích ứng.\"}"
                            + "},"
                            + "\"suggestions\":[\"Duy trì chăm sóc da đều đặn.\",\"Sử dụng kem chống nắng hằng ngày.\"]"
                            + "}"
            );
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static boolean isValidScanResult(JSONObject data) {
        return data != null
                && data.has("detailedEvaluation")
                && data.has("score")
                && data.has("summaryText");
    }

    private static String extractTime(String rawTime) {
        if (TextUtils.isEmpty(rawTime)) {
            return "";
        }
        String trimmed = rawTime.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            trimmed = trimmed.substring(0, spaceIndex);
        }
        return trimmed.trim();
    }

    private static String normalizeDate(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return "";
        }
        return rawDate.trim();
    }

    static java.util.Map<String, Object> toFirestoreMap(JSONObject jsonObject) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        if (jsonObject == null) {
            return map;
        }
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.opt(key);
            if (value instanceof JSONObject) {
                map.put(key, toFirestoreMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.put(key, toFirestoreList((JSONArray) value));
            } else if (value != null && !JSONObject.NULL.equals(value)) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static java.util.List<Object> toFirestoreList(JSONArray array) {
        java.util.List<Object> list = new java.util.ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                list.add(toFirestoreMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                list.add(toFirestoreList((JSONArray) value));
            } else if (value != null && !JSONObject.NULL.equals(value)) {
                list.add(value);
            }
        }
        return list;
    }
}
