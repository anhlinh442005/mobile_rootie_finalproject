package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.ProductEntity
import com.veganbeauty.app.data.local.entities.OrderEntity
import com.veganbeauty.app.data.local.entities.OrderItem
import com.veganbeauty.app.data.local.entities.NotificationItem
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

    fun getAllOrders(): List<OrderEntity> {
        return try {
            val jsonString = context.assets.open("orders.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("orders")
            
            val orderList = mutableListOf<OrderEntity>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val itemList = mutableListOf<OrderItem>()
                
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    itemList.add(
                        OrderItem(
                            productId = itemObj.getString("productId"),
                            productName = itemObj.getString("productName"),
                            productImage = itemObj.getString("productImage"),
                            quantity = itemObj.getInt("quantity"),
                            price = itemObj.getLong("price")
                        )
                    )
                }
                
                val subtotal = itemList.sumOf { it.price * it.quantity }
                val shippingCost = obj.optLong("shippingCost", 30000L)
                val voucherDiscount = obj.optLong("voucherDiscount", 50000L)
                val totalAmount = subtotal + shippingCost - voucherDiscount

                orderList.add(
                    OrderEntity(
                        orderId = obj.getString("id"),
                        orderDate = obj.getString("orderDate"),
                        orderTime = obj.getString("orderTime"),
                        status = obj.getString("status"),
                        totalAmount = totalAmount,
                        items = itemList,
                        shippingName = obj.optString("shippingName", "Nguyễn Văn A"),
                        shippingPhone = obj.optString("shippingPhone", "090 123 4567"),
                        shippingAddress = obj.optString("shippingAddress", "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh"),
                        shippingCost = shippingCost,
                        voucherDiscount = voucherDiscount,
                        paymentMethod = obj.optString("paymentMethod", "Ví MoMo"),
                        expectedDeliveryTime = obj.optString("expectedDeliveryTime").takeIf { it.isNotEmpty() }
                    )
                )
            }
            orderList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getAllNotifications(): List<NotificationItem> {
        return try {
            val jsonString = context.assets.open("notifications.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("notifications")
            
            val notificationList = mutableListOf<NotificationItem>()
            
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                notificationList.add(
                    NotificationItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        content = obj.getString("content"),
                        time = obj.getString("time"),
                        category = obj.getString("category"),
                        tag = obj.optString("tag").takeIf { it.isNotEmpty() },
                        voucherCode = obj.optString("voucherCode").takeIf { it.isNotEmpty() },
                        actionText = obj.optString("actionText").takeIf { it.isNotEmpty() },
                        isRead = obj.optBoolean("isRead", false),
                        section = obj.getString("section"),
                        iconResName = obj.getString("iconResName")
                    )
                )
            }
            notificationList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
