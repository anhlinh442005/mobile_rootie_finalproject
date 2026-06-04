package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.*
import org.json.JSONObject

class LocalJsonReader(private val context: Context) {

    fun getAllProducts(): List<ProductEntity> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            val productList = mutableListOf<ProductEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                productList.add(ProductEntity(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    sku = obj.getString("sku"),
                    price = obj.getLong("price"),
                    category = obj.getString("category"),
                    brand = obj.optString("brand", ""),
                    stock = obj.getInt("stock"),
                    description = obj.optString("description", ""),
                    mainImage = obj.optString("mainImage", ""),
                    suitableFor = obj.optString("suitableFor", ""),
                    origin = obj.optString("origin", ""),
                    expiryDate = obj.optString("expiryDate", ""),
                    isNew = obj.optBoolean("isNew", false)
                ))
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
    fun getAllOrders(): List<OrderEntity> = emptyList()

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
