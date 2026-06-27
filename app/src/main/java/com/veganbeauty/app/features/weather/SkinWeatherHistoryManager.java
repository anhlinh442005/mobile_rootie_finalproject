package com.veganbeauty.app.features.weather;

import android.content.Context;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.veganbeauty.app.data.local.ProfileSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkinWeatherHistoryManager {

    private static final String FILE_NAME = "skin_weather_history.json";

    public static class SkinWeatherDiagnostic {
        private final String id;
        private final long timestamp;
        private final String date;
        private final String city;
        private final double temperature;
        private final int humidity;
        private final double uv;
        private final int pm25;
        private final String skinType;
        private final int oilyPercent;
        private final int hydrationPercent;
        private final int sensitivityPercent;
        private final String insight;
        private final List<RoutineItem> recommendedRoutine;

        public SkinWeatherDiagnostic(String id, long timestamp, String date, String city, double temperature, int humidity, double uv, int pm25, String skinType, int oilyPercent, int hydrationPercent, int sensitivityPercent, String insight, List<RoutineItem> recommendedRoutine) {
            this.id = id;
            this.timestamp = timestamp;
            this.date = date;
            this.city = city;
            this.temperature = temperature;
            this.humidity = humidity;
            this.uv = uv;
            this.pm25 = pm25;
            this.skinType = skinType;
            this.oilyPercent = oilyPercent;
            this.hydrationPercent = hydrationPercent;
            this.sensitivityPercent = sensitivityPercent;
            this.insight = insight;
            this.recommendedRoutine = recommendedRoutine;
        }

        public String getId() { return id; }
        public long getTimestamp() { return timestamp; }
        public String getDate() { return date; }
        public String getCity() { return city; }
        public double getTemperature() { return temperature; }
        public int getHumidity() { return humidity; }
        public double getUv() { return uv; }
        public int getPm25() { return pm25; }
        public String getSkinType() { return skinType; }
        public int getOilyPercent() { return oilyPercent; }
        public int getHydrationPercent() { return hydrationPercent; }
        public int getSensitivityPercent() { return sensitivityPercent; }
        public String getInsight() { return insight; }
        public List<RoutineItem> getRecommendedRoutine() { return recommendedRoutine; }

        public static class RoutineItem {
            private final String category;
            private final String productName;
            private final String description;

            public RoutineItem(String category, String productName, String description) {
                this.category = category;
                this.productName = productName;
                this.description = description;
            }

            public String getCategory() { return category; }
            public String getProductName() { return productName; }
            public String getDescription() { return description; }
        }
    }

    public static void saveDiagnostic(Context context, SkinWeatherDiagnostic diagnostic) {
        saveToLocalOnly(context, diagnostic);

        try {
            String userId = ProfileSession.getCurrentUserId(context);
            if (userId != null && !userId.trim().isEmpty() && !userId.equals("guest_user")) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String docId = diagnostic.getDate().replace("/", "-");

                Map<String, Object> firestoreData = new HashMap<>();
                firestoreData.put("id", diagnostic.getId());
                firestoreData.put("timestamp", diagnostic.getTimestamp());
                firestoreData.put("date", diagnostic.getDate());
                firestoreData.put("city", diagnostic.getCity());
                firestoreData.put("temperature", diagnostic.getTemperature());
                firestoreData.put("humidity", diagnostic.getHumidity());
                firestoreData.put("uv", diagnostic.getUv());
                firestoreData.put("pm25", diagnostic.getPm25());
                firestoreData.put("skinType", diagnostic.getSkinType());
                firestoreData.put("oilyPercent", diagnostic.getOilyPercent());
                firestoreData.put("hydrationPercent", diagnostic.getHydrationPercent());
                firestoreData.put("sensitivityPercent", diagnostic.getSensitivityPercent());
                firestoreData.put("insight", diagnostic.getInsight());

                List<Map<String, String>> routineList = new ArrayList<>();
                for (SkinWeatherDiagnostic.RoutineItem item : diagnostic.getRecommendedRoutine()) {
                    Map<String, String> itemMap = new HashMap<>();
                    itemMap.put("category", item.getCategory());
                    itemMap.put("productName", item.getProductName());
                    itemMap.put("description", item.getDescription());
                    routineList.add(itemMap);
                }
                firestoreData.put("recommendedRoutine", routineList);

                db.collection("users").document(userId)
                        .collection("skin_weather_history").document(docId)
                        .set(firestoreData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveToLocalOnly(Context context, SkinWeatherDiagnostic diagnostic) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            JSONArray jsonArray;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String content = new String(data, "UTF-8");
                jsonArray = content.trim().isEmpty() ? new JSONArray() : new JSONArray(content);
            } else {
                jsonArray = new JSONArray();
            }

            JSONObject diagObj = new JSONObject();
            diagObj.put("id", diagnostic.getId());
            diagObj.put("timestamp", diagnostic.getTimestamp());
            diagObj.put("date", diagnostic.getDate());
            diagObj.put("city", diagnostic.getCity());
            diagObj.put("temperature", diagnostic.getTemperature());
            diagObj.put("humidity", diagnostic.getHumidity());
            diagObj.put("uv", diagnostic.getUv());
            diagObj.put("pm25", diagnostic.getPm25());
            diagObj.put("skinType", diagnostic.getSkinType());
            diagObj.put("oilyPercent", diagnostic.getOilyPercent());
            diagObj.put("hydrationPercent", diagnostic.getHydrationPercent());
            diagObj.put("sensitivityPercent", diagnostic.getSensitivityPercent());
            diagObj.put("insight", diagnostic.getInsight());

            JSONArray routineArray = new JSONArray();
            for (SkinWeatherDiagnostic.RoutineItem item : diagnostic.getRecommendedRoutine()) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("category", item.getCategory());
                itemObj.put("productName", item.getProductName());
                itemObj.put("description", item.getDescription());
                routineArray.put(itemObj);
            }
            diagObj.put("recommendedRoutine", routineArray);

            boolean replaced = false;
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                if (diagnostic.getDate().equals(item.optString("date"))) {
                    jsonArray.put(i, diagObj);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                jsonArray.put(diagObj);
            }

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(jsonArray.toString(4).getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<SkinWeatherDiagnostic> getHistory(Context context) {
        List<SkinWeatherDiagnostic> list = new ArrayList<>();
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.exists()) return list;

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            String content = new String(data, "UTF-8");

            if (content.trim().isEmpty()) return list;

            JSONArray jsonArray = new JSONArray(content);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                List<SkinWeatherDiagnostic.RoutineItem> routineList = new ArrayList<>();
                JSONArray routineArray = obj.optJSONArray("recommendedRoutine");
                if (routineArray != null) {
                    for (int j = 0; j < routineArray.length(); j++) {
                        JSONObject itemObj = routineArray.getJSONObject(j);
                        routineList.add(new SkinWeatherDiagnostic.RoutineItem(
                                itemObj.optString("category"),
                                itemObj.optString("productName"),
                                itemObj.optString("description")
                        ));
                    }
                }

                list.add(new SkinWeatherDiagnostic(
                        obj.optString("id"),
                        obj.optLong("timestamp"),
                        obj.optString("date"),
                        obj.optString("city"),
                        obj.optDouble("temperature"),
                        obj.optInt("humidity"),
                        obj.optDouble("uv"),
                        obj.optInt("pm25"),
                        obj.optString("skinType"),
                        obj.optInt("oilyPercent"),
                        obj.optInt("hydrationPercent"),
                        obj.optInt("sensitivityPercent"),
                        obj.optString("insight"),
                        routineList
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collections.sort(list, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        return list;
    }

    public interface OnSyncCompleteListener {
        void onComplete(List<SkinWeatherDiagnostic> history);
    }

    public static void syncFromFirestore(Context context, OnSyncCompleteListener listener) {
        try {
            String userId = ProfileSession.getCurrentUserId(context);
            if (userId == null || userId.trim().isEmpty() || userId.equals("guest_user")) {
                listener.onComplete(getHistory(context));
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId)
                    .collection("skin_weather_history")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                String id = document.getString("id") != null ? document.getString("id") : "";
                                long timestamp = document.getLong("timestamp") != null ? document.getLong("timestamp") : 0L;
                                String date = document.getString("date") != null ? document.getString("date") : "";
                                String city = document.getString("city") != null ? document.getString("city") : "";
                                double temperature = document.getDouble("temperature") != null ? document.getDouble("temperature") : 0.0;
                                int humidity = document.getLong("humidity") != null ? document.getLong("humidity").intValue() : 0;
                                double uv = document.getDouble("uv") != null ? document.getDouble("uv") : 0.0;
                                int pm25 = document.getLong("pm25") != null ? document.getLong("pm25").intValue() : 0;
                                String skinType = document.getString("skinType") != null ? document.getString("skinType") : "";
                                int oilyPercent = document.getLong("oilyPercent") != null ? document.getLong("oilyPercent").intValue() : 0;
                                int hydrationPercent = document.getLong("hydrationPercent") != null ? document.getLong("hydrationPercent").intValue() : 0;
                                int sensitivityPercent = document.getLong("sensitivityPercent") != null ? document.getLong("sensitivityPercent").intValue() : 0;
                                String insight = document.getString("insight") != null ? document.getString("insight") : "";

                                List<SkinWeatherDiagnostic.RoutineItem> routineList = new ArrayList<>();
                                Object routineRaw = document.get("recommendedRoutine");
                                if (routineRaw instanceof List) {
                                    List<?> rawList = (List<?>) routineRaw;
                                    for (Object itemObj : rawList) {
                                        if (itemObj instanceof Map) {
                                            Map<?, ?> itemMap = (Map<?, ?>) itemObj;
                                            routineList.add(new SkinWeatherDiagnostic.RoutineItem(
                                                    itemMap.get("category") instanceof String ? (String) itemMap.get("category") : "",
                                                    itemMap.get("productName") instanceof String ? (String) itemMap.get("productName") : "",
                                                    itemMap.get("description") instanceof String ? (String) itemMap.get("description") : ""
                                            ));
                                        }
                                    }
                                }

                                SkinWeatherDiagnostic diagnostic = new SkinWeatherDiagnostic(
                                        id, timestamp, date, city, temperature, humidity, uv, pm25, skinType,
                                        oilyPercent, hydrationPercent, sensitivityPercent, insight, routineList
                                );
                                saveToLocalOnly(context, diagnostic);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        listener.onComplete(getHistory(context));
                    })
                    .addOnFailureListener(e -> listener.onComplete(getHistory(context)));
        } catch (Exception e) {
            e.printStackTrace();
            listener.onComplete(getHistory(context));
        }
    }
}
