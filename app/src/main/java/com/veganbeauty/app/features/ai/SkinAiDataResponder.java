package com.veganbeauty.app.features.ai;

import android.content.Context;

import com.veganbeauty.app.features.weather.SkinWeatherProductMatcher;
import com.veganbeauty.app.features.weather.SkinWeatherProfileHelper;

import java.util.Locale;
import java.util.Map;

/**
 * Trả lời chat bằng cách đọc đúng trường dữ liệu trong {@link SkinAiAppDataSnapshot}.
 * Không dùng câu mẫu chung — mỗi intent map tới field cụ thể.
 */
public final class SkinAiDataResponder {

    private SkinAiDataResponder() {
    }

    public static String respond(
            SkinAiAppDataSnapshot d,
            SkinAiIntentHelper.Intent intent,
            Context context,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            SkinAiAssistantHelper.WeatherContext weather,
            String userMessage) {

        switch (intent) {
            case GREETING:
                return greeting(d);
            case COINS:
                return coins(d);
            case VOUCHER:
                return vouchers(d);
            case DIAGNOSTIC:
                return diagnostic(d, metrics);
            case BOOKING:
                return bookings(d);
            case ORDER_CART:
                return ordersAndCart(d);
            case ROUTINE:
                return routine(d, context, profile);
            case INGREDIENT:
                return ingredients(d);
            case PROFILE:
                return profile(d, metrics);
            case GIFT:
                return gifts(d);
            case STORE:
                return stores(d);
            case NOTIFICATION:
                return notifications(d);
            case HOW_TO:
                return howTo(d, userMessage);
            case PRODUCT:
                return products(context, d, profile, metrics, weather);
            case WEATHER:
                return weather(d, profile, metrics, weather);
            case SKIN_GENERAL:
                return profile(d, metrics);
            case UNKNOWN:
            default:
                return fallback(d);
        }
    }

    private static String greeting(SkinAiAppDataSnapshot d) {
        String who = d.userName == null || d.userName.trim().isEmpty() ? "bạn" : d.userName;
        if (!d.hasSkinProfile) {
            return "Chào " + who + "! Mình là Rootie AI. Bạn chưa làm quiz da — vào My Skin → Bài test da nhé.";
        }
        return "Chào " + who + "! "
                + field("loại da", d.skinType)
                + ", " + fieldPct("cấp ẩm", d.hydration)
                + ", " + fieldPct("nhạy cảm", d.sensitivity) + ". "
                + "Bạn hỏi: xu (" + d.totalCoins + "), voucher, đơn hàng, routine, sản phẩm, thời tiết.";
    }

    private static String coins(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        if (d.coinsEarnedToday <= 0) {
            sb.append("Hôm nay bạn chưa nhận thêm xu. ");
        } else {
            sb.append("Hôm nay bạn nhận ").append(fmt(d.coinsEarnedToday)).append(" xu. ");
            int limit = Math.min(3, d.todayCoins.size());
            for (int i = 0; i < limit; i++) {
                SkinAiAppDataSnapshot.CoinEntry c = d.todayCoins.get(i);
                sb.append("+").append(fmt(c.points)).append(" xu — ")
                        .append(SkinAiTextHelper.sanitize(c.reason)).append(". ");
            }
        }
        sb.append("Tổng số dư: ").append(fmt(d.totalCoins)).append(" xu.");
        return sb.toString().trim();
    }

    private static String vouchers(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        if (d.userVouchers.isEmpty()) {
            sb.append("Ví voucher: chưa có mã còn hạn. ");
        } else {
            sb.append("Voucher trong ví:\n");
            for (SkinAiAppDataSnapshot.VoucherEntry v : d.userVouchers) {
                sb.append("• ").append(v.title)
                        .append(" | mã ").append(v.code)
                        .append(" | ").append(v.status)
                        .append(" | HSD ").append(v.expiry)
                        .append(" | ").append(v.discountLabel).append('\n');
            }
        }
        if (!d.systemVouchers.isEmpty()) {
            sb.append("Khuyến mãi hệ thống: ");
            for (int i = 0; i < Math.min(4, d.systemVouchers.size()); i++) {
                SkinAiAppDataSnapshot.VoucherEntry v = d.systemVouchers.get(i);
                if (i > 0) sb.append("; ");
                sb.append(v.title).append(" (mã ").append(v.code).append(")");
            }
            sb.append(". ");
        }
        sb.append("Chi tiết: Tài khoản → Voucher.");
        return sb.toString().trim();
    }

    private static String diagnostic(SkinAiAppDataSnapshot d, SkinWeatherProfileHelper.TodaySkinMetrics metrics) {
        if (!d.hasSkinProfile) {
            return "Chưa có hồ sơ da — làm quiz trước để có phác đồ.";
        }
        return d.skinType + " — "
                + fieldPct("cấp ẩm", metrics.hydrationPercent) + ", "
                + fieldPct("bã nhờn", metrics.oilyPercent) + ", "
                + fieldPct("nhạy cảm", metrics.sensitivityPercent)
                + ". Mình gửi phác đồ chi tiết bên dưới.";
    }

    private static String profile(SkinAiAppDataSnapshot d, SkinWeatherProfileHelper.TodaySkinMetrics metrics) {
        if (!d.hasSkinProfile) {
            return "Chưa có dữ liệu quiz. My Skin → Bài test da.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(field("Loại da", d.skinType)).append(". ");
        sb.append(fieldPct("Cấp ẩm (quiz)", d.hydration)).append(", ");
        sb.append(fieldPct("bã nhờn", d.sebum)).append(", ");
        sb.append(fieldPct("nhạy cảm", d.sensitivity)).append(", ");
        sb.append(fieldPct("đàn hồi", d.elasticity)).append(". ");
        sb.append("Ước tính hôm nay: ẩm ").append(metrics.hydrationPercent)
                .append("%, dầu ").append(metrics.oilyPercent)
                .append("%, nhạy cảm ").append(metrics.sensitivityPercent).append("%. ");
        if (d.recommendation != null && !d.recommendation.isEmpty()) {
            sb.append(SkinAiTextHelper.sanitize(d.recommendation));
        }
        return sb.toString().trim();
    }

    private static String ingredients(SkinAiAppDataSnapshot d) {
        if (!d.hasSkinProfile) {
            return "Làm quiz da để biết thành phần cần tránh.";
        }
        String avoid = d.flaggedGroups == null || d.flaggedGroups.isEmpty()
                ? "cồn, hương liệu, sulfate (gợi ý chung cho da nhạy cảm)"
                : String.join(", ", d.flaggedGroups);
        return "Với " + d.skinType + ", thành phần cần tránh/theo quiz: " + avoid + ".";
    }

    private static String bookings(SkinAiAppDataSnapshot d) {
        if (d.bookings.isEmpty()) {
            return "Chưa có lịch soi da. My Skin → Đặt lịch.";
        }
        SkinAiAppDataSnapshot.BookingEntry b = d.bookings.get(0);
        return "Lịch gần nhất: " + b.service + " | " + b.status
                + " | " + b.store + " | " + b.date + ". Xem My Skin → Lịch sử đặt lịch.";
    }

    private static String ordersAndCart(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        if (d.cartItems.isEmpty()) {
            sb.append("Giỏ hàng: trống. ");
        } else {
            sb.append("Giỏ hàng: ");
            for (int i = 0; i < d.cartItems.size(); i++) {
                if (i > 0) sb.append(", ");
                SkinAiAppDataSnapshot.CartEntry c = d.cartItems.get(i);
                sb.append(c.name).append(" x").append(c.quantity);
            }
            sb.append(". ");
        }
        if (!d.orders.isEmpty()) {
            SkinAiAppDataSnapshot.OrderEntry o = d.orders.get(0);
            sb.append("Đơn mới nhất: ").append(o.id)
                    .append(" | ").append(o.status)
                    .append(" | ").append(fmtVnd(o.totalAmount))
                    .append(" | ").append(o.orderDate).append(". ");
        }
        sb.append("Chi tiết: Cửa hàng → Đơn hàng của tôi.");
        return sb.toString().trim();
    }

    private static String routine(
            SkinAiAppDataSnapshot d, Context context, SkinWeatherProfileHelper.UserSkinProfile profile) {
        if (d.hasSkinProfile && (!d.routineMorning.isEmpty() || !d.routineEvening.isEmpty())) {
            String saved = "";
            if (!"chưa bật bước nào".equals(d.routineMorning)) {
                saved += "Routine đang lưu — Sáng: " + d.routineMorning + ". ";
            }
            if (!"chưa bật bước nào".equals(d.routineEvening)) {
                saved += "Tối: " + d.routineEvening + ". ";
            }
            if (!saved.isEmpty()) {
                return saved.trim();
            }
        }
        try {
            SkinAiRoutineRecommender.RoutinePlan plan = SkinAiRoutineRecommender.recommend(
                    context,
                    d.hasSkinProfile ? d.skinType : "Da thường",
                    d.hydration, d.sebum, d.sensitivity, d.elasticity,
                    d.flaggedGroups, "");
            return "Gợi ý theo " + (d.hasSkinProfile ? d.skinType : "da bạn") + ":\n"
                    + "Sáng: " + SkinAiRoutineRecommender.formatRoutineForChat(plan, true) + "\n"
                    + "Tối: " + SkinAiRoutineRecommender.formatRoutineForChat(plan, false);
        } catch (Exception e) {
            return "Routine sáng: " + d.routineMorning + ". Tối: " + d.routineEvening + ".";
        }
    }

    private static String gifts(SkinAiAppDataSnapshot d) {
        if (d.gifts.isEmpty()) {
            return "Chưa có quà trong ví. Tài khoản → Đổi quà (số dư " + fmt(d.totalCoins) + " xu).";
        }
        StringBuilder sb = new StringBuilder("Quà / voucher đã nhận:\n");
        for (int i = 0; i < Math.min(5, d.gifts.size()); i++) {
            SkinAiAppDataSnapshot.GiftEntry g = d.gifts.get(i);
            sb.append("• ").append(g.title).append(" (").append(g.status).append(")\n");
        }
        return sb.toString().trim();
    }

    private static String stores(SkinAiAppDataSnapshot d) {
        if (d.stores.isEmpty()) {
            return "Xem cửa hàng tại Cửa hàng → Hệ thống cửa hàng.";
        }
        StringBuilder sb = new StringBuilder("Cửa hàng Rootie:\n");
        for (SkinAiAppDataSnapshot.StoreEntry s : d.stores) {
            sb.append("• ").append(s.name).append(" — ").append(s.address)
                    .append(" (").append(s.hours).append(") ").append(s.phone).append('\n');
        }
        return sb.toString().trim();
    }

    private static String notifications(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        sb.append("Thông báo chưa đọc: ").append(d.notificationUnread).append(". ");
        if (!d.notificationTitles.isEmpty()) {
            sb.append("Gần đây: ");
            for (int i = 0; i < d.notificationTitles.size(); i++) {
                if (i > 0) sb.append("; ");
                sb.append(d.notificationTitles.get(i));
            }
            sb.append(". ");
        }
        sb.append("Mở chuông góc phải để xem đầy đủ.");
        return sb.toString().trim();
    }

    private static String howTo(SkinAiAppDataSnapshot d, String userMessage) {
        String lower = userMessage.toLowerCase(Locale.getDefault());
        if (lower.contains("quiz") || lower.contains("test")) {
            return "My Skin → Bài test da → làm quiz → xem loại da + routine gợi ý.";
        }
        if (lower.contains("đổi quà") || lower.contains("xu")) {
            return "Tài khoản → Đổi quà. Số dư hiện tại: " + fmt(d.totalCoins) + " xu.";
        }
        if (lower.contains("voucher")) {
            return "Tài khoản → Voucher. Bạn có " + d.userVouchers.size() + " mã trong ví.";
        }
        return SkinAiTrainingExamples.getAppNavigationMap().replace("\n", " ");
    }

    private static String products(
            Context context,
            SkinAiAppDataSnapshot d,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            SkinAiAssistantHelper.WeatherContext weather) {
        if (!d.hasSkinProfile) {
            return "Làm quiz da trước để gợi ý sản phẩm phù hợp.";
        }
        if (weather.available) {
            Map<String, SkinWeatherProductMatcher.ProductMatch> matches =
                    SkinWeatherProductMatcher.matchProductsForWeatherAndSkin(
                            context, weather.temp, weather.humidity, profile.skinType, profile.flaggedGroups);
            if (!matches.isEmpty()) {
                StringBuilder sb = new StringBuilder("Với " + d.skinType + " + thời tiết hiện tại, gợi ý: ");
                boolean first = true;
                for (SkinWeatherProductMatcher.ProductMatch m : matches.values()) {
                    if (!first) sb.append("; ");
                    sb.append(m.getName());
                    first = false;
                }
                sb.append(". Tránh: ").append(d.flaggedGroups.isEmpty() ? "cồn, hương liệu" : String.join(", ", d.flaggedGroups));
                return sb.toString();
            }
        }
        if (!d.productsInUse.isEmpty()) {
            StringBuilder sb = new StringBuilder("Bạn đang dùng: ");
            for (int i = 0; i < d.productsInUse.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(d.productsInUse.get(i).name);
            }
            sb.append(". Với ").append(d.skinType).append(", ưu tiên làm sạch dịu + cấp ẩm + chống nắng.");
            return sb.toString();
        }
        return "Với " + d.skinType + " (ẩm " + metrics.hydrationPercent + "%, nhạy cảm "
                + metrics.sensitivityPercent + "%), ưu tiên sản phẩm dịu, cấp ẩm, chống nắng.";
    }

    private static String weather(
            SkinAiAppDataSnapshot d,
            SkinWeatherProfileHelper.UserSkinProfile profile,
            SkinWeatherProfileHelper.TodaySkinMetrics metrics,
            SkinAiAssistantHelper.WeatherContext weather) {
        if (!d.hasSkinProfile) {
            return "Làm quiz da trước. Sau đó mở Da × Thời tiết để cập nhật.";
        }
        if (!weather.available) {
            return "Chưa có dữ liệu thời tiết. Mở Da × Thời tiết rồi hỏi lại.";
        }
        SkinWeatherProfileHelper.PersonalizedAdvice advice = SkinWeatherProfileHelper.buildRuleBasedAdvice(
                profile, metrics,
                weather.temp, weather.humidity, weather.uv,
                (int) Math.round(weather.pm25), weather.hasPm25,
                d.weatherCity.isEmpty() ? "khu vực bạn" : d.weatherCity);
        return d.weatherCity + ": " + (int) d.weatherTemp + "°C, độ ẩm " + d.weatherHumidity
                + "%, UV " + String.format(Locale.US, "%.1f", d.weatherUv) + ". "
                + SkinAiTextHelper.sanitize(advice.headline) + " "
                + SkinAiTextHelper.sanitize(advice.subtext);
    }

    private static String fallback(SkinAiAppDataSnapshot d) {
        return "Mình chưa hiểu rõ câu hỏi. Dữ liệu của bạn hiện có: "
                + fmt(d.totalCoins) + " xu, "
                + (d.hasSkinProfile ? d.skinType : "chưa quiz da") + ", "
                + d.orders.size() + " đơn, "
                + d.userVouchers.size() + " voucher, "
                + d.cartItems.size() + " SP trong giỏ. "
                + "Thử hỏi: xu hôm nay, voucher, loại da, routine, đơn hàng.";
    }

    private static String field(String label, String value) {
        return label + ": " + (value == null || value.isEmpty() ? "chưa có" : value);
    }

    private static String fieldPct(String label, int value) {
        return label + " " + value + "%";
    }

    private static String fmt(int n) {
        return String.format(Locale.getDefault(), "%,d", n).replace(',', '.');
    }

    private static String fmtVnd(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount).replace(',', '.');
    }
}
