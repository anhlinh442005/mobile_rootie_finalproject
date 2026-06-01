package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import org.json.JSONArray
import org.json.JSONObject

class LocalJsonReader(private val context: Context) {

    fun getCommunityPosts(): List<CommunityPostEntity> {
        return try {
            val jsonString = context.assets.open("community_posts.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("posts")
            
            val postList = mutableListOf<CommunityPostEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val authorObj = obj.getJSONObject("author")
                val authorId = authorObj.getString("user_id")
                val authorUsername = authorObj.getString("username")
                val authorDisplayName = authorObj.optString("display_name", "")
                val authorAvatar = authorObj.optString("avatar_url", "")
                val authorAvatarUrl = if (authorAvatar.isEmpty() || authorAvatar == "null") null else authorAvatar
                
                val mediaArray = obj.optJSONArray("media")
                val mediaUrls = mutableListOf<String>()
                if (mediaArray != null) {
                    for (j in 0 until mediaArray.length()) {
                        val mediaObj = mediaArray.getJSONObject(j)
                        mediaUrls.add(mediaObj.getString("url"))
                    }
                }
                
                var likesCount = 0
                if (obj.has("reactions")) {
                    likesCount = obj.getJSONObject("reactions").optInt("like", 0)
                }

                val linkedProductsArray = obj.optJSONArray("linked_products")
                val linkedProductIds = mutableListOf<String>()
                if (linkedProductsArray != null) {
                    for (j in 0 until linkedProductsArray.length()) {
                        linkedProductIds.add(linkedProductsArray.getString(j))
                    }
                }

                postList.add(
                    CommunityPostEntity(
                        postId = obj.getString("post_id"),
                        authorId = authorId,
                        authorUsername = authorUsername,
                        authorDisplayName = authorDisplayName,
                        authorAvatarUrl = authorAvatarUrl,
                        content = obj.getString("content"),
                        createdAt = obj.getString("created_at"),
                        likesCount = likesCount,
                        commentsCount = obj.optInt("comments_count", 0),
                        skinType = obj.optString("skin_type").takeIf { it.isNotEmpty() },
                        concern = obj.optString("concern").takeIf { it.isNotEmpty() },
                        mediaUrlsString = mediaUrls.joinToString(","),
                        type = obj.optString("type").takeIf { it.isNotEmpty() },
                        linkedProductIds = linkedProductIds.joinToString(",").takeIf { it.isNotEmpty() }
                    )
                )
            }
            postList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getUsers(): List<UserEntity> {
        return try {
            val jsonString = context.assets.open("users.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            val userList = mutableListOf<UserEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val avatar = obj.optString("avatar", "")
                val avatarUrl = if (avatar == "null" || avatar.isEmpty()) null else avatar
                userList.add(
                    UserEntity(
                        userId = obj.getString("user_id"),
                        username = obj.getString("username"),
                        avatarUrl = avatarUrl
                    )
                )
            }
            userList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getReels(): List<ReelEntity> {
        return try {
            val jsonString = context.assets.open("community_reels_fb.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            val reelList = mutableListOf<ReelEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val authorObj = obj.getJSONObject("author")
                val authorId = authorObj.getString("user_id")
                val authorUsername = authorObj.getString("username")
                val authorDisplayName = authorObj.optString("display_name", "")
                val authorAvatar = authorObj.optString("avatar_url", "")
                val authorAvatarUrl = if (authorAvatar.isEmpty() || authorAvatar == "null") null else authorAvatar

                var thumbnailUrl = ""
                if (obj.has("video")) {
                    thumbnailUrl = obj.getJSONObject("video").optString("thumbnail", "")
                }
                
                reelList.add(
                    ReelEntity(
                        videoId = obj.getString("video_id"),
                        caption = obj.getString("caption"),
                        authorId = authorId,
                        authorUsername = authorUsername,
                        authorDisplayName = authorDisplayName,
                        authorAvatarUrl = authorAvatarUrl,
                        likesCount = obj.optInt("likes_count", 0),
                        commentsCount = obj.optInt("comment_count", 0),
                        shareCount = obj.optInt("share_count", 0),
                        thumbnailUrl = thumbnailUrl
                    )
                )
            }
            reelList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getProducts(): List<com.veganbeauty.app.data.local.entities.CommunityProduct> {
        return try {
            val jsonString = context.assets.open("products.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val jsonArray = root.getJSONArray("products")
            
            val productList = mutableListOf<com.veganbeauty.app.data.local.entities.CommunityProduct>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                productList.add(
                    com.veganbeauty.app.data.local.entities.CommunityProduct(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        mainImage = obj.getString("mainImage"),
                        price = obj.optInt("price", 0)
                    )
                )
            }
            productList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getExploreVideos(): List<YtVideoEntity> {
        return try {
            val usersList = getUsers()
            val usersById = usersList.associateBy { it.userId }
            val usersByName = usersList.associateBy { it.username }
            val validAvatars = usersList.mapNotNull { it.avatarUrl }.filter { it.isNotEmpty() }

            val jsonString = context.assets.open("community_video_yt.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            val videoList = mutableListOf<YtVideoEntity>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeArray = obj.optJSONArray("Type")
                val types = mutableListOf<String>()
                var isNotebook = false
                if (typeArray != null) {
                    for (j in 0 until typeArray.length()) {
                        val t = typeArray.getString(j)
                        types.add(t)
                        if (t.equals("notebook", ignoreCase = true)) {
                            isNotebook = true
                        }
                    }
                }
                
                // Skip videos with "notebook" type
                if (isNotebook) continue
                
                val userId = obj.optString("user_id", "")
                val username = obj.getString("username")

                var finalAvatarUrl: String? = null
                if (userId.isNotEmpty() && usersById.containsKey(userId)) {
                    finalAvatarUrl = usersById[userId]?.avatarUrl
                } else if (usersByName.containsKey(username)) {
                    finalAvatarUrl = usersByName[username]?.avatarUrl
                }

                if (finalAvatarUrl.isNullOrEmpty()) {
                    val originalAvatar = obj.optString("avatar", "")
                    finalAvatarUrl = if (originalAvatar == "null" || originalAvatar.isEmpty() || originalAvatar.contains("ytimg.com")) {
                        if (validAvatars.isNotEmpty()) validAvatars.random() else null
                    } else {
                        originalAvatar
                    }
                }
                
                videoList.add(
                    YtVideoEntity(
                        id = obj.getString("_id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        description = obj.optString("short_description", ""),
                        username = username,
                        avatarUrl = finalAvatarUrl,
                        type = types
                    )
                )
            }
            videoList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
