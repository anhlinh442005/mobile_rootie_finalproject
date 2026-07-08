package com.veganbeauty.app.features.ai;

import java.util.Locale;

/** Nhận diện ý định câu hỏi — dùng cho rule-based và gợi ý Gemini. */
public final class SkinAiIntentHelper {

    public enum Intent {
        GREETING,
        COINS,
        VOUCHER,
        DIAGNOSTIC,
        BOOKING,
        ORDER_CART,
        ROUTINE,
        PRODUCT,
        WEATHER,
        INGREDIENT,
        PROFILE,
        GIFT,
        STORE,
        NOTIFICATION,
        HOW_TO,
        SKIN_GENERAL,
        UNKNOWN
    }

    private SkinAiIntentHelper() {
    }

    public static Intent detect(String userMessage) {
        if (userMessage == null) return Intent.UNKNOWN;
        String lower = userMessage.toLowerCase(Locale.getDefault()).trim();
        if (lower.isEmpty()) return Intent.UNKNOWN;

        if (isGreeting(lower)) return Intent.GREETING;
        if (isCoinsQuestion(lower)) return Intent.COINS;
        if (isVoucherQuestion(lower)) return Intent.VOUCHER;
        if (isDiagnosticRequest(lower)) return Intent.DIAGNOSTIC;
        if (isBookingQuestion(lower)) return Intent.BOOKING;
        if (isOrderOrCartQuestion(lower)) return Intent.ORDER_CART;
        if (isRoutineQuestion(lower)) return Intent.ROUTINE;
        if (isIngredientQuestion(lower)) return Intent.INGREDIENT;
        if (isProfileQuestion(lower)) return Intent.PROFILE;
        if (isGiftQuestion(lower)) return Intent.GIFT;
        if (isStoreQuestion(lower)) return Intent.STORE;
        if (isNotificationQuestion(lower)) return Intent.NOTIFICATION;
        if (isHowToQuestion(lower)) return Intent.HOW_TO;
        if (isProductQuestion(lower)) return Intent.PRODUCT;
        if (isWeatherQuestion(lower)) return Intent.WEATHER;
        if (looksLikeSkinQuestion(lower)) return Intent.SKIN_GENERAL;
        return Intent.UNKNOWN;
    }

    /** Câu hỏi có thể trả lời ngay từ dữ liệu app — không cần gọi Gemini. */
    public static boolean usesAppData(Intent intent) {
        return intent != Intent.UNKNOWN;
    }

    public static String intentHint(Intent intent) {
        switch (intent) {
            case COINS: return "Trả lời về XU/điểm thưởng từ mục XU.";
            case VOUCHER: return "Trả lời về VOUCHER/khuyến mãi — KHÔNG nói routine da.";
            case DIAGNOSTIC: return "Mô tả phác đồ/chẩn đoán da từ hồ sơ quiz.";
            case BOOKING: return "Trả lời lịch đặt soi da/spa.";
            case ORDER_CART: return "Trả lời đơn hàng và giỏ hàng.";
            case ROUTINE: return "Liệt kê routine sáng/tối từ ROUTINE GỢI Ý hoặc ROUTINE CHĂM DA.";
            case PRODUCT: return "Gợi ý sản phẩm phù hợp loại da + thành phần tránh.";
            case WEATHER: return "Trả lời thời tiết/UV và ảnh hưởng da.";
            case INGREDIENT: return "Trả lời thành phần nên tránh/dùng từ hồ sơ quiz.";
            case PROFILE: return "Trả lời loại da và chỉ số từ HỒ SƠ DA.";
            case GIFT: return "Trả lời quà đã đổi từ mục QUÀ CỦA USER.";
            case STORE: return "Trả lời cửa hàng/spa từ mục CỬA HÀNG ROOTIE.";
            case NOTIFICATION: return "Trả lời thông báo từ mục THÔNG BÁO.";
            case HOW_TO: return "Hướng dẫn thao tác trong app theo BẢN ĐỒ APP.";
            case SKIN_GENERAL: return "Tư vấn chăm sóc da theo hồ sơ.";
            default: return "Trả lời đúng chủ đề câu hỏi từ ngữ cảnh; nếu không chắc, gợi ý chủ đề có thể hỏi.";
        }
    }

    private static boolean isGreeting(String lower) {
        return lower.equals("hello") || lower.equals("hi") || lower.equals("chào")
                || lower.startsWith("chào ") || lower.contains("xin chào")
                || lower.equals("hey") || lower.equals("yo");
    }

    private static boolean isCoinsQuestion(String lower) {
        return lower.contains("xu") || lower.contains("coin") || lower.contains("điểm thưởng")
                || lower.contains("tích điểm") || lower.contains("điểm danh")
                || lower.contains("bao nhiêu xu") || lower.contains("mấy xu")
                || (lower.contains("cộng") && lower.contains("xu"))
                || (lower.contains("nhận") && lower.contains("xu"));
    }

    private static boolean isVoucherQuestion(String lower) {
        return lower.contains("voucher") || lower.contains("mã giảm") || lower.contains("coupon")
                || lower.contains("khuyến mãi") || lower.contains("freeship")
                || lower.contains("free ship") || lower.contains("mã giảm giá")
                || (lower.contains("giảm giá") && (lower.contains("đơn") || lower.contains("mã")))
                || (lower.contains("đang có") && (lower.contains("mã") || lower.contains("giảm") || lower.contains("voucher")))
                || (lower.contains("có gì") && (lower.contains("voucher") || lower.contains("mã") || lower.contains("khuyến mãi")));
    }

    private static boolean isDiagnosticRequest(String lower) {
        return lower.contains("phác đồ") || lower.contains("chẩn đoán")
                || lower.contains("phân tích da") || lower.contains("bản phân tích");
    }

    private static boolean isBookingQuestion(String lower) {
        return lower.contains("đặt lịch") || lower.contains("booking")
                || lower.contains("lịch soi") || lower.contains("spa")
                || (lower.contains("lịch") && !lower.contains("lịch sử") && !lower.contains("quiz"));
    }

    private static boolean isOrderOrCartQuestion(String lower) {
        return lower.contains("đơn hàng") || lower.contains("giỏ hàng") || lower.contains("giỏ ")
                || lower.contains("order") || lower.contains("mua hàng")
                || (lower.contains("đơn") && !lower.contains("điểm danh"));
    }

    private static boolean isRoutineQuestion(String lower) {
        return lower.contains("routine") || lower.contains("chu trình")
                || lower.contains("routine nào") || lower.contains("nên xài")
                || lower.contains("nên dùng gì") || lower.contains("skincare")
                || (lower.contains("sáng") && (lower.contains("bước") || lower.contains("làm gì") || lower.contains("dưỡng")))
                || (lower.contains("tối") && (lower.contains("bước") || lower.contains("làm gì") || lower.contains("dưỡng")));
    }

    private static boolean isIngredientQuestion(String lower) {
        return lower.contains("thành phần") || lower.contains("ingredient")
                || lower.contains("tránh gì") || lower.contains("dị ứng")
                || lower.contains("cồn") || lower.contains("retinol") || lower.contains("bha")
                || lower.contains("hương liệu") || lower.contains("không nên dùng");
    }

    private static boolean isProfileQuestion(String lower) {
        return lower.contains("loại da") || lower.contains("hồ sơ da")
                || lower.contains("da tôi") || lower.contains("da mình")
                || lower.contains("chỉ số da") || lower.contains("kết quả quiz")
                || lower.contains("tôi là da") || lower.contains("mình là da")
                || (lower.contains("da gì") && !lower.contains("mua"));
    }

    private static boolean isGiftQuestion(String lower) {
        if (isVoucherQuestion(lower)) return false;
        return lower.contains("đổi quà") || lower.contains("quà tặng")
                || lower.contains("quà của tôi") || lower.contains("quà đã")
                || (lower.contains("quà") && lower.contains("nhận"));
    }

    private static boolean isStoreQuestion(String lower) {
        return lower.contains("cửa hàng") || lower.contains("spa rootie")
                || lower.contains("địa chỉ") || lower.contains("ở đâu")
                || lower.contains("chi nhánh") || lower.contains("store");
    }

    private static boolean isNotificationQuestion(String lower) {
        return lower.contains("thông báo") || lower.contains("notification")
                || lower.contains("tin mới") || lower.contains("có gì mới");
    }

    private static boolean isHowToQuestion(String lower) {
        return lower.startsWith("làm sao") || lower.startsWith("làm thế nào")
                || lower.contains("hướng dẫn") || lower.contains("vào đâu")
                || lower.contains("ở tab nào") || lower.contains("tìm ở đâu");
    }

    private static boolean isProductQuestion(String lower) {
        return lower.contains("sản phẩm") || lower.contains("phù hợp") || lower.contains("mua gì")
                || lower.contains("dùng gì") || (lower.contains("kem") && lower.contains("nào"))
                || lower.contains("gợi ý mua");
    }

    private static boolean isWeatherQuestion(String lower) {
        if (isCoinsQuestion(lower) || isVoucherQuestion(lower)) return false;
        return lower.contains("thời tiết") || lower.contains(" uv")
                || lower.startsWith("uv") || lower.contains("nắng")
                || lower.contains("độ ẩm không khí") || lower.contains("pm2.5")
                || (lower.contains("hôm nay") && (lower.contains("da") || lower.contains("dưỡng")
                || lower.contains("thời tiết") || lower.contains("nắng")))
                || lower.contains("thời tiết & da") || lower.contains("thời tiết và da");
    }

    private static boolean looksLikeSkinQuestion(String lower) {
        return lower.contains("da ") || lower.startsWith("da")
                || lower.contains(" mụn") || lower.contains("spf")
                || lower.contains("serum") || lower.contains("kem dưỡng")
                || lower.contains("chống nắng") || lower.contains("nhạy cảm")
                || lower.contains("cấp ẩm") || lower.contains("bã nhờn")
                || lower.contains("dưỡng da") || lower.contains("làm đẹp");
    }
}
