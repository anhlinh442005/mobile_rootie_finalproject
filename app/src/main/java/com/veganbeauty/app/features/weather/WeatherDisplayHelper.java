package com.veganbeauty.app.features.weather;

import com.veganbeauty.app.R;

import java.util.Locale;

/**
 * Chuẩn hóa hiển thị dữ liệu thời tiết từ Open-Meteo.
 * PM2.5: nồng độ µg/m³ (microgram/m³), thang tham chiếu WHO / US EPA.
 * UV: thang chỉ số UV quốc tế (WHO).
 */
public final class WeatherDisplayHelper {

    private WeatherDisplayHelper() {
    }

    public static final class Pm25Display {
        public final String valueText;
        public final String levelText;
        public final String aqiText;
        public final int colorRes;
        public final int bgRes;
        public final int numericValue;
        public final int usAqi;
        public final boolean hasData;

        Pm25Display(String valueText, String levelText, String aqiText,
                    int colorRes, int bgRes, int numericValue, int usAqi, boolean hasData) {
            this.valueText = valueText;
            this.levelText = levelText;
            this.aqiText = aqiText;
            this.colorRes = colorRes;
            this.bgRes = bgRes;
            this.numericValue = numericValue;
            this.usAqi = usAqi;
            this.hasData = hasData;
        }
    }

    public static final class UvDisplay {
        public final String valueText;
        public final String levelText;
        public final int colorRes;
        public final int bgRes;

        UvDisplay(String valueText, String levelText, int colorRes, int bgRes) {
            this.valueText = valueText;
            this.levelText = levelText;
            this.colorRes = colorRes;
            this.bgRes = bgRes;
        }
    }

    /** PM2.5 theo µg/m³ + AQI US EPA để người dùng dễ đối chiếu app thời tiết. */
    public static Pm25Display formatPm25(double pm25, boolean hasData, int usAqiFromApi) {
        if (!hasData || pm25 < 0) {
            return new Pm25Display("--", "Chưa có dữ liệu", "",
                    R.color.gray_dark, R.drawable.bg_card_status_blue, -1, -1, false);
        }

        int levelValue = (int) Math.round(pm25);
        int usAqi = usAqiFromApi > 0 ? usAqiFromApi : computeUsAqiFromPm25(pm25);
        String valueText = String.format(Locale.US, "%.1f", pm25);
        String levelText = pm25LevelText(levelValue);
        String aqiText = "AQI " + usAqi + " · " + aqiLevelText(usAqi);
        int colorRes = aqiColorRes(usAqi);
        int bgRes = aqiBgRes(usAqi);

        return new Pm25Display(valueText, levelText, aqiText, colorRes, bgRes, levelValue, usAqi, true);
    }

    /** @deprecated dùng {@link #formatPm25(double, boolean, int)} */
    public static Pm25Display formatPm25(double pm25, boolean hasData) {
        return formatPm25(pm25, hasData, -1);
    }

    public static int computeUsAqiFromPm25(double pm25) {
        if (pm25 <= 12.0) return (int) Math.round(Math.max(0, pm25 * 50.0 / 12.0));
        if (pm25 <= 35.4) return (int) Math.round(51 + (pm25 - 12.1) * 49.0 / (35.4 - 12.1));
        if (pm25 <= 55.4) return (int) Math.round(101 + (pm25 - 35.5) * 49.0 / (55.4 - 35.5));
        if (pm25 <= 150.4) return (int) Math.round(151 + (pm25 - 55.5) * 49.0 / (150.4 - 55.5));
        if (pm25 <= 250.4) return (int) Math.round(201 + (pm25 - 150.5) * 99.0 / (250.4 - 150.5));
        if (pm25 <= 350.4) return (int) Math.round(301 + (pm25 - 250.5) * 99.0 / (350.4 - 250.5));
        return (int) Math.round(401 + (pm25 - 350.5) * 99.0 / (500.4 - 350.5));
    }

    private static String pm25LevelText(int pm25Rounded) {
        if (pm25Rounded <= 12) return "Tốt";
        if (pm25Rounded <= 35) return "Trung bình";
        if (pm25Rounded <= 55) return "Kém";
        if (pm25Rounded <= 150) return "Xấu";
        return "Rất xấu";
    }

    private static String aqiLevelText(int usAqi) {
        if (usAqi <= 50) return "Tốt";
        if (usAqi <= 100) return "Trung bình";
        if (usAqi <= 150) return "Kém";
        if (usAqi <= 200) return "Xấu";
        if (usAqi <= 300) return "Rất xấu";
        return "Nguy hiểm";
    }

    private static int aqiColorRes(int usAqi) {
        if (usAqi <= 50) return R.color.status_level_green;
        if (usAqi <= 100) return R.color.status_level_yellow;
        if (usAqi <= 150) return R.color.status_level_orange;
        return R.color.status_level_red;
    }

    private static int aqiBgRes(int usAqi) {
        if (usAqi <= 50) return R.drawable.bg_card_status_green;
        if (usAqi <= 100) return R.drawable.bg_card_status_yellow;
        if (usAqi <= 150) return R.drawable.bg_card_status_orange;
        return R.drawable.bg_card_status_red;
    }

    /** UV theo thang WHO. */
    public static UvDisplay formatUv(double uv) {
        String valueText = String.format(Locale.US, "%.1f", uv);
        String levelText;
        int colorRes;
        int bgRes;

        if (uv < 3) {
            levelText = "Thấp";
            colorRes = R.color.status_level_green;
            bgRes = R.drawable.bg_card_status_green;
        } else if (uv < 6) {
            levelText = "Trung bình";
            colorRes = R.color.status_level_yellow;
            bgRes = R.drawable.bg_card_status_yellow;
        } else if (uv < 8) {
            levelText = "Cao";
            colorRes = R.color.status_level_orange;
            bgRes = R.drawable.bg_card_status_orange;
        } else if (uv < 11) {
            levelText = "Rất cao";
            colorRes = R.color.status_level_red;
            bgRes = R.drawable.bg_card_status_red;
        } else {
            levelText = "Cực cao";
            colorRes = R.color.status_level_red;
            bgRes = R.drawable.bg_card_status_red;
        }

        return new UvDisplay(valueText, levelText, colorRes, bgRes);
    }

    /** Mô tả thời tiết từ mã WMO (Open-Meteo weather_code). */
    public static String weatherCodeToDescription(int code, double tempC) {
        String base;
        switch (code) {
            case 0: base = "Trời quang"; break;
            case 1: base = "Ít mây"; break;
            case 2: base = "Có mây"; break;
            case 3: base = "U ám"; break;
            case 45:
            case 48: base = "Sương mù"; break;
            case 51:
            case 53:
            case 55: base = "Mưa phùn"; break;
            case 56:
            case 57: base = "Mưa phùn đóng băng"; break;
            case 61: base = "Mưa nhẹ"; break;
            case 63: base = "Mưa vừa"; break;
            case 65: base = "Mưa to"; break;
            case 66:
            case 67: base = "Mưa đá"; break;
            case 71: base = "Tuyết nhẹ"; break;
            case 73: base = "Tuyết vừa"; break;
            case 75: base = "Tuyết dày"; break;
            case 77: base = "Hạt tuyết"; break;
            case 80: base = "Mưa rào nhẹ"; break;
            case 81: base = "Mưa rào"; break;
            case 82: base = "Mưa rào mạnh"; break;
            case 85:
            case 86: base = "Mưa tuyết"; break;
            case 95: base = "Dông"; break;
            case 96:
            case 99: base = "Dông kèm mưa đá"; break;
            default: base = tempC >= 33 ? "Nắng nóng" : tempC >= 28 ? "Ấm áp" : "Dễ chịu"; break;
        }

        if (tempC >= 35 && code <= 3) {
            return "NẮNG NÓNG GAY GẮT";
        }
        if (tempC >= 33 && code <= 3) {
            return "NẮNG NHIỀU, OI NHẸ";
        }
        return base.toUpperCase(Locale.getDefault());
    }

    public static int weatherConditionColorRes(int code, double tempC) {
        if (tempC >= 33) return R.color.status_level_red;
        if (tempC >= 28 && code <= 3) return R.color.status_level_yellow;
        if (code >= 61 && code <= 67) return R.color.status_level_blue;
        if (code >= 80 && code <= 82) return R.color.status_level_blue;
        if (code >= 95) return R.color.status_level_orange;
        return R.color.secondary;
    }

    public static String buildDataSourceNote(boolean hasLiveWeather, boolean hasPm25) {
        if (!hasLiveWeather) {
            return "Không tải được dữ liệu thời tiết. Vui lòng kiểm tra kết nối mạng.";
        }
        if (hasPm25) {
            return "Nguồn: Open-Meteo • Nhiệt độ, độ ẩm, UV hiện tại • PM2.5 (µg/m³)";
        }
        return "Nguồn: Open-Meteo • Nhiệt độ, độ ẩm, UV hiện tại • PM2.5: chưa có dữ liệu khu vực";
    }
}
