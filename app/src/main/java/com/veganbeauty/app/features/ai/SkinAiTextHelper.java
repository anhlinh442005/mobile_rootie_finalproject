package com.veganbeauty.app.features.ai;

/** Chuẩn hóa văn bản AI hiển thị — tránh lỗi font / ký tự lạ. */
public final class SkinAiTextHelper {

    private SkinAiTextHelper() {
    }

    public static String sanitize(String raw) {
        if (raw == null) return "";
        String text = raw.trim();
        if (text.isEmpty()) return "";

        // Sửa lỗi hay gặp khi recommendation bị corrupt
        text = text.replace("hàna naàv", "hàng ngày")
                .replace("hàna", "hàng")
                .replace("naàv", "ngày");

        // Bỏ ký tự điều khiển / surrogate hỏng
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        return text;
    }

    public static String sanitizeAiReply(String raw) {
        String text = sanitize(raw);
        if (text.isEmpty()) return text;

        // Cắt quá dài cho bubble chat
        if (text.length() > 900) {
            text = text.substring(0, 897) + "...";
        }
        return text;
    }
}
