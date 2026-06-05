package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.*
import org.json.JSONObject

class LocalJsonReader(private val context: Context) {

    fun getFriendsForUser(userId: String): List<String> {
        return try {
            val jsonString = context.assets.open("User_com_friend.json").bufferedReader().use { it.readText() }
            val array = org.json.JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.getString("user_id") == userId) {
                    val friendsArray = obj.getJSONArray("friends")
                    val list = mutableListOf<String>()
                    for (j in 0 until friendsArray.length()) {
                        list.add(friendsArray.getString(j))
                    }
                    return list
                }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllProducts(): List<ProductEntity> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            val productList = mutableListOf<ProductEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val categoryIdRaw = obj.opt("categoryId")
                val categoryIdsStr = when (categoryIdRaw) {
                    is org.json.JSONArray -> {
                        val list = mutableListOf<String>()
                        for (j in 0 until categoryIdRaw.length()) {
                            list.add(categoryIdRaw.getString(j))
                        }
                        list.joinToString(",")
                    }
                    is String -> categoryIdRaw
                    else -> ""
                }

                val albumArr = obj.optJSONArray("album")
                val albumList = mutableListOf<String>()
                if (albumArr != null) {
                    for (j in 0 until albumArr.length()) {
                        albumList.add(albumArr.getString(j))
                    }
                }

                val keyArr = obj.optJSONArray("keyIngredients")
                val keyIngredientsList = mutableListOf<com.veganbeauty.app.data.local.entities.KeyIngredient>()
                if (keyArr != null) {
                    for (j in 0 until keyArr.length()) {
                        val keyObj = keyArr.getJSONObject(j)
                        keyIngredientsList.add(
                            com.veganbeauty.app.data.local.entities.KeyIngredient(
                                name = keyObj.optString("name", ""),
                                description = keyObj.optString("description", "")
                            )
                        )
                    }
                }

                val detailedArr = obj.optJSONArray("detailedIngredients")
                val detailedIngredientsList = mutableListOf<String>()
                if (detailedArr != null) {
                    for (j in 0 until detailedArr.length()) {
                        detailedIngredientsList.add(detailedArr.getString(j))
                    }
                }

                val idealArr = obj.optJSONArray("idealFor")
                val idealForList = mutableListOf<String>()
                if (idealArr != null) {
                    for (j in 0 until idealArr.length()) {
                        idealForList.add(idealArr.getString(j))
                    }
                }

                val benefitsArr = obj.optJSONArray("benefits")
                val benefitsList = mutableListOf<String>()
                if (benefitsArr != null) {
                    for (j in 0 until benefitsArr.length()) {
                        benefitsList.add(benefitsArr.getString(j))
                    }
                }

                productList.add(
                    ProductEntity(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        sku = obj.getString("sku"),
                        price = obj.getLong("price"),
                        category = obj.getString("category"),
                        categoryIds = categoryIdsStr,
                        brand = obj.optString("brand", ""),
                        stock = obj.getInt("stock"),
                        description = obj.optString("description", ""),
                        mainImage = obj.optString("mainImage", ""),
                        suitableFor = obj.optString("suitableFor", ""),
                        origin = obj.optString("origin", ""),
                        expiryDate = obj.optString("expiryDate", ""),
                        isNew = obj.optBoolean("isNew", false),
                        
                        album = albumList,
                        mainIngredientsSummary = obj.optString("mainIngredientsSummary", ""),
                        allergyInformation = obj.optString("allergyInformation", ""),
                        keyIngredients = keyIngredientsList,
                        detailedIngredients = detailedIngredientsList,
                        storyDescription = obj.optString("storyDescription", ""),
                        storyImage = obj.optString("storyImage", ""),
                        idealFor = idealForList,
                        benefits = benefitsList,
                        usage = obj.optString("usage", ""),
                        usageAmount = obj.optString("usageAmount", ""),
                        scent = obj.optString("scent", ""),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            productList
        } catch (e: Exception) { emptyList() }
    }

    fun getProducts(): List<CommunityProduct> {
        return getAllProducts().map { 
            CommunityProduct(it.id, it.name, it.mainImage, it.price.toInt()) 
        }
    }
    fun getUsers(): List<UserEntity> = emptyList()
    fun getCommunityPosts(): List<CommunityPostEntity> = emptyList()
    
    fun getReels(): List<ReelEntity> {
        return try {
            val jsonString = context.assets.open("community_reels_fb.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val reelList = mutableListOf<ReelEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val authorObj = obj.optJSONObject("author")
                val statsObj = obj.optJSONObject("stats")
                val videoObj = obj.optJSONObject("video")
                reelList.add(ReelEntity(
                    videoId = obj.optString("video_id", java.util.UUID.randomUUID().toString()),
                    caption = obj.optString("caption", ""),
                    authorId = authorObj?.optString("user_id") ?: "",
                    authorUsername = authorObj?.optString("username") ?: "",
                    authorDisplayName = authorObj?.optString("display_name") ?: "",
                    authorAvatarUrl = authorObj?.optString("avatar_url", authorObj?.optString("avatar")),
                    likesCount = statsObj?.optInt("likes") ?: obj.optInt("likes_count", 0),
                    commentsCount = statsObj?.optInt("comments") ?: obj.optInt("comment_count", 0),
                    shareCount = statsObj?.optInt("shares") ?: obj.optInt("share_count", 0),
                    thumbnailUrl = videoObj?.optString("thumbnail") ?: obj.optString("thumbnail_url", "")
                ))
            }
            reelList
        } catch (e: Exception) { emptyList() }
    }
    
    fun getExploreVideos(): List<YtVideoEntity> {
        return try {
            val jsonString = context.assets.open("community_video_yt.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val videoList = mutableListOf<YtVideoEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeArray = obj.optJSONArray("Type")
                val types = mutableListOf<String>()
                if (typeArray != null) {
                    for (j in 0 until typeArray.length()) {
                        types.add(typeArray.getString(j))
                    }
                }
                videoList.add(YtVideoEntity(
                    id = obj.optString("_id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", ""),
                    url = obj.optString("url", ""),
                    description = obj.optString("short_description", obj.optString("title", "")),
                    username = obj.optString("username", "Unknown User"),
                    avatarUrl = obj.optString("avatar", null),
                    type = types.joinToString(",")
                ))
            }
            videoList
        } catch (e: Exception) { emptyList() }
    }
    fun getAllNotifications(): List<NotificationItem> = emptyList()
    fun getAllOrders(): List<OrderEntity> {
        return try {
            val jsonString = context.assets.open("orders.json").bufferedReader().use { it.readText() }
            val root = org.json.JSONObject(jsonString)
            val jsonArray = root.getJSONArray("orders")
            val orderList = mutableListOf<OrderEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val orderItems = mutableListOf<com.veganbeauty.app.data.local.entities.OrderItem>()
                var totalAmount = 0L
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val price = itemObj.optLong("price", 0L)
                    val qty = itemObj.optInt("quantity", 1)
                    totalAmount += (price * qty)
                    orderItems.add(
                        com.veganbeauty.app.data.local.entities.OrderItem(
                            productId = itemObj.getString("productId"),
                            productName = itemObj.getString("productName"),
                            productImage = itemObj.getString("productImage"),
                            quantity = qty,
                            price = price
                        )
                    )
                }
                
                val shippingCost = obj.optLong("shippingCost", 30000L)
                val voucherDiscount = obj.optLong("voucherDiscount", 0L)
                val finalTotal = totalAmount + shippingCost - voucherDiscount
                
                orderList.add(
                    OrderEntity(
                        orderId = obj.getString("id"),
                        orderDate = obj.getString("orderDate"),
                        orderTime = obj.getString("orderTime"),
                        status = obj.getString("status"),
                        totalAmount = finalTotal,
                        items = orderItems,
                        shippingName = obj.optString("shippingName", "Nguyễn Văn A"),
                        shippingPhone = obj.optString("shippingPhone", "090 123 4567"),
                        shippingAddress = obj.optString("shippingAddress", "123 Đường Nguyễn Thị Minh Khai, Phường Đa Kao, Quận 1, TP. Hồ Chí Minh"),
                        shippingCost = shippingCost,
                        voucherDiscount = voucherDiscount,
                        paymentMethod = obj.optString("paymentMethod", "Thanh toán qua Ví MoMo"),
                        expectedDeliveryTime = if (obj.has("expectedDeliveryTime")) obj.getString("expectedDeliveryTime") else null,
                        hasReview = obj.optBoolean("hasReview", false),
                        reviewStars = obj.optInt("reviewStars", 0),
                        reviewText = if (obj.has("reviewText")) obj.getString("reviewText") else null,
                        reviewImage = if (obj.has("reviewImage")) obj.getString("reviewImage") else null,
                        isAnonymous = obj.optBoolean("isAnonymous", false),
                        recommendToFriends = obj.optBoolean("recommendToFriends", false)
                    )
                )
            }
            orderList
        } catch (e: Exception) { 
            e.printStackTrace()
            emptyList() 
        }
    }

    fun getIngredients(): List<IngredientEntity> {
        return try {
            val jsonString = context.assets.open("ingredient.json").bufferedReader().use { it.readText() }
            val jsonArray = org.json.JSONArray(jsonString)
            val list = mutableListOf<IngredientEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(IngredientEntity(
                    slug = obj.optString("slug", java.util.UUID.randomUUID().toString()),
                    name = obj.optString("name", ""),
                    scientificName = obj.optString("scientific_name", ""),
                    image = obj.optString("image", ""),
                    description = obj.optString("description", ""),
                    uses = obj.optString("uses", "")
                ))
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    fun getCommunityBlogs(limit: Int = 10): List<CommunityBlogEntity> {
        val list = mutableListOf<CommunityBlogEntity>()
        try {
            val inputStream = context.assets.open("community_blog.json")
            val reader = android.util.JsonReader(java.io.InputStreamReader(inputStream, "UTF-8"))
            reader.beginArray()
            while (reader.hasNext() && list.size < limit) {
                var id = ""
                var title = ""
                var shortDesc = ""
                var imageUrl = ""
                var publishedAt = ""
                
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    when (name) {
                        "_id" -> id = reader.nextString()
                        "title" -> title = reader.nextString()
                        "shortDescription" -> shortDesc = reader.nextString()
                        "publishedAt" -> publishedAt = reader.nextString()
                        "primaryImage" -> {
                            if (reader.peek() == android.util.JsonToken.NULL) {
                                reader.nextNull()
                            } else {
                                reader.beginObject()
                                while(reader.hasNext()){
                                    if(reader.nextName() == "url") {
                                        if (reader.peek() == android.util.JsonToken.NULL) {
                                            reader.nextNull()
                                        } else {
                                            imageUrl = reader.nextString()
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (id.isEmpty()) id = java.util.UUID.randomUUID().toString()
                list.add(CommunityBlogEntity(id, title, shortDesc, imageUrl, publishedAt))
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
