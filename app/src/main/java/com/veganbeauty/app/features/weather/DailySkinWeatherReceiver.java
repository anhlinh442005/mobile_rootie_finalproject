package com.veganbeauty.app.features.weather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.veganbeauty.app.BuildConfig;
import com.veganbeauty.app.data.local.ProfileSession;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DailySkinWeatherReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        final Context appContext = context.getApplicationContext();

        Log.d("DailySkinWeatherReceiver", "onReceive triggered for daily skin weather notification");

        boolean enabled = ProfileSession.isSkinWeatherNotiEnabled(appContext);
        boolean notiAllowed = ProfileSession.isNotiEnabled(appContext);
        if (!enabled || !notiAllowed || !com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.canPost(appContext)) {
            Log.d("DailySkinWeatherReceiver", "Daily skin weather notification is disabled or cannot be posted. Rescheduling and returning.");
            DailySkinWeatherScheduler.scheduleDailyNotification(appContext);
            pendingResult.finish();
            return;
        }

        new Thread(() -> {
            try {
                SharedPreferences prefs = appContext.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE);
                String skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm");
                if (skinType == null) skinType = "Da dầu nhạy cảm";

                double lat = prefs.getFloat("SAVED_WEATHER_LAT", 10.8231f);
                double lng = prefs.getFloat("SAVED_WEATHER_LNG", 106.6297f);

                double temp = 32.0;
                int humidity = 68;
                double uv = 9.2;
                double pm25 = -1.0;
                int usAqi = -1;
                int weatherCode = -1;
                boolean hasPm25 = false;
                boolean success = false;
                String pm25Source = "";
                String pm25Station = "";
                String cityName = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh");
                if (cityName == null || cityName.trim().isEmpty()) {
                    cityName = "Thành phố Hồ Chí Minh";
                }

                try {
                    String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lng
                            + "&current=temperature_2m,relative_humidity_2m,weather_code,uv_index&timezone=auto";
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) responseBuilder.append(line);
                        reader.close();

                        JSONObject json = new JSONObject(responseBuilder.toString());
                        JSONObject current = json.getJSONObject("current");
                        temp = current.getDouble("temperature_2m");
                        humidity = current.getInt("relative_humidity_2m");
                        weatherCode = current.optInt("weather_code", -1);
                        if (current.has("uv_index") && !current.isNull("uv_index")) {
                            uv = current.getDouble("uv_index");
                        }
                        success = true;
                    }
                } catch (Exception e) {
                    Log.e("DailySkinWeatherReceiver", "Failed to fetch weather data, using fallback", e);
                }

                if (success) {
                    AirQualityFetcher.Reading aqReading = AirQualityFetcher.fetch(lat, lng, BuildConfig.WAQI_API_KEY);
                    if (aqReading.hasData()) {
                        pm25 = aqReading.pm25UgM3;
                        hasPm25 = true;
                        usAqi = aqReading.usAqi > 0
                                ? aqReading.usAqi
                                : WeatherDisplayHelper.computeUsAqiFromPm25(pm25);
                        pm25Source = aqReading.source.name();
                        pm25Station = aqReading.stationName != null ? aqReading.stationName : "";
                    }

                    String condition = WeatherDisplayHelper.weatherCodeToDescription(weatherCode, temp);
                    SkinWeatherSnapshotManager.saveAndSync(appContext, new SkinWeatherSnapshotManager.Snapshot(
                            temp, humidity, uv, pm25, usAqi,
                            lat, lng, weatherCode, true, hasPm25,
                            cityName, condition, pm25Source, pm25Station,
                            System.currentTimeMillis()
                    ));
                }

                String apiKey = BuildConfig.GEMINI_API_KEY;
                String advice = "";
                String pm25Text = "không có dữ liệu";
                if (hasPm25) {
                    WeatherDisplayHelper.Pm25Display pm25Display =
                            WeatherDisplayHelper.formatPm25(pm25, true, usAqi);
                    pm25Text = pm25Display.valueText + " µg/m³ PM2.5 (" + pm25Display.aqiText + ")";
                }

                if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
                    try {
                        String urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
                        URL url = new URL(urlString);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setConnectTimeout(8000);
                        connection.setReadTimeout(8000);
                        connection.setDoOutput(true);

                        JSONObject requestJson = new JSONObject();
                        JSONArray partsArray = new JSONArray();
                        String prompt = "Làn da: " + skinType + ". Thời tiết hôm nay: " + temp + "°C, độ ẩm " + humidity + "%, UV " + uv + ", PM2.5 " + pm25Text + ". Đưa ra 1 câu khuyên bảo vệ da theo cấu trúc mẫu: 'Nhiệt độ hôm nay khá cao (" + temp + "°C), UV đạt mức " + uv + ". Da " + skinType + " của bạn có nguy cơ [tình trạng], hãy nhớ [lời khuyên] nhé!'";
                        partsArray.put(new JSONObject().put("text", prompt));

                        JSONArray contentsArray = new JSONArray();
                        contentsArray.put(new JSONObject().put("parts", partsArray));
                        requestJson.put("contents", contentsArray);

                        JSONObject systemInstruction = new JSONObject();
                        JSONArray systemParts = new JSONArray();
                        systemParts.put(new JSONObject().put("text", "Bạn là bác sĩ da liễu thông minh của ROOTIE. Hãy phân tích thời tiết và đưa ra 1 câu khuyên bảo vệ da bằng tiếng Việt dựa trên thời tiết hôm nay và loại da của người dùng theo đúng cấu trúc mẫu được yêu cầu: 'Nhiệt độ hôm nay khá cao (X°C), UV đạt mức Y. Da [loại da] của bạn có nguy cơ [tình trạng], hãy nhớ [lời khuyên] nhé!'. Không dùng ký tự ngoặc kép."));
                        systemInstruction.put("parts", systemParts);
                        requestJson.put("systemInstruction", systemInstruction);

                        OutputStream os = connection.getOutputStream();
                        os.write(requestJson.toString().getBytes("UTF-8"));
                        os.close();

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            StringBuilder responseBuilder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) responseBuilder.append(line);
                            reader.close();

                            JSONObject json = new JSONObject(responseBuilder.toString());
                            JSONArray candidates = json.getJSONArray("candidates");
                            if (candidates.length() > 0) {
                                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                                JSONArray parts = content.getJSONArray("parts");
                                if (parts.length() > 0) {
                                    advice = parts.getJSONObject(0).getString("text").trim();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("DailySkinWeatherReceiver", "Failed to fetch Gemini advice in background", e);
                    }
                }

                if (advice.isEmpty()) {
                    String lowerSkinType = skinType.toLowerCase();
                    if (lowerSkinType.contains("nhạy cảm") || lowerSkinType.contains("dầu nhạy cảm")) {
                        if (uv >= 8) advice = "Nhiệt độ hôm nay khá cao (" + temp + "°C), UV đạt mức " + uv + ". Da nhạy cảm của bạn có nguy cơ kích ứng đỏ ửng, hãy nhớ thoa kem chống nắng vật lý dịu nhẹ và che chắn kỹ nhé!";
                        else advice = "Nhiệt độ hôm nay khoảng " + temp + "°C, độ ẩm " + humidity + "%. Da nhạy cảm của bạn có nguy cơ mất nước, hãy nhớ thoa kem dưỡng ẩm phục hồi B5 nhé!";
                    } else if (lowerSkinType.contains("dầu")) {
                        if (temp >= 32) advice = "Nhiệt độ hôm nay khá cao (" + temp + "°C), UV đạt mức " + uv + ". Da dầu của bạn có nguy cơ bóng nhờn bít tắc, hãy nhớ dùng sữa rửa mặt dịu nhẹ và bôi kem chống nắng kiềm dầu nhé!";
                        else advice = "Nhiệt độ hôm nay khoảng " + temp + "°C, độ ẩm " + humidity + "%. Da dầu của bạn hoạt động ổn định, hãy nhớ bôi kem dưỡng dạng gel mỏng nhẹ nhé!";
                    } else if (lowerSkinType.contains("khô")) {
                        if (humidity < 55) advice = "Nhiệt độ hôm nay khá cao (" + temp + "°C), UV đạt mức " + uv + ". Da khô của bạn có nguy cơ căng rát, hãy nhớ thoa kem chống nắng phổ rộng và cấp ẩm phục hồi nhé!";
                        else advice = "Nhiệt độ hôm nay khoảng " + temp + "°C, độ ẩm " + humidity + "%. Da khô của bạn cần giữ nước, hãy nhớ cấp ẩm serum HA và kem dưỡng ẩm khóa sâu nhé!";
                    } else {
                        if (uv >= 7) advice = "Nhiệt độ hôm nay khá cao (" + temp + "°C), UV đạt mức " + uv + ". Làn da của bạn có nguy cơ sạm nám đen sạm, hãy nhớ bôi kem chống nắng phổ rộng trước khi ra ngoài nhé!";
                        else advice = "Nhiệt độ hôm nay khoảng " + temp + "°C, độ ẩm " + humidity + "%. Làn da của bạn đang trong trạng thái tốt, hãy nhớ bôi kem chống nắng mỏng nhẹ để bảo vệ nhé!";
                    }
                }

                com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.dispatch(
                        appContext,
                        com.veganbeauty.app.features.account.notification.LocalSystemNotificationHelper.dailyStableId("weather_daily"),
                        "ROOTIE • Thời tiết & Da hôm nay ☀️",
                        advice,
                        "Khác",
                        "skin care",
                        "WEATHER_FORECAST",
                        "XEM NGAY"
                );
                Log.d("DailySkinWeatherReceiver", "Daily notification posted: " + advice);

            } catch (Exception e) {
                Log.e("DailySkinWeatherReceiver", "Error processing daily notification", e);
            } finally {
                DailySkinWeatherScheduler.scheduleDailyNotification(appContext);
                pendingResult.finish();
            }
        }).start();
    }
}
