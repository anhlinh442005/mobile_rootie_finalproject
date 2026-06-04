package com.veganbeauty.app.data.local

import androidx.room.TypeConverter
import com.veganbeauty.app.data.local.entities.KeyIngredient
import org.json.JSONArray
import org.json.JSONObject

class ProductConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.let { JSONArray(it).toString() } ?: "[]"
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    @TypeConverter
    fun fromKeyIngredients(value: List<KeyIngredient>?): String {
        if (value == null) return "[]"
        val array = JSONArray()
        for (item in value) {
            val obj = JSONObject()
            obj.put("name", item.name)
            obj.put("description", item.description)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toKeyIngredients(value: String?): List<KeyIngredient> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<KeyIngredient>()
        val array = JSONArray(value)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                KeyIngredient(
                    name = obj.getString("name"),
                    description = obj.getString("description")
                )
            )
        }
        return list
    }
}
