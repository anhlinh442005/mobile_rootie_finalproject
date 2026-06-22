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

                // 1. Sync Explore Videos (YouTube/Handbook)
                val videos = reader.getExploreVideos()
                if (videos.isNotEmpty()) {
                    firestore.uploadAllExploreVideos(videos)
                    Log.d("SyncData", "Đã đồng bộ ${videos.size} Videos")
                }

                // 2. Sync Community Posts
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

                // 3. Sync Ingredients
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

                Log.d("SyncData", "ĐỒNG BỘ THÀNH CÔNG TẤT CẢ DỮ LIỆU!")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Lỗi đồng bộ: ${e.message}")
            }
        }
    }

    fun syncUserProfileToFirebaseAndLocal(context: Context) {
        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context)
        val username = com.veganbeauty.app.data.local.ProfileSession.getUsername(context)
        val fullName = com.veganbeauty.app.data.local.ProfileSession.getFullName(context)
        val email = com.veganbeauty.app.data.local.ProfileSession.getEmail(context)
        val phone = com.veganbeauty.app.data.local.ProfileSession.getPhone(context)
        val avatar = com.veganbeauty.app.data.local.ProfileSession.getAvatar(context)
        val address = com.veganbeauty.app.data.local.ProfileSession.getAddress(context)
        val cccd = com.veganbeauty.app.data.local.ProfileSession.getCCCD(context)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context)
                val existingUser = db.userDao().getUserByEmail(email) ?: db.userDao().getUserByPhone(phone)
                val password = existingUser?.password ?: "123456"

                val userEntity = com.veganbeauty.app.data.local.entities.UserEntity(
                    user_id = userId,
                    username = username,
                    full_name = fullName,
                    email = email,
                    phone = phone,
                    password = password,
                    avatar = avatar,
                    primary_image = existingUser?.primary_image
                )
                db.userDao().insertUser(userEntity)

                val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userMap = hashMapOf<String, Any>(
                    "username" to username,
                    "full_name" to fullName,
                    "email" to email,
                    "phone" to phone,
                    "avatar" to avatar,
                    "address" to address,
                    "cccd" to cccd
                )
                firestoreDb.collection("users").document(userId)
                    .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                    .await()

                Log.d("SyncData", "Successfully synced user profile to SQLite & Firestore for user: $userId")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Failed to sync user profile: ${e.message}")
            }
        }
    }

    fun uploadAvatarToFirebase(context: Context, fileUri: android.net.Uri, onComplete: (String?) -> Unit) {
        try {
            val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context)
            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            val avatarRef = storageRef.child("avatars/$userId.jpg")

            avatarRef.putFile(fileUri)
                .addOnSuccessListener {
                    avatarRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val downloadUrl = downloadUri.toString()
                        com.veganbeauty.app.data.local.ProfileSession.setAvatar(context, downloadUrl)
                        syncUserProfileToFirebaseAndLocal(context)
                        onComplete(downloadUrl)
                    }.addOnFailureListener { e ->
                        e.printStackTrace()
                        onComplete(null)
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    onComplete(null)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }
}
