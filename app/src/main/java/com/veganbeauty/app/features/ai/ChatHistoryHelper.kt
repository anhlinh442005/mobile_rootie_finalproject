package com.veganbeauty.app.features.ai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object ChatHistoryHelper {

    private const val FILE_NAME = "rootie_chat_history.json"

    fun saveChatHistory(context: Context, chatList: List<RootieChatItem>) {
        try {
            val jsonArray = JSONArray()
            chatList.forEach { item ->
                val jsonObject = JSONObject().apply {
                    put("id", item.id)
                    put("sender", item.sender.name)
                    put("messageText", item.messageText)
                    put("timeStr", item.timeStr)
                    put("type", item.type.name)
                    
                    item.diagnosticData?.let { diag ->
                        val diagJson = JSONObject().apply {
                            put("assessment", diag.assessment)
                            put("detailExplanation", diag.detailExplanation)
                            put("moistureVal", diag.moistureVal)
                            put("sensitivityVal", diag.sensitivityVal)
                            put("barrierVal", diag.barrierVal)
                            put("whyExplanation", diag.whyExplanation)
                            put("recommendedProductIds", JSONArray(diag.recommendedProductIds))
                            put("productPhases", JSONArray(diag.productPhases))
                            put("productSubcategories", JSONArray(diag.productSubcategories))
                            put("productExpertReasons", JSONArray(diag.productExpertReasons))
                        }
                        put("diagnosticData", diagJson)
                    }
                }
                jsonArray.put(jsonObject)
            }
            context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { fos ->
                fos.write(jsonArray.toString().toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChatHistory(context: Context): List<RootieChatItem> {
        val list = mutableListOf<RootieChatItem>()
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return list

        try {
            val jsonString = file.readText(Charsets.UTF_8)
            if (jsonString.isBlank()) return list
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val sender = RootieChatItem.Sender.valueOf(obj.optString("sender", "USER"))
                val messageText = obj.optString("messageText", "")
                val timeStr = obj.optString("timeStr", "")
                val type = RootieChatItem.ItemType.valueOf(obj.optString("type", "TEXT"))
                
                var diagnosticData: RootieChatItem.DiagnosticData? = null
                if (obj.has("diagnosticData")) {
                    val diagObj = obj.getJSONObject("diagnosticData")
                    val assessment = diagObj.optString("assessment", "")
                    val detailExplanation = diagObj.optString("detailExplanation", "")
                    val moistureVal = diagObj.optString("moistureVal", "")
                    val sensitivityVal = diagObj.optString("sensitivityVal", "")
                    val barrierVal = diagObj.optString("barrierVal", "")
                    val whyExplanation = diagObj.optString("whyExplanation", "")
                    
                    val prodIdsArray = diagObj.optJSONArray("recommendedProductIds")
                    val recommendedProductIds = mutableListOf<String>()
                    if (prodIdsArray != null) {
                        for (j in 0 until prodIdsArray.length()) {
                            recommendedProductIds.add(prodIdsArray.getString(j))
                        }
                    }
                    
                    val phasesArray = diagObj.optJSONArray("productPhases")
                    val productPhases = mutableListOf<String>()
                    if (phasesArray != null) {
                        for (j in 0 until phasesArray.length()) {
                            productPhases.add(phasesArray.getString(j))
                        }
                    }
                    
                    val subcatsArray = diagObj.optJSONArray("productSubcategories")
                    val productSubcategories = mutableListOf<String>()
                    if (subcatsArray != null) {
                        for (j in 0 until subcatsArray.length()) {
                            productSubcategories.add(subcatsArray.getString(j))
                        }
                    }
                    
                    val reasonsArray = diagObj.optJSONArray("productExpertReasons")
                    val productExpertReasons = mutableListOf<String>()
                    if (reasonsArray != null) {
                        for (j in 0 until reasonsArray.length()) {
                            productExpertReasons.add(reasonsArray.getString(j))
                        }
                    }

                    diagnosticData = RootieChatItem.DiagnosticData(
                        assessment = assessment,
                        detailExplanation = detailExplanation,
                        moistureVal = moistureVal,
                        sensitivityVal = sensitivityVal,
                        barrierVal = barrierVal,
                        whyExplanation = whyExplanation,
                        recommendedProductIds = recommendedProductIds,
                        productPhases = productPhases,
                        productSubcategories = productSubcategories,
                        productExpertReasons = productExpertReasons
                    )
                }

                list.add(
                    RootieChatItem(
                        id = id,
                        sender = sender,
                        messageText = messageText,
                        timeStr = timeStr,
                        type = type,
                        diagnosticData = diagnosticData
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun clearChatHistory(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
