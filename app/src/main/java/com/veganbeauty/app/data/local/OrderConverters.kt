package com.veganbeauty.app.data.local

import androidx.room.TypeConverter
import com.veganbeauty.app.data.local.entities.OrderItem
import org.json.JSONArray
import org.json.JSONObject

class OrderConverters {
    @TypeConverter
    fun fromItemList(items: List<OrderItem>?): String {
        if (items == null) return "[]"
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("productId", item.productId)
            obj.put("productName", item.productName)
            obj.put("productImage", item.productImage)
            obj.put("quantity", item.quantity)
            obj.put("price", item.price)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toItemList(value: String?): List<OrderItem> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<OrderItem>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                OrderItem(
                    productId = obj.getString("productId"),
                    productName = obj.getString("productName"),
                    productImage = obj.getString("productImage"),
                    quantity = obj.getInt("quantity"),
                    price = obj.getLong("price")
                )
            )
        }
        return list
    }
}
