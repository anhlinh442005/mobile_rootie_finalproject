package com.veganbeauty.app.features.ai;

import java.util.Locale;

/** In xuất snapshot ra text — dùng cho ngữ cảnh Gemini hoặc debug. */
public final class SkinAiSnapshotFormatter {

    private SkinAiSnapshotFormatter() {
    }

    public static String toCompact(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        sb.append("userName=").append(d.userName).append('\n');
        sb.append("totalCoins=").append(d.totalCoins)
                .append(" | coinsEarnedToday=").append(d.coinsEarnedToday).append('\n');
        if (d.hasSkinProfile) {
            sb.append("skinType=").append(d.skinType)
                    .append(" | hydration=").append(d.hydration)
                    .append(" | sebum=").append(d.sebum)
                    .append(" | sensitivity=").append(d.sensitivity).append('\n');
        }
        if (d.weatherOk) {
            sb.append("weather=").append(d.weatherCity).append(" ")
                    .append((int) d.weatherTemp).append("C UV=").append(d.weatherUv).append('\n');
        }
        sb.append("vouchers=").append(d.userVouchers.size())
                .append(" | orders=").append(d.orders.size())
                .append(" | cart=").append(d.cartItems.size()).append('\n');
        return sb.toString();
    }

    public static String toFullContext(SkinAiAppDataSnapshot d) {
        StringBuilder sb = new StringBuilder();
        sb.append(SkinAiFieldCatalog.describe()).append('\n');
        sb.append("=== DỮ LIỆU USER HIỆN TẠI ===\n");
        sb.append("Tài khoản: ").append(d.userName).append(" | ").append(d.email)
                .append(" | xu=").append(d.totalCoins).append('\n');

        if (d.hasSkinProfile) {
            sb.append("Da: ").append(d.skinType)
                    .append(" | ẩm=").append(d.hydration).append("%")
                    .append(" | dầu=").append(d.sebum).append("%")
                    .append(" | nhạy cảm=").append(d.sensitivity).append("%\n");
            if (d.flaggedGroups != null && !d.flaggedGroups.isEmpty()) {
                sb.append("Tránh: ").append(String.join(", ", d.flaggedGroups)).append('\n');
            }
        }

        sb.append("Xu hôm nay: ").append(d.coinsEarnedToday).append('\n');
        for (SkinAiAppDataSnapshot.CoinEntry c : d.todayCoins) {
            sb.append("  +").append(c.points).append(" ").append(c.reason).append('\n');
        }

        for (SkinAiAppDataSnapshot.VoucherEntry v : d.userVouchers) {
            sb.append("Voucher: ").append(v.title).append(" | ").append(v.code)
                    .append(" | ").append(v.status).append('\n');
        }
        for (SkinAiAppDataSnapshot.OrderEntry o : d.orders) {
            sb.append("Đơn: ").append(o.id).append(" | ").append(o.status)
                    .append(" | ").append(fmtVnd(o.totalAmount)).append('\n');
        }
        for (SkinAiAppDataSnapshot.CartEntry c : d.cartItems) {
            sb.append("Giỏ: ").append(c.name).append(" x").append(c.quantity).append('\n');
        }

        if (d.weatherOk) {
            sb.append("Thời tiết: ").append(d.weatherCity).append(" ")
                    .append((int) d.weatherTemp).append("C, ẩm ")
                    .append(d.weatherHumidity).append("%, UV ")
                    .append(String.format(Locale.US, "%.1f", d.weatherUv)).append('\n');
        }

        sb.append("Routine sáng: ").append(d.routineMorning).append('\n');
        sb.append("Routine tối: ").append(d.routineEvening).append('\n');
        return sb.toString();
    }

    private static String fmtVnd(long amount) {
        return String.format(Locale.getDefault(), "%,dđ", amount).replace(',', '.');
    }
}
