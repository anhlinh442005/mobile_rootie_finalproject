package com.veganbeauty.app.data.local;

import androidx.room.TypeConverter;
import com.veganbeauty.app.data.local.entities.KeyIngredient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductConverters {
    @TypeConverter
    public String fromStringList(List<String> value) {
        if (value == null) return "[]";
        return new JSONArray(value).toString();
    }

    @TypeConverter
    public List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @TypeConverter
    public String fromKeyIngredients(List<KeyIngredient> value) {
        if (value == null) return "[]";
        JSONArray array = new JSONArray();
        try {
            for (KeyIngredient item : value) {
                JSONObject obj = new JSONObject();
                obj.put("name", item.getName());
                obj.put("description", item.getDescription());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    @TypeConverter
    public List<KeyIngredient> toKeyIngredients(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        List<KeyIngredient> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new KeyIngredient(
                    obj.getString("name"),
                    obj.getString("description")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
