package com.veganbeauty.app.features.ai;

/** Bản đồ trường dữ liệu app — Rootie “học” các field này để trả lời. */
public final class SkinAiFieldCatalog {

    private SkinAiFieldCatalog() {
    }

    public static String describe() {
        return "CÁC TRƯỜNG DỮ LIỆU ROOTIE (AI chỉ trả lời từ đây):\n"
                + "| Chủ đề | Trường dữ liệu | Nguồn |\n"
                + "| Xu | totalCoins, coinsEarnedToday, todayCoins[].points/reason | user_coin |\n"
                + "| Hồ sơ da | skinType, hydration, sebum, sensitivity, elasticity, recommendation, flaggedGroups | RootieQuizPrefs |\n"
                + "| Voucher ví | userVouchers[].title, code, status, expiry | user_gifts |\n"
                + "| Khuyến mãi | systemVouchers[].title, code, expiry | vouchers |\n"
                + "| Đơn hàng | orders[].id, status, totalAmount, orderDate | orders |\n"
                + "| Giỏ hàng | cartItems[].name, quantity | cart_items |\n"
                + "| Routine | routineMorning, routineEvening | ProfileSession |\n"
                + "| Thời tiết | weatherCity, weatherTemp, weatherHumidity, weatherUv | SkinWeatherSnapshot |\n"
                + "| Đặt lịch | bookings[].service, status, store, date | booking JSON |\n"
                + "| SP đang dùng | productsInUse[].name, expiry | user_product_expiry |\n"
                + "| Quà | gifts[].title, status, type | user_gifts |\n"
                + "| Cửa hàng | stores[].name, address, hours, phone | rootie_stores.json |\n"
                + "| Thông báo | notificationUnread, notificationTitles[] | notification_account.json |\n";
    }
}
