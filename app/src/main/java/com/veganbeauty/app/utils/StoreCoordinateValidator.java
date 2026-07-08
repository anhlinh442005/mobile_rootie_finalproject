package com.veganbeauty.app.utils;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.entities.StoreEntity;

/**
 * Kiểm tra tọa độ cửa hàng có khớp tỉnh/thành trên địa chỉ hay không.
 */
public final class StoreCoordinateValidator {

    private StoreCoordinateValidator() {
    }

    public static boolean isPlausible(@Nullable StoreEntity store) {
        if (store == null) {
            return false;
        }
        String province = store.getTinhThanh();
        if (province == null || province.trim().isEmpty()) {
            return true;
        }
        return isPlausible(province.trim(), store.getLat(), store.getLng());
    }

    public static boolean isPlausible(String province, double lat, double lng) {
        if (lat == 0d && lng == 0d) {
            return false;
        }
        switch (province) {
            case "Hồ Chí Minh":
                return inRange(lat, lng, 10.35, 11.05, 106.30, 107.05);
            case "Cần Thơ":
                return inRange(lat, lng, 9.80, 10.40, 105.40, 106.10);
            case "Tiền Giang":
                return inRange(lat, lng, 10.00, 10.60, 106.00, 106.85);
            case "Bà Rịa - Vũng Tàu":
                return inRange(lat, lng, 10.20, 10.80, 106.90, 107.50);
            case "Đồng Nai":
            case "Biên Hòa":
                return inRange(lat, lng, 10.70, 11.20, 106.80, 107.40);
            case "Bình Dương":
                return inRange(lat, lng, 10.80, 11.40, 106.40, 107.00);
            case "Long An":
                return inRange(lat, lng, 10.30, 10.90, 105.90, 106.70);
            case "An Giang":
                return inRange(lat, lng, 10.00, 11.20, 104.50, 105.60);
            case "Nam Định":
                return inRange(lat, lng, 19.80, 20.70, 105.90, 106.50);
            case "Hà Nội":
                return inRange(lat, lng, 20.90, 21.20, 105.70, 106.00);
            case "Đà Nẵng":
                return inRange(lat, lng, 15.90, 16.20, 107.90, 108.40);
            default:
                return lat >= 8.0 && lat <= 23.5 && lng >= 102.0 && lng <= 110.0;
        }
    }

    private static boolean inRange(double lat, double lng,
                                   double minLat, double maxLat,
                                   double minLng, double maxLng) {
        return lat >= minLat && lat <= maxLat && lng >= minLng && lng <= maxLng;
    }
}
