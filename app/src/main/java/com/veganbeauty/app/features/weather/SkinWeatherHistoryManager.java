package com.veganbeauty.app.features.weather;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkinWeatherHistoryManager {

    private static final String FILE_NAME = "skin_weather_history.json";

    public static void saveDiagnostic(Context context, SkinWeatherDiagnostic diagnostic) {
        saveToLocalOnly(context, diagnostic);
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
        listener.onComplete(getHistory(context));
    }
}
