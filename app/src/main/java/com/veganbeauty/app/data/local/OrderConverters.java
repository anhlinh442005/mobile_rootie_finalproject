package com.veganbeauty.app.data.local;

import androidx.room.TypeConverter;
import com.veganbeauty.app.data.local.entities.OrderItem;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class OrderConverters {
    @TypeConverter
    public String fromItemList(List<OrderItem> items) {
        if (items == null) return "[]";
        JSONArray array = new JSONArray();
        try {
            for (OrderItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("productId", item.getProductId());
                obj.put("productName", item.getProductName());
                obj.put("productImage", item.getProductImage());
                obj.put("quantity", item.getQuantity());
                obj.put("price", item.getPrice());
                array.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    @TypeConverter
    public List<OrderItem> toItemList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        List<OrderItem> list = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(value);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new OrderItem(
                    obj.getString("productId"),
                    obj.getString("productName"),
                    obj.getString("productImage"),
                    obj.getInt("quantity"),
                    obj.getLong("price")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
