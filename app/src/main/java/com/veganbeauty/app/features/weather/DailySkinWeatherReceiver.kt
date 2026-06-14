package com.veganbeauty.app.features.weather

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.veganbeauty.app.MainActivity
import com.veganbeauty.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class DailySkinWeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        
        Log.d("DailySkinWeatherReceiver", "onReceive triggered for daily skin weather notification")

        val enabled = com.veganbeauty.app.data.local.ProfileSession.isSkinWeatherNotiEnabled(context)
        val notiAllowed = com.veganbeauty.app.data.local.ProfileSession.isNotiEnabled(context)
        if (!enabled || !notiAllowed) {
            Log.d("DailySkinWeatherReceiver", "Daily skin weather notification is disabled or general notifications are disabled. Rescheduling and returning.")
            DailySkinWeatherScheduler.scheduleDailyNotification(context)
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("RootieQuizPrefs", Context.MODE_PRIVATE)
                val skinType = prefs.getString("SAVED_USER_SKIN_TYPE", "Da dầu nhạy cảm") ?: "Da dầu nhạy cảm"
                
                // Read coordinates
                val lat = prefs.getFloat("SAVED_WEATHER_LAT", 10.8231f).toDouble()
                val lng = prefs.getFloat("SAVED_WEATHER_LNG", 106.6297f).toDouble()
                val cityName = prefs.getString("SAVED_WEATHER_CITY", "Thành phố Hồ Chí Minh") ?: "Thành phố Hồ Chí Minh"

                // 1. Fetch current weather for the coordinates
                var temp = 32.0
                var humidity = 68
                var uv = 9.2
                var pm25Val = 45
                var success = false

                try {
                    val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current=temperature_2m,relative_humidity_2m&daily=uv_index_max&timezone=auto"
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(response)
                        val current = json.getJSONObject("current")
                        temp = current.getDouble("temperature_2m")
                        humidity = current.getInt("relative_humidity_2m")
                        val daily = json.getJSONObject("daily")
                        val uvArray = daily.getJSONArray("uv_index_max")
                        uv = if (uvArray.length() > 0) uvArray.getDouble(0) else 5.0
                        success = true
                    }
                } catch (e: Exception) {
                    Log.e("DailySkinWeatherReceiver", "Failed to fetch weather data, using fallback", e)
                }

                if (success) {
                    try {
                        val aqUrlString = "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lng&current=pm2_5&timezone=auto"
                        val aqUrl = URL(aqUrlString)
                        val aqConnection = aqUrl.openConnection() as HttpURLConnection
                        aqConnection.connectTimeout = 3000
                        aqConnection.readTimeout = 3000
                        if (aqConnection.responseCode == HttpURLConnection.HTTP_OK) {
                            val aqResponse = aqConnection.inputStream.bufferedReader().use { it.readText() }
                            val aqJson = JSONObject(aqResponse)
                            val aqCurrent = aqJson.getJSONObject("current")
                            pm25Val = aqCurrent.getDouble("pm2_5").toInt()
                         }
                    } catch (e: Exception) {
                        pm25Val = (15 + (temp * 0.4) + (humidity * 0.1)).toInt()
                    }
                }

                // 2. Generate custom skin advice using either Gemini or Ruleset
                val apiKey = com.veganbeauty.app.BuildConfig.GEMINI_API_KEY
                var advice = ""

                if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY_HERE") {
                    try {
                        val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"
                        val url = URL(urlString)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "application/json")
                        connection.connectTimeout = 8000
                        connection.readTimeout = 8000
                        connection.doOutput = true

                        val requestJson = JSONObject()
                        val partsArray = org.json.JSONArray().apply {
                            put(JSONObject().put("text", "Làn da: $skinType. Thời tiết hôm nay: $temp°C, độ ẩm $humidity%, UV $uv, PM2.5 $pm25Val. Đưa ra 1 câu khuyên bảo vệ da theo cấu trúc mẫu: 'Nhiệt độ hôm nay khá cao ($temp°C), UV đạt mức $uv. Da $skinType của bạn có nguy cơ [tình trạng], hãy nhớ [lời khuyên] nhé!'"))
                        }
                        val contentsArray = org.json.JSONArray().apply {
                            put(JSONObject().put("parts", partsArray))
                        }
                        requestJson.put("contents", contentsArray)

                        val systemInstruction = JSONObject().apply {
                            val systemParts = org.json.JSONArray().apply {
                                put(JSONObject().put("text", "Bạn là bác sĩ da liễu thông minh của ROOTIE. Hãy phân tích thời tiết và đưa ra 1 câu khuyên bảo vệ da bằng tiếng Việt dựa trên thời tiết hôm nay và loại da của người dùng theo đúng cấu trúc mẫu được yêu cầu: 'Nhiệt độ hôm nay khá cao (X°C), UV đạt mức Y. Da [loại da] của bạn có nguy cơ [tình trạng], hãy nhớ [lời khuyên] nhé!'. Không dùng ký tự ngoặc kép."))
                            }
                            put("parts", systemParts)
                        }
                        requestJson.put("systemInstruction", systemInstruction)

                        connection.outputStream.use { os ->
                            os.write(requestJson.toString().toByteArray(Charsets.UTF_8))
                        }

                        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                            val json = JSONObject(response)
                            val candidates = json.getJSONArray("candidates")
                            if (candidates.length() > 0) {
                                val content = candidates.getJSONObject(0).getJSONObject("content")
                                val parts = content.getJSONArray("parts")
                                if (parts.length() > 0) {
                                    advice = parts.getJSONObject(0).getString("text").trim()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DailySkinWeatherReceiver", "Failed to fetch Gemini advice in background", e)
                    }
                }

                if (advice.isBlank()) {
                    advice = when {
                        skinType.contains("nhạy cảm", ignoreCase = true) || skinType.contains("dầu nhạy cảm", ignoreCase = true) -> {
                            if (uv >= 8) "Nhiệt độ hôm nay khá cao ($temp°C), UV đạt mức $uv. Da nhạy cảm của bạn có nguy cơ kích ứng đỏ ửng, hãy nhớ thoa kem chống nắng vật lý dịu nhẹ và che chắn kỹ nhé!"
                            else "Nhiệt độ hôm nay khoảng $temp°C, độ ẩm $humidity%. Da nhạy cảm của bạn có nguy cơ mất nước, hãy nhớ thoa kem dưỡng ẩm phục hồi B5 nhé!"
                        }
                        skinType.contains("dầu", ignoreCase = true) -> {
                            if (temp >= 32) "Nhiệt độ hôm nay khá cao ($temp°C), UV đạt mức $uv. Da dầu của bạn có nguy cơ bóng nhờn bít tắc, hãy nhớ dùng sữa rửa mặt dịu nhẹ và bôi kem chống nắng kiềm dầu nhé!"
                            else "Nhiệt độ hôm nay khoảng $temp°C, độ ẩm $humidity%. Da dầu của bạn hoạt động ổn định, hãy nhớ bôi kem dưỡng dạng gel mỏng nhẹ nhé!"
                        }
                        skinType.contains("khô", ignoreCase = true) -> {
                            if (humidity < 55) "Nhiệt độ hôm nay khá cao ($temp°C), UV đạt mức $uv. Da khô của bạn có nguy cơ căng rát, hãy nhớ thoa kem chống nắng phổ rộng và cấp ẩm phục hồi nhé!"
                            else "Nhiệt độ hôm nay khoảng $temp°C, độ ẩm $humidity%. Da khô của bạn cần giữ nước, hãy nhớ cấp ẩm serum HA và kem dưỡng ẩm khóa sâu nhé!"
                        }
                        else -> {
                            if (uv >= 7) "Nhiệt độ hôm nay khá cao ($temp°C), UV đạt mức $uv. Làn da của bạn có nguy cơ sạm nám đen sạm, hãy nhớ bôi kem chống nắng phổ rộng trước khi ra ngoài nhé!"
                            else "Nhiệt độ hôm nay khoảng $temp°C, độ ẩm $humidity%. Làn da của bạn đang trong trạng thái tốt, hãy nhớ bôi kem chống nắng mỏng nhẹ để bảo vệ nhé!"
                        }
                    }
                }

                // 3. Post System Notification
                val channelId = "skin_weather_daily_channel"
                val notificationId = 2001
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                         channelId,
                         "Đánh giá thời tiết & Da hàng ngày",
                         NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                         description = "Kênh gửi thông báo phân tích da theo thời tiết mỗi sáng lúc 6h30"
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("NAVIGATE_TO", "WEATHER_FORECAST")
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val appIconBitmap = try {
                    BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
                } catch (e: Exception) {
                    null
                }

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("ROOTIE • Thời tiết & Da hôm nay ☀️")
                    .setContentText(advice)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(advice))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                if (appIconBitmap != null) {
                    builder.setLargeIcon(appIconBitmap)
                }

                notificationManager.notify(notificationId, builder.build())
                Log.d("DailySkinWeatherReceiver", "Daily notification posted: $advice")

            } catch (e: Exception) {
                Log.e("DailySkinWeatherReceiver", "Error processing daily notification", e)
            } finally {
                // 4. Reschedule for the next day
                DailySkinWeatherScheduler.scheduleDailyNotification(context)
                pendingResult.finish()
            }
        }
    }
}
