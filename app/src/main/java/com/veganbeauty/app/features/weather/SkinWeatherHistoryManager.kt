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
        // Save locally first
        saveToLocalOnly(context, diagnostic)

        // Then push to Firebase Firestore
        try {
            val userId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(context)
            if (userId.isNotBlank() && userId != "guest_user") {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val docId = diagnostic.date.replace("/", "-")

                val firestoreData = hashMapOf(
                    "id" to diagnostic.id,
                    "timestamp" to diagnostic.timestamp,
                    "date" to diagnostic.date,
                    "city" to diagnostic.city,
                    "temperature" to diagnostic.temperature,
                    "humidity" to diagnostic.humidity,
                    "uv" to diagnostic.uv,
                    "pm25" to diagnostic.pm25,
                    "skinType" to diagnostic.skinType,
                    "oilyPercent" to diagnostic.oilyPercent,
                    "hydrationPercent" to diagnostic.hydrationPercent,
                    "sensitivityPercent" to diagnostic.sensitivityPercent,
                    "insight" to diagnostic.insight,
                    "recommendedRoutine" to diagnostic.recommendedRoutine.map { item ->
                        hashMapOf(
                            "category" to item.category,
                            "productName" to item.productName,
                            "description" to item.description
                        )
                    }
                )

                db.collection("users").document(userId)
                    .collection("skin_weather_history").document(docId)
                    .set(firestoreData)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveToLocalOnly(context: Context, diagnostic: SkinWeatherDiagnostic) {
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

    fun syncFromFirestore(context: Context, onComplete: (List<SkinWeatherDiagnostic>) -> Unit) {
        try {
            val userId = com.veganbeauty.app.data.local.ProfileSession.getCurrentUserId(context)
            if (userId.isBlank() || userId == "guest_user") {
                onComplete(getHistory(context))
                return
            }

            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(userId)
                .collection("skin_weather_history")
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        try {
                            val id = document.getString("id") ?: ""
                            val timestamp = document.getLong("timestamp") ?: 0L
                            val date = document.getString("date") ?: ""
                            val city = document.getString("city") ?: ""
                            val temperature = document.getDouble("temperature") ?: 0.0
                            val humidity = document.getLong("humidity")?.toInt() ?: 0
                            val uv = document.getDouble("uv") ?: 0.0
                            val pm25 = document.getLong("pm25")?.toInt() ?: 0
                            val skinType = document.getString("skinType") ?: ""
                            val oilyPercent = document.getLong("oilyPercent")?.toInt() ?: 0
                            val hydrationPercent = document.getLong("hydrationPercent")?.toInt() ?: 0
                            val sensitivityPercent = document.getLong("sensitivityPercent")?.toInt() ?: 0
                            val insight = document.getString("insight") ?: ""

                            val routineList = mutableListOf<SkinWeatherDiagnostic.RoutineItem>()
                            val routineRaw = document.get("recommendedRoutine") as? List<Map<String, Any>>
                            if (routineRaw != null) {
                                for (itemMap in routineRaw) {
                                    routineList.add(
                                        SkinWeatherDiagnostic.RoutineItem(
                                            category = itemMap["category"] as? String ?: "",
                                            productName = itemMap["productName"] as? String ?: "",
                                            description = itemMap["description"] as? String ?: ""
                                        )
                                    )
                                }
                            }

                            val diagnostic = SkinWeatherDiagnostic(
                                id = id,
                                timestamp = timestamp,
                                date = date,
                                city = city,
                                temperature = temperature,
                                humidity = humidity,
                                uv = uv,
                                pm25 = pm25,
                                skinType = skinType,
                                oilyPercent = oilyPercent,
                                hydrationPercent = hydrationPercent,
                                sensitivityPercent = sensitivityPercent,
                                insight = insight,
                                recommendedRoutine = routineList
                            )
                            saveToLocalOnly(context, diagnostic)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    onComplete(getHistory(context))
                }
                .addOnFailureListener {
                    onComplete(getHistory(context))
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(getHistory(context))
        }
    }
}
