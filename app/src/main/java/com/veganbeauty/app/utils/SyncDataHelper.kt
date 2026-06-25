package com.veganbeauty.app.utils

import android.content.Context
import android.util.Log
import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object SyncDataHelper {
    fun syncAllLocalDataToFirebase(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                Log.d("SyncData", "Bắt đầu đồng bộ dữ liệu lên Firebase...")
                val reader = LocalJsonReader(context)
                val firestore = FirestoreService()
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

                // TẠM THỜI TẮT BƯỚC 1, 2, 3 THEO YÊU CẦU ĐỂ CHỈ ĐỒNG BỘ SKIN DATA
                /*
                // 1. Sync Explore Videos (YouTube/Handbook)
                try {
                    val videos = reader.getExploreVideos()
                    if (videos.isNotEmpty()) {
                        firestore.uploadAllExploreVideos(videos)
                        Log.d("SyncData", "Đã đồng bộ ${videos.size} Videos")
                    }
                } catch (e: Exception) { Log.e("SyncData", "Lỗi đồng bộ Videos: ${e.message}") }

                // 2. Sync Community Posts
                try {
                    val posts = reader.getCommunityPosts()
                    if (posts.isNotEmpty()) {
                        val batch = db.batch()
                        for (post in posts) {
                            val ref = db.collection("community_posts").document(post.postId)
                            val postMap = hashMapOf(
                                "author" to mapOf(
                                    "user_id" to post.authorId,
                                    "username" to post.authorUsername,
                                    "display_name" to post.authorDisplayName,
                                    "avatar" to post.authorAvatarUrl
                                ),
                                "content" to post.content,
                                "media" to post.mediaUrlsString.split(",").filter { it.isNotBlank() },
                                "created_at" to post.createdAt,
                                "reactions" to mapOf("like" to post.likesCount),
                                "comments_count" to post.commentsCount,
                                "skin_type" to post.skinType,
                                "concern" to post.concern,
                                "type" to post.type,
                                "linked_products" to (post.linkedProductIds ?: "").split(",").filter { it.isNotBlank() }
                            )
                            batch.set(ref, postMap)
                        }
                        batch.commit().await()
                        Log.d("SyncData", "Đã đồng bộ ${posts.size} Posts")
                    }
                } catch (e: Exception) { Log.e("SyncData", "Lỗi đồng bộ Posts: ${e.message}") }

                // 3. Sync Ingredients
                try {
                    val ingredientsJson = try {
                        context.assets.open("ingredients.json").bufferedReader().use { it.readText() }
                    } catch (e: Exception) { null }

                    if (ingredientsJson != null) {
                        val array = org.json.JSONArray(ingredientsJson)
                        val batch = db.batch()
                        for (i in 0 until array.length()) {
                            val obj = array.getJSONObject(i)
                            val slug = obj.optString("slug", java.util.UUID.randomUUID().toString())
                            val map = hashMapOf(
                                "slug" to slug,
                                "name" to obj.optString("name"),
                                "scientificName" to obj.optString("scientificName"),
                                "imageUrl" to obj.optString("imageUrl", obj.optString("image_url", obj.optString("image"))),
                                "description" to obj.optString("description"),
                                "uses" to obj.optString("uses")
                            )
                            val ref = db.collection("ingredients").document(slug)
                            batch.set(ref, map)
                        }
                        batch.commit().await()
                        Log.d("SyncData", "Đã đồng bộ ${array.length()} Ingredients")
                    }
                } catch (e: Exception) { Log.e("SyncData", "Lỗi đồng bộ Ingredients: ${e.message}") }
                */

                // 4. Sync Skin Bookings
                try {
                    val bookingsJson = try {
                        context.assets.open("skin_bookings.json").bufferedReader().use { it.readText() }
                    } catch (e: Exception) { null }
                    if (bookingsJson != null) {
                        firestore.forceSyncCollection("skin_bookings", bookingsJson, "id", "bookings")
                        Log.d("SyncData", "Đã đồng bộ Skin Bookings qua forceSyncCollection")
                    }
                } catch (e: Exception) { Log.e("SyncData", "Lỗi đồng bộ Skin Bookings: ${e.message}") }

                // 5. Sync Skin History
                try {
                    val historyJson = try {
                        context.assets.open("skin_history.json").bufferedReader().use { it.readText() }
                    } catch (e: Exception) { null }
                    if (historyJson != null) {
                        firestore.forceSyncCollection("skin_history", historyJson, "id")
                        Log.d("SyncData", "Đã đồng bộ Skin Histories qua forceSyncCollection")
                    }
                } catch (e: Exception) { Log.e("SyncData", "Lỗi đồng bộ Skin Histories: ${e.message}") }

                Log.d("SyncData", "ĐỒNG BỘ THÀNH CÔNG TẤT CẢ DỮ LIỆU!")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Lỗi đồng bộ: ${e.message}")
            }
        }
    }

    // Extension to convert JSONObject to Map deeply for SyncDataHelper
    private fun org.json.JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            var value = this.get(key)
            if (value is org.json.JSONArray) {
                value = value.toList()
            } else if (value is org.json.JSONObject) {
                value = value.toMap()
            }
            map[key] = value
        }
        return map
    }

    private fun org.json.JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            var value = this.get(i)
            if (value is org.json.JSONArray) {
                value = value.toList()
            } else if (value is org.json.JSONObject) {
                value = value.toMap()
            }
            list.add(value)
        }
        return list
    }
}
