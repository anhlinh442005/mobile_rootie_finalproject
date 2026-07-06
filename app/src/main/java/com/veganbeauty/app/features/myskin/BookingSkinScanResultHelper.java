package com.veganbeauty.app.features.myskin;

import android.content.Context;
import android.text.TextUtils;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.SkinHistoryLocalStore;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

final class BookingSkinScanResultHelper {

    private BookingSkinScanResultHelper() {
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
