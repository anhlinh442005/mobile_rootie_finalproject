package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.ProductEntity
import org.json.JSONObject

import org.json.JSONArray

class LocalJsonReader(private val context: Context) {

    fun getAllProducts(): List<ProductEntity> {
        return try {
            // Đọc file từ thư mục assets
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            
            val productList = mutableListOf<ProductEntity>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                val categoryIdRaw = obj.opt("categoryId")
                val categoryIdsStr = when (categoryIdRaw) {
                    is JSONArray -> {
                        val list = mutableListOf<String>()
                        for (j in 0 until categoryIdRaw.length()) {
                            list.add(categoryIdRaw.getString(j))
                        }
                        list.joinToString(",")
                    }
                    is String -> categoryIdRaw
                    else -> ""
                }

                productList.add(
                    ProductEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        sku = obj.getString("sku"),
                        price = obj.getLong("price"),
                        category = obj.getString("category"),
                        categoryIds = categoryIdsStr,
                        stock = obj.getInt("stock"),
                        description = obj.optString("description", ""),
                        mainImage = obj.optString("mainImage", ""),
                        suitableFor = obj.optString("suitableFor", ""),
                        origin = obj.optString("origin", ""),
                        expiryDate = obj.optString("expiryDate", "")
                    )
                )
            }
            productList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
