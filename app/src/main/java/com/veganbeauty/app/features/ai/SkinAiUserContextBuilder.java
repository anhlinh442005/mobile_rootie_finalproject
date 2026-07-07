package com.veganbeauty.app.features.ai;

import android.content.Context;
import android.content.SharedPreferences;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.SkinHistoryLocalStore;
import com.veganbeauty.app.data.local.entities.BookingHistoryEntity;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.OrderEntity;
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;
import com.veganbeauty.app.features.weather.SkinWeatherSnapshotManager;
import com.veganbeauty.app.utils.RewardPointsHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Gom toàn bộ ngữ cảnh người dùng trong app để Rootie AI trả lời chính xác. */
public final class SkinAiUserContextBuilder {

    private SkinAiUserContextBuilder() {
    }

    public static String build(Context context) {
        StringBuilder sb = new StringBuilder();
        String userId = ProfileSession.getUserId(context);
        String email = ProfileSession.getEmail(context);
        String name = ProfileSession.getFullName(context);

        sb.append("=== TÀI KHOẢN ===\n");
        sb.append("- Tên: ").append(safe(name)).append('\n');
        sb.append("- Email: ").append(safe(email)).append('\n');
        sb.append("- Đã đăng nhập: ").append(ProfileSession.isLoggedIn(context) ? "có" : "không").append('\n');
        sb.append("- Xu thưởng: ").append(RewardPointsHelper.getTotalPoints(context)).append('\n');

        SkinWeatherProfileHelper.UserSkinProfile profile = SkinWeatherProfileHelper.load(context);
        sb.append("\n=== HỒ SƠ DA (quiz) ===\n");
        if (profile.hasSavedProfile) {
            sb.append("- Loại da: ").append(profile.skinType).append('\n');
            sb.append("- Cấp ẩm: ").append(profile.hydration).append("%\n");
            sb.append("- Bã nhờn: ").append(profile.sebum).append("%\n");
            sb.append("- Nhạy cảm: ").append(profile.sensitivity).append("%\n");
            sb.append("- Đàn hồi: ").append(profile.elasticity).append("%\n");
            if (!profile.recommendation.isEmpty()) {
                sb.append("- Khuyến nghị: ").append(SkinAiTextHelper.sanitize(profile.recommendation)).append('\n');
            }
            if (!profile.skinAreas.isEmpty()) {
                sb.append("- Vùng da: ").append(SkinAiTextHelper.sanitize(profile.skinAreas)).append('\n');
            }
            Set<String> flagged = profile.flaggedGroups;
            sb.append("- Thành phần tránh: ")
                    .append(flagged.isEmpty() ? "chưa ghi nhận" : String.join(", ", flagged))
                    .append('\n');
        } else {
            sb.append("- Chưa làm bài test da / chưa lưu hồ sơ.\n");
        }

        appendQuizHistory(sb, context);
        appendSkinScanHistory(sb, context, userId, email);

        SkinWeatherSnapshotManager.Snapshot weather = SkinWeatherSnapshotManager.loadLocal(context);
        sb.append("\n=== THỜI TIẾT GẦN NHẤT ===\n");
        if (weather != null && weather.weatherSuccess) {
            sb.append("- Thành phố: ").append(weather.city).append('\n');
            sb.append("- Nhiệt độ: ").append(String.format(Locale.US, "%.0f", weather.temp)).append("°C\n");
            sb.append("- Độ ẩm: ").append(weather.humidity).append("%\n");
            sb.append("- UV: ").append(String.format(Locale.US, "%.1f", weather.uv)).append('\n');
            if (weather.hasPm25) {
                sb.append("- PM2.5: ").append((int) Math.round(weather.pm25)).append(" µg/m³\n");
            }
        } else {
            sb.append("- Chưa có — user cần mở mục Da × Thời tiết để cập nhật.\n");
        }

        appendProductsInUse(sb, context, userId);
        appendRoutine(sb, context);
        appendBookings(sb, context, userId, email);
        appendOrders(sb, context, userId);
        appendCoinsSummary(sb, context);
        appendCartSummary(sb, context);

        sb.append("\n=== HƯỚNG DẪN TRẢ LỜI ===\n");
        sb.append("- Trả lời tiếng Việt chuẩn, không lỗi chính tả, không ký tự lạ.\n");
        sb.append("- Dùng đúng số liệu ở trên; không bịa thông tin user.\n");
        sb.append("- Câu hỏi về XU: trả lời đúng số xu hôm nay và tổng số dư từ mục XU.\n");
        sb.append("- Câu hỏi PHÁC ĐỒ/CHẨN ĐOÁN: mô tả chỉ số da từ hồ sơ, không lặp lời khuyên thời tiết.\n");
        sb.append("- Nếu thiếu dữ liệu, nói rõ và hướng dẫn user làm quiz / mở Da×Thời tiết / xem đơn hàng.\n");
        sb.append("- Thuần chay, ngắn gọn 2-5 câu, thân thiện.\n");

        return sb.toString();
    }

    private static void appendQuizHistory(StringBuilder sb, Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
            String historyStr = prefs.getString("QUIZ_HISTORY_LIST", "[]");
            JSONArray arr = new JSONArray(historyStr != null ? historyStr : "[]");
            if (arr.length() == 0) return;
            sb.append("\n=== LỊCH SỬ QUIZ (").append(arr.length()).append(" lần) ===\n");
            int start = Math.max(0, arr.length() - 3);
            for (int i = arr.length() - 1; i >= start; i--) {
                JSONObject o = arr.getJSONObject(i);
                sb.append("- ").append(o.optString("date", ""))
                        .append(": ").append(o.optString("skinType", ""))
                        .append(" (ẩm ").append(o.optInt("hydration", 0)).append("%, ")
                        .append("dầu ").append(o.optInt("sebum", 0)).append("%, ")
                        .append("nhạy cảm ").append(o.optInt("sensitivity", 0)).append("%)\n");
            }
        } catch (Exception ignored) {
        }
    }

    private static void appendSkinScanHistory(StringBuilder sb, Context context, String userId, String email) {
        try {
            JSONArray history = SkinHistoryLocalStore.getHistory(context, userId, email);
            if (history.length() == 0) return;
            sb.append("\n=== LỊCH SỬ SOI DA / QUIZ LƯU TRỮ ===\n");
            int limit = Math.min(3, history.length());
            for (int i = 0; i < limit; i++) {
                JSONObject o = history.getJSONObject(i);
                sb.append("- ").append(o.optString("date", ""))
                        .append(" [").append(o.optString("scanType", "")).append("]: ");
                if (o.has("score")) {
                    sb.append("điểm ").append(o.optInt("score", 0)).append(' ');
                }
                sb.append(o.optString("skinType", o.optString("overallCondition", ""))).append('\n');
            }
        } catch (Exception ignored) {
        }
    }

    private static void appendProductsInUse(StringBuilder sb, Context context, String userId) {
        try {
            List<UserProductExpiryEntity> products = RootieDatabase.getDatabase(context)
                    .userProductExpiryDao()
                    .getProductsByUserId(userId);
            if (products == null || products.isEmpty()) return;
            sb.append("\n=== SẢN PHẨM ĐANG DÙNG ===\n");
            int limit = Math.min(6, products.size());
            for (int i = 0; i < limit; i++) {
                UserProductExpiryEntity p = products.get(i);
                sb.append("- ").append(p.getName())
                        .append(" (HSD: ").append(p.getExpiryDate()).append(")\n");
            }
        } catch (Exception ignored) {
        }
    }

    private static void appendBookings(StringBuilder sb, Context context, String userId, String email) {
        try {
            String key = email != null && !email.isEmpty() ? email : userId;
            List<BookingHistoryEntity> bookings = new LocalJsonReader(context).getUserBookingHistory(key);
            if (bookings == null || bookings.isEmpty()) return;
            sb.append("\n=== LỊCH ĐẶT SOI DA ===\n");
            int limit = Math.min(3, bookings.size());
            for (int i = 0; i < limit; i++) {
                BookingHistoryEntity b = bookings.get(i);
                sb.append("- ").append(b.getServiceName())
                        .append(" | ").append(b.getStatus())
                        .append(" | ").append(b.getStoreName())
                        .append(" | ").append(b.getDateDisplay()).append('\n');
            }
        } catch (Exception ignored) {
        }
    }

    private static void appendCoinsSummary(StringBuilder sb, Context context) {
        try {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();
            int today = RootieDatabase.getDatabase(context).rewardPointDao().getPointsEarnedSince(startOfDay);
            int total = RewardPointsHelper.getTotalPoints(context);
            sb.append("\n=== XU / ĐIỂM THƯỞNG ===\n");
            sb.append("- Tổng số dư: ").append(total).append(" xu\n");
            sb.append("- Nhận hôm nay: ").append(today).append(" xu\n");
        } catch (Exception ignored) {
        }
    }

    private static void appendRoutine(StringBuilder sb, Context context) {
        try {
            Set<String> morning = ProfileSession.getMorningSteps(context);
            Set<String> evening = ProfileSession.getEveningSteps(context);
            if (morning.isEmpty() && evening.isEmpty()) return;

            sb.append("\n=== ROUTINE CHĂM DA ===\n");
            if (!morning.isEmpty()) {
                sb.append("- Sáng: ").append(formatRoutineSteps(morning)).append('\n');
            }
            if (!evening.isEmpty()) {
                sb.append("- Tối: ").append(formatRoutineSteps(evening)).append('\n');
            }
            String morningTime = ProfileSession.getMorningReminderTime(context);
            String eveningTime = ProfileSession.getEveningReminderTime(context);
            sb.append("- Nhắc sáng: ").append(morningTime)
                    .append(" | Nhắc tối: ").append(eveningTime).append('\n');
        } catch (Exception ignored) {
        }
    }

    private static String formatRoutineSteps(Set<String> steps) {
        StringBuilder line = new StringBuilder();
        for (String step : steps) {
            String[] parts = step.split(":", 4);
            if (parts.length < 3) continue;
            boolean enabled = parts.length < 4 || "true".equalsIgnoreCase(parts[3]);
            if (!enabled) continue;
            if (line.length() > 0) line.append(" → ");
            line.append(parts[2]);
        }
        return line.length() > 0 ? line.toString() : "chưa bật bước nào";
    }

    private static void appendOrders(StringBuilder sb, Context context, String userId) {
        try {
            String phone = ProfileSession.getPhone(context);
            List<OrderEntity> orders = RootieDatabase.getDatabase(context)
                    .orderDao()
                    .getOrdersForBuyerIdentitySync(userId, phone != null ? phone : "");
            if (orders == null || orders.isEmpty()) return;

            sb.append("\n=== ĐƠN HÀNG GẦN ĐÂY ===\n");
            int limit = Math.min(3, orders.size());
            for (int i = 0; i < limit; i++) {
                OrderEntity o = orders.get(i);
                sb.append("- ").append(o.getId())
                        .append(" | ").append(o.getStatus())
                        .append(" | ").append(formatVnd(o.getTotalAmount()))
                        .append(" | ").append(safe(o.getOrderDate())).append('\n');
            }
        } catch (Exception ignored) {
        }
    }

    private static String formatVnd(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount).replace(',', '.');
    }

    private static void appendCartSummary(StringBuilder sb, Context context) {
        try {
            List<CartItemEntity> items = RootieDatabase.getDatabase(context).cartDao().getCartItemsSync();
            if (items == null || items.isEmpty()) return;
            sb.append("\n=== GIỎ HÀNG ===\n");
            for (CartItemEntity item : items) {
                sb.append("- ").append(item.getName())
                        .append(" x").append(item.getQuantity()).append('\n');
            }
        } catch (Exception ignored) {
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "chưa có" : value.trim();
    }
}
