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
        val dob = com.veganbeauty.app.data.local.ProfileSession.getDob(context)
        val gender = com.veganbeauty.app.data.local.ProfileSession.getGender(context)

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context)
                val existingUser = db.userDao().getUserByIdSync(userId)
                    ?: db.userDao().getUserByEmail(email)
                    ?: db.userDao().getUserByPhone(phone)
                val password = existingUser?.password ?: "123456"
                val totalPoints = db.rewardPointDao().getAllRewardHistoryList().sumOf { it.points }

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
                    "cccd" to cccd,
                    "dob" to dob,
                    "gender" to gender,
                    "coins" to totalPoints
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

    fun syncRewardPointsToFirestore(context: Context) {
        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context)
                val history = db.rewardPointDao().getAllRewardHistoryList()
                val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                val totalPoints = history.sumOf { it.points }
                
                firestoreDb.collection("users").document(userId)
                    .update("coins", totalPoints)
                    .await()
                
                val batch = firestoreDb.batch()
                for (item in history) {
                    val ref = firestoreDb.collection("users").document(userId)
                        .collection("reward_history").document(item.id.toString())
                    val data = hashMapOf(
                        "id" to item.id,
                        "orderId" to item.orderId,
                        "points" to item.points,
                        "reason" to item.reason,
                        "timestamp" to item.timestamp
                    )
                    batch.set(ref, data, com.google.firebase.firestore.SetOptions.merge())
                }
                batch.commit().await()
                Log.d("SyncData", "Successfully synced reward points to Firestore for user: $userId (Total: $totalPoints)")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Failed to sync reward points: ${e.message}")
            }
        }
    }

    fun syncRewardPointsFromFirestore(context: Context) {
        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context)
        if (userId.isBlank()) return
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context)
                val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                
                val snapshot = firestoreDb.collection("users").document(userId)
                    .collection("reward_history")
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val id = doc.getLong("id")?.toInt() ?: continue
                        val orderId = doc.getString("orderId") ?: ""
                        val points = doc.getLong("points")?.toInt() ?: 0
                        val reason = doc.getString("reason") ?: ""
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        
                        val entity = com.veganbeauty.app.data.local.entities.RewardPointEntity(
                            id = id,
                            orderId = orderId,
                            points = points,
                            reason = reason,
                            timestamp = timestamp
                        )
                        db.rewardPointDao().insertRewardPoints(entity)
                    }
                    Log.d("SyncData", "Successfully pulled/synced reward points FROM Firestore: ${snapshot.size()} items for user $userId.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Failed to sync reward points from Firestore: ${e.message}")
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

    @JvmOverloads
    fun syncUserProfileFromFirestore(context: Context, onComplete: Runnable? = null) {
        val userId = com.veganbeauty.app.data.local.ProfileSession.getUserId(context)
        if (userId.isBlank()) {
            onComplete?.run()
            return
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val doc = firestoreDb.collection("users").document(userId).get().await()
                if (doc.exists()) {
                    val username = doc.getString("username") ?: ""
                    val fullName = doc.getString("full_name") ?: ""
                    val email = doc.getString("email") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val avatar = doc.getString("avatar") ?: ""
                    val address = doc.getString("address") ?: ""
                    val cccd = doc.getString("cccd") ?: ""
                    val dob = doc.getString("dob") ?: ""
                    val gender = doc.getString("gender") ?: ""

                    // Update ProfileSession
                    com.veganbeauty.app.data.local.ProfileSession.setUserId(context, userId)
                    if (username.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setUsername(context, username)
                    if (fullName.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setFullName(context, fullName)
                    if (email.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setEmail(context, email)
                    if (phone.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setPhone(context, phone)
                    if (avatar.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setAvatar(context, avatar)
                    if (address.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setAddress(context, address)
                    if (cccd.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setCCCD(context, cccd)
                    if (dob.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setDob(context, dob)
                    if (gender.isNotBlank()) com.veganbeauty.app.data.local.ProfileSession.setGender(context, gender)

                    // Update SQLite UserDao
                    val db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(context)
                    val existingUser = db.userDao().getUserByIdSync(userId)
                        ?: db.userDao().getUserByEmail(email)
                        ?: db.userDao().getUserByPhone(phone)
                    val password = existingUser?.password ?: "123456"
                    val primaryImage = existingUser?.primary_image ?: (if (avatar.isNotBlank()) avatar else "")

                    val userEntity = com.veganbeauty.app.data.local.entities.UserEntity(
                        user_id = userId,
                        username = if (username.isNotBlank()) username else (existingUser?.username ?: ""),
                        full_name = if (fullName.isNotBlank()) fullName else (existingUser?.full_name ?: ""),
                        email = if (email.isNotBlank()) email else (existingUser?.email ?: ""),
                        phone = if (phone.isNotBlank()) phone else (existingUser?.phone ?: ""),
                        password = password,
                        avatar = if (avatar.isNotBlank()) avatar else (existingUser?.avatar ?: ""),
                        primary_image = primaryImage
                    )
                    db.userDao().insertUser(userEntity)
                    Log.d("SyncData", "Successfully synced user profile FROM Firestore for user $userId.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SyncData", "Failed to sync user profile from Firestore: ${e.message}")
            } finally {
                if (onComplete != null) {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        onComplete.run()
                    }
                }
            }
        }
    }
}
