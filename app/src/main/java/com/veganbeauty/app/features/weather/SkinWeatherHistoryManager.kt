package com.veganbeauty.app.features.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SkinWeatherDiagnostic(
    val id: String,
    val timestamp: Long,
    val date: String,
    val city: String,
    val temperature: Double,
    val humidity: Int,
    val uv: Double,
    val pm25: Int,
    val skinType: String,
    val oilyPercent: Int,
    val hydrationPercent: Int,
    val sensitivityPercent: Int,
    val insight: String,
    val recommendedRoutine: List<RoutineItem>
) {
    data class RoutineItem(
        val category: String,
        val productName: String,
        val description: String
    )
}

object SkinWeatherHistoryManager {
    private const val FILE_NAME = "skin_weather_history.json"

    fun saveDiagnostic(context: Context, diagnostic: SkinWeatherDiagnostic) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val jsonArray = if (file.exists()) {
                val content = file.readText()
                if (content.isNotBlank()) JSONArray(content) else JSONArray()
            } else {
                JSONArray()
            }

            val diagObj = JSONObject().apply {
                put("id", diagnostic.id)
                put("timestamp", diagnostic.timestamp)
                put("date", diagnostic.date)
                put("city", diagnostic.city)
                put("temperature", diagnostic.temperature)
                put("humidity", diagnostic.humidity)
                put("uv", diagnostic.uv)
                put("pm25", diagnostic.pm25)
                put("skinType", diagnostic.skinType)
                put("oilyPercent", diagnostic.oilyPercent)
                put("hydrationPercent", diagnostic.hydrationPercent)
                put("sensitivityPercent", diagnostic.sensitivityPercent)
                put("insight", diagnostic.insight)

                val routineArray = JSONArray()
                diagnostic.recommendedRoutine.forEach { item ->
                    routineArray.put(JSONObject().apply {
                        put("category", item.category)
                        put("productName", item.productName)
                        put("description", item.description)
                    })
                }
                put("recommendedRoutine", routineArray)
            }

            var replaced = false
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                if (item.optString("date") == diagnostic.date) {
                    jsonArray.put(i, diagObj)
                    replaced = true
                    break
                }
            }

            if (!replaced) {
                jsonArray.put(diagObj)
            }

            file.writeText(jsonArray.toString(4))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getHistory(context: Context): List<SkinWeatherDiagnostic> {
        val list = mutableListOf<SkinWeatherDiagnostic>()
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return list
            val content = file.readText()
            if (content.isBlank()) return list

            val jsonArray = JSONArray(content)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val routineList = mutableListOf<SkinWeatherDiagnostic.RoutineItem>()
                val routineArray = obj.optJSONArray("recommendedRoutine")
                if (routineArray != null) {
                    for (j in 0 until routineArray.length()) {
                        val itemObj = routineArray.getJSONObject(j)
                        routineList.add(
                            SkinWeatherDiagnostic.RoutineItem(
                                category = itemObj.optString("category"),
                                productName = itemObj.optString("productName"),
                                description = itemObj.optString("description")
                            )
                        )
                    }
                }

                list.add(
                    SkinWeatherDiagnostic(
                        id = obj.optString("id"),
                        timestamp = obj.optLong("timestamp"),
                        date = obj.optString("date"),
                        city = obj.optString("city"),
                        temperature = obj.optDouble("temperature"),
                        humidity = obj.optInt("humidity"),
                        uv = obj.optDouble("uv"),
                        pm25 = obj.optInt("pm25"),
                        skinType = obj.optString("skinType"),
                        oilyPercent = obj.optInt("oilyPercent"),
                        hydrationPercent = obj.optInt("hydrationPercent"),
                        sensitivityPercent = obj.optInt("sensitivityPercent"),
                        insight = obj.optString("insight"),
                        recommendedRoutine = routineList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }
}
