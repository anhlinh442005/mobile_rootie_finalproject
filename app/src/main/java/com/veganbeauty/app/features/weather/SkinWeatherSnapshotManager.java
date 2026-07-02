package com.veganbeauty.app.features.weather;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.firestore.FirebaseFirestore;
import com.veganbeauty.app.data.local.ProfileSession;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Lưu local + đồng bộ Firestore snapshot thời tiết hiện tại của user. */
public final class SkinWeatherSnapshotManager {

    private static final String PREFS = "RootieQuizPrefs";
    private static final String DOC_ID = "current_weather";

    private SkinWeatherSnapshotManager() {
    }

    public static final class Snapshot {
        public final double temp;
        public final int humidity;
        public final double uv;
        public final double pm25;
        public final int usAqi;
        public final double lat;
        public final double lng;
        public final int weatherCode;
        public final boolean weatherSuccess;
        public final boolean hasPm25;
        public final String city;
        public final String condition;
        public final String pm25Source;
        public final String pm25Station;
        public final long updatedAt;

        public Snapshot(double temp, int humidity, double uv, double pm25, int usAqi,
                        double lat, double lng, int weatherCode, boolean weatherSuccess, boolean hasPm25,
                        String city, String condition, String pm25Source, String pm25Station, long updatedAt) {
            this.temp = temp;
            this.humidity = humidity;
            this.uv = uv;
            this.pm25 = pm25;
            this.usAqi = usAqi;
            this.lat = lat;
            this.lng = lng;
            this.weatherCode = weatherCode;
            this.weatherSuccess = weatherSuccess;
            this.hasPm25 = hasPm25;
            this.city = city;
            this.condition = condition;
            this.pm25Source = pm25Source;
            this.pm25Station = pm25Station;
            this.updatedAt = updatedAt;
        }

        public boolean isFresh(long maxAgeMs) {
            return updatedAt > 0 && (System.currentTimeMillis() - updatedAt) <= maxAgeMs;
        }
    }

    public interface OnSnapshotLoadedListener {
        void onLoaded(Snapshot snapshot);
    }

    public static void saveLocal(Context context, Snapshot snapshot) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit()
                    .putLong("SAVED_WEATHER_UPDATED_AT", snapshot.updatedAt)
                    .putFloat("SAVED_WEATHER_TEMP", (float) snapshot.temp)
                    .putInt("SAVED_WEATHER_HUMIDITY", snapshot.humidity)
                    .putFloat("SAVED_WEATHER_UV", (float) snapshot.uv)
                    .putString("SAVED_WEATHER_CITY", snapshot.city)
                    .putString("SAVED_WEATHER_CONDITION", snapshot.condition)
                    .putFloat("SAVED_WEATHER_LAT", (float) snapshot.lat)
                    .putFloat("SAVED_WEATHER_LNG", (float) snapshot.lng)
                    .putInt("SAVED_WEATHER_CODE", snapshot.weatherCode)
                    .putBoolean("SAVED_WEATHER_SUCCESS", snapshot.weatherSuccess)
                    .putString("SAVED_WEATHER_PM25_SOURCE", snapshot.pm25Source)
                    .putString("SAVED_WEATHER_PM25_STATION", snapshot.pm25Station)
                    .putInt("SAVED_WEATHER_US_AQI", snapshot.usAqi);
            if (snapshot.hasPm25) {
                editor.putFloat("SAVED_WEATHER_PM25", (float) snapshot.pm25);
            }
            editor.apply();
        } catch (Exception e) {
            android.util.Log.w("SkinWeatherSnapshot", "saveLocal failed", e);
        }
    }

    public static Snapshot loadLocal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long updatedAt = prefs.getLong("SAVED_WEATHER_UPDATED_AT", 0L);
        if (updatedAt <= 0L) {
            return null;
        }
        return new Snapshot(
                prefs.getFloat("SAVED_WEATHER_TEMP", 0f),
                prefs.getInt("SAVED_WEATHER_HUMIDITY", 0),
                prefs.getFloat("SAVED_WEATHER_UV", 0f),
                prefs.getFloat("SAVED_WEATHER_PM25", -1f),
                prefs.getInt("SAVED_WEATHER_US_AQI", -1),
                prefs.getFloat("SAVED_WEATHER_LAT", 0f),
                prefs.getFloat("SAVED_WEATHER_LNG", 0f),
                prefs.getInt("SAVED_WEATHER_CODE", -1),
                prefs.getBoolean("SAVED_WEATHER_SUCCESS", false),
                prefs.getFloat("SAVED_WEATHER_PM25", -1f) >= 0,
                prefs.getString("SAVED_WEATHER_CITY", ""),
                prefs.getString("SAVED_WEATHER_CONDITION", ""),
                prefs.getString("SAVED_WEATHER_PM25_SOURCE", ""),
                prefs.getString("SAVED_WEATHER_PM25_STATION", ""),
                updatedAt
        );
    }

    public static void saveAndSync(Context context, Snapshot snapshot) {
        saveLocal(context, snapshot);
        syncToFirestore(context, snapshot);
    }

    public static void syncToFirestore(Context context, Snapshot snapshot) {
        try {
            String userId = ProfileSession.getCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty() || "guest_user".equals(userId)) {
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("updatedAt", snapshot.updatedAt);
            data.put("temperature", snapshot.temp);
            data.put("humidity", snapshot.humidity);
            data.put("uv", snapshot.uv);
            data.put("pm25", snapshot.hasPm25 ? snapshot.pm25 : null);
            data.put("usAqi", snapshot.usAqi > 0 ? snapshot.usAqi : null);
            data.put("lat", snapshot.lat);
            data.put("lng", snapshot.lng);
            data.put("weatherCode", snapshot.weatherCode);
            data.put("weatherSuccess", snapshot.weatherSuccess);
            data.put("city", snapshot.city);
            data.put("condition", snapshot.condition);
            data.put("pm25Source", snapshot.pm25Source);
            data.put("pm25Station", snapshot.pm25Station);

            FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("profile_data").document(DOC_ID)
                    .set(data);
        } catch (Exception e) {
            android.util.Log.w("SkinWeatherSnapshot", "syncToFirestore failed", e);
        }
    }

    public static void loadFromFirestore(Context context, OnSnapshotLoadedListener listener) {
        try {
            String userId = ProfileSession.getCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty() || "guest_user".equals(userId)) {
                Snapshot local = loadLocal(context);
                if (local != null) listener.onLoaded(local);
                return;
            }

            FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("profile_data").document(DOC_ID)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (!document.exists()) {
                            Snapshot local = loadLocal(context);
                            if (local != null) listener.onLoaded(local);
                            return;
                        }
                        long remoteUpdatedAt = document.getLong("updatedAt") != null ? document.getLong("updatedAt") : 0L;
                        Snapshot local = loadLocal(context);
                        if (local != null && local.updatedAt >= remoteUpdatedAt) {
                            listener.onLoaded(local);
                            return;
                        }

                        Double pm25 = document.getDouble("pm25");
                        Long usAqi = document.getLong("usAqi");
                        Snapshot remote = new Snapshot(
                                document.getDouble("temperature") != null ? document.getDouble("temperature") : 0,
                                document.getLong("humidity") != null ? document.getLong("humidity").intValue() : 0,
                                document.getDouble("uv") != null ? document.getDouble("uv") : 0,
                                pm25 != null ? pm25 : -1,
                                usAqi != null ? usAqi.intValue() : -1,
                                document.getDouble("lat") != null ? document.getDouble("lat") : 0,
                                document.getDouble("lng") != null ? document.getDouble("lng") : 0,
                                document.getLong("weatherCode") != null ? document.getLong("weatherCode").intValue() : -1,
                                document.getBoolean("weatherSuccess") != null && document.getBoolean("weatherSuccess"),
                                pm25 != null,
                                document.getString("city") != null ? document.getString("city") : "",
                                document.getString("condition") != null ? document.getString("condition") : "",
                                document.getString("pm25Source") != null ? document.getString("pm25Source") : "",
                                document.getString("pm25Station") != null ? document.getString("pm25Station") : "",
                                remoteUpdatedAt
                        );
                        saveLocal(context, remote);
                        listener.onLoaded(remote);
                    })
                    .addOnFailureListener(e -> {
                        Snapshot local = loadLocal(context);
                        if (local != null) listener.onLoaded(local);
                    });
        } catch (Exception e) {
            Snapshot local = loadLocal(context);
            if (local != null) listener.onLoaded(local);
        }
    }

    public static String sourceLabel(String sourceKey) {
        if (sourceKey == null) return "";
        switch (sourceKey) {
            case "WAQI_STATION":
                return "Trạm đo gần bạn";
            case "OPEN_METEO_MODEL":
                return "Mô hình vệ tinh";
            default:
                return "";
        }
    }
}
