package com.veganbeauty.app.data.local

import android.content.Context
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
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
                        mediaUrlsString = mediaUrls.joinToString(",")
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
}
