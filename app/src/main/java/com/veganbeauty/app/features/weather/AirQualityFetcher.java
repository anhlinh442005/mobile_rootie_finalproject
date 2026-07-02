package com.veganbeauty.app.features.weather;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

/**
 * Lấy PM2.5 (µg/m³).
 * Ưu tiên WAQI (trạm đo gần nhất — sát app thời tiết VN hơn).
 * Dự phòng: Open-Meteo CAMS (mô hình vệ tinh, có thể lệch vài µg/m³–vài chục %).
 */
public final class AirQualityFetcher {

    public enum Source {
        WAQI_STATION,
        OPEN_METEO_MODEL,
        NONE
    }

    public static final class Reading {
        public final double pm25UgM3;
        public final int usAqi;
        public final Source source;
        public final String stationName;

        Reading(double pm25UgM3, int usAqi, Source source, String stationName) {
            this.pm25UgM3 = pm25UgM3;
            this.usAqi = usAqi;
            this.source = source;
            this.stationName = stationName;
        }

        public boolean hasData() {
            return pm25UgM3 >= 0;
        }
    }

    private AirQualityFetcher() {
    }

    public static Reading fetch(double lat, double lng, String waqiApiKey) {
        if (isValidWaqiKey(waqiApiKey)) {
            Reading waqiReading = fetchFromWaqi(lat, lng, waqiApiKey);
            if (waqiReading.hasData()) {
                return waqiReading;
            }
        }
        return fetchFromOpenMeteo(lat, lng);
    }

    private static boolean isValidWaqiKey(String key) {
        return key != null
                && !key.trim().isEmpty()
                && !"YOUR_WAQI_API_KEY_HERE".equals(key.trim());
    }

    /** WAQI — dữ liệu từ trạm quan trắc gần GPS nhất (aqicn.org). */
    private static Reading fetchFromWaqi(double lat, double lng, String token) {
        try {
            String urlString = String.format(Locale.US,
                    "https://api.waqi.info/feed/geo:%.4f;%.4f/?token=%s", lat, lng, token.trim());
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                return emptyReading();
            }

            String response = readResponse(connection);
            connection.disconnect();

            JSONObject root = new JSONObject(response);
            if (!"ok".equalsIgnoreCase(root.optString("status"))) {
                return emptyReading();
            }

            JSONObject data = root.optJSONObject("data");
            if (data == null) {
                return emptyReading();
            }

            int aqi = data.optInt("aqi", -1);
            String dominentPol = data.optString("dominentpol", "");
            String stationName = "";
            JSONObject city = data.optJSONObject("city");
            if (city != null) {
                stationName = city.optString("name", "");
            }
            JSONObject station = data.optJSONObject("station");
            if (station != null && station.has("name")) {
                String sName = station.optString("name", "");
                if (!sName.isEmpty()) stationName = sName;
            }

            double pm25 = -1;
            JSONObject iaqi = data.optJSONObject("iaqi");
            if (iaqi != null) {
                JSONObject pm25Obj = iaqi.optJSONObject("pm25");
                if (pm25Obj != null && !pm25Obj.isNull("v")) {
                    pm25 = pm25Obj.getDouble("v");
                }
            }

            if (pm25 < 0 && aqi > 0) {
                pm25 = estimatePm25FromUsAqi(aqi);
            }

            if (pm25 < 0) {
                return emptyReading();
            }

            int displayAqi = aqi;
            if (!"pm25".equals(dominentPol) || displayAqi <= 0) {
                displayAqi = WeatherDisplayHelper.computeUsAqiFromPm25(pm25);
            }

            return new Reading(pm25, displayAqi, Source.WAQI_STATION, stationName);
        } catch (Exception e) {
            android.util.Log.w("AirQualityFetcher", "WAQI fetch failed", e);
            return emptyReading();
        }
    }

    /** Open-Meteo CAMS — mô hình khí tượng, không phải trạm đo trực tiếp. */
    private static Reading fetchFromOpenMeteo(double lat, double lng) {
        try {
            String urlString = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + lat
                    + "&longitude=" + lng + "&current=pm2_5,us_aqi&timezone=auto";
            HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                return emptyReading();
            }

            String response = readResponse(connection);
            connection.disconnect();

            JSONObject json = new JSONObject(response);
            JSONObject current = json.getJSONObject("current");
            if (!current.has("pm2_5") || current.isNull("pm2_5")) {
                return emptyReading();
            }

            double pm25 = current.getDouble("pm2_5");
            int usAqi = current.optInt("us_aqi", -1);
            return new Reading(pm25, usAqi, Source.OPEN_METEO_MODEL, "");
        } catch (Exception e) {
            android.util.Log.w("AirQualityFetcher", "Open-Meteo air quality fetch failed", e);
            return emptyReading();
        }
    }

    /** Ước lượng PM2.5 từ US AQI khi trạm chỉ trả về AQI tổng hợp. */
    private static double estimatePm25FromUsAqi(int usAqi) {
        if (usAqi <= 50) return usAqi * 12.0 / 50.0;
        if (usAqi <= 100) return 12.0 + (usAqi - 50) * (35.4 - 12.0) / 50.0;
        if (usAqi <= 150) return 35.5 + (usAqi - 100) * (55.4 - 35.4) / 50.0;
        if (usAqi <= 200) return 55.5 + (usAqi - 150) * (150.4 - 55.4) / 50.0;
        if (usAqi <= 300) return 150.5 + (usAqi - 200) * (250.4 - 150.4) / 100.0;
        return 250.5 + (usAqi - 300) * (350.4 - 250.4) / 100.0;
    }

    private static String readResponse(HttpURLConnection connection) throws Exception {
        InputStream is = connection.getInputStream();
        Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        String response = scanner.hasNext() ? scanner.next() : "";
        is.close();
        return response;
    }

    private static Reading emptyReading() {
        return new Reading(-1, -1, Source.NONE, "");
    }
}
