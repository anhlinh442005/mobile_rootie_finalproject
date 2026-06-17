package com.veganbeauty.app.features.community.affiliate

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AffiliateProductsHelper {
    private const val FILE_NAME = "affiliate_product_local.json"

    private fun getFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    private fun readData(context: Context): JSONObject {
        val file = getFile(context)
        if (file.exists()) {
            try {
                return JSONObject(file.readText())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        try {
            val assetStr = context.assets.open("affiliate_product.json").bufferedReader().use { it.readText() }
            val root = JSONObject(assetStr)
            file.writeText(assetStr) // Save to local
            return root
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return JSONObject().apply { put("affiliate_products", JSONArray()) }
    }

    private fun writeData(context: Context, jsonObject: JSONObject) {
        getFile(context).writeText(jsonObject.toString())
    }

    private fun getUserData(context: Context, userId: String): JSONObject {
        val root = readData(context)
        val arr = root.optJSONArray("affiliate_products") ?: JSONArray().also { root.put("affiliate_products", it) }
        
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("userId") == userId) {
                return obj
            }
        }
        
        val newObj = JSONObject().apply {
            put("userId", userId)
            put("products", JSONArray())
        }
        arr.put(newObj)
        return newObj
    }

    fun isProductDisplayed(context: Context, userId: String, productId: String): Boolean {
        val userData = getUserData(context, userId)
        val productsArr = userData.optJSONArray("products") ?: return true
        for (i in 0 until productsArr.length()) {
            val p = productsArr.getJSONObject(i)
            if (p.optString("productId") == productId) {
                return p.optBoolean("affiliate_display", true)
            }
        }
        // If not in the list, default to false (not displayed)
        return false
    }

    fun setProductDisplayed(context: Context, userId: String, productId: String, displayed: Boolean) {
        val root = readData(context)
        val arr = root.optJSONArray("affiliate_products") ?: JSONArray().also { root.put("affiliate_products", it) }
        
        var userData: JSONObject? = null
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.optString("userId") == userId) {
                userData = obj
                break
            }
        }
        
        if (userData == null) {
            userData = JSONObject().apply {
                put("userId", userId)
                put("products", JSONArray())
            }
            arr.put(userData)
        }
        
        val productsArr = userData.optJSONArray("products") ?: JSONArray().also { userData.put("products", it) }
        var found = false
        for (i in 0 until productsArr.length()) {
            val p = productsArr.getJSONObject(i)
            if (p.optString("productId") == productId) {
                p.put("affiliate_display", displayed)
                found = true
                break
            }
        }
        
        if (!found) {
            productsArr.put(JSONObject().apply {
                put("productId", productId)
                put("affiliate_display", displayed)
            })
        }
        
        writeData(context, root)
    }
}
