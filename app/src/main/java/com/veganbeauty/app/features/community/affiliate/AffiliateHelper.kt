package com.veganbeauty.app.features.community.affiliate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object AffiliateHelper {
    private const val FILE_NAME = "affiliates_local.json"

    fun getAffiliateData(context: Context): JSONArray {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            try {
                return JSONArray(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fallback to assets
        try {
            val assetStr = context.assets.open("affiliates.json").bufferedReader().use { it.readText() }
            val root = JSONObject(assetStr)
            val arr = root.optJSONArray("affiliates") ?: JSONArray()
            saveAffiliateData(context, arr) // Save to local
            return arr
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return JSONArray()
    }

    fun saveAffiliateData(context: Context, jsonArray: JSONArray) {
        val file = File(context.filesDir, FILE_NAME)
        file.writeText(jsonArray.toString())
    }

    fun addAffiliateOrder(
        context: Context,
        referrerUserId: String,
        productId: String,
        productName: String,
        productImage: String,
        orderValue: Long,
        commissionAmount: Long,
        customerEmail: String
    ) {
        val affiliateArray = getAffiliateData(context)
        
        var userAffiliateObj: JSONObject? = null
        for (i in 0 until affiliateArray.length()) {
            val obj = affiliateArray.getJSONObject(i)
            if (obj.optString("user_id") == referrerUserId) {
                userAffiliateObj = obj
                break
            }
        }
        
        if (userAffiliateObj == null) {
            userAffiliateObj = JSONObject().apply {
                put("user_id", referrerUserId)
                put("total_revenue", 0L)
                put("total_commission", 0L)
                put("pending_commission", 0L)
                put("successful_orders", 0)
                put("new_customers", 0)
                put("orders", JSONArray())
                put("withdrawals", JSONArray())
            }
            affiliateArray.put(userAffiliateObj)
        }
        
        // Update values
        val currentPending = userAffiliateObj.optLong("pending_commission", 0L)
        userAffiliateObj.put("pending_commission", currentPending + commissionAmount)
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        val orderDateStr = sdf.format(Date())
        
        val newOrder = JSONObject().apply {
            put("order_id", "RT" + UUID.randomUUID().toString().substring(0, 6).uppercase())
            put("order_date", orderDateStr)
            put("customer", customerEmail)
            put("product_id", productId)
            put("product_name", productName)
            put("product_image", productImage)
            put("order_value", orderValue)
            put("commission", commissionAmount)
            put("status", "Đang xử lý")
        }
        
        val ordersArr = userAffiliateObj.optJSONArray("orders") ?: JSONArray()
        ordersArr.put(newOrder)
        userAffiliateObj.put("orders", ordersArr)
        
        saveAffiliateData(context, affiliateArray)
    }
}
