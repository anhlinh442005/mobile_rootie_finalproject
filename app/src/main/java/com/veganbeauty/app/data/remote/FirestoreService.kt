package com.veganbeauty.app.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchAllUsers(): List<UserEntity> {
        return try {
            val snapshot = db.collection("users").get().await()
            snapshot.documents.mapNotNull { doc ->
                UserEntity(
                    userId = doc.getString("user_id") ?: doc.id,
                    username = doc.getString("username") ?: "",
                    avatarUrl = doc.getString("avatar")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAllCommunityPosts(): List<CommunityPostEntity> {
        return try {
            val snapshot = db.collection("community_posts").get().await()
            snapshot.documents.mapNotNull { doc ->
                val author = doc.get("author") as? Map<*, *>
                val authorId = author?.get("user_id") as? String ?: ""
                val authorUsername = author?.get("username") as? String ?: ""
                val authorDisplayName = author?.get("display_name") as? String ?: ""
                val authorAvatarUrl = author?.get("avatar_url") as? String

                 val media = doc.get("media") as? List<*>
                 val mediaUrls = media?.mapNotNull { (it as? Map<*, *>)?.get("url") as? String } ?: emptyList()
                 val mediaUrlsString = mediaUrls.joinToString(",")
 
                 val reactions = doc.get("reactions") as? Map<*, *>
                 val likesCount = (reactions?.get("like") as? Long)?.toInt() ?: 0
                 val linkedProductIds = (doc.get("linked_products") as? List<*>)?.joinToString(",")

                 CommunityPostEntity(
                     postId = doc.getString("post_id") ?: doc.id,
                     authorId = authorId,
                     authorUsername = authorUsername,
                     authorDisplayName = authorDisplayName,
                     authorAvatarUrl = authorAvatarUrl,
                     content = doc.getString("content") ?: "",
                     createdAt = doc.getString("created_at") ?: "",
                     likesCount = likesCount,
                     commentsCount = (doc.getLong("comments_count") ?: 0L).toInt(),
                     skinType = doc.getString("skin_type"),
                     concern = doc.getString("concern"),
                     mediaUrlsString = mediaUrlsString,
                     type = doc.getString("type"),
                     linkedProductIds = linkedProductIds
                 )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun fetchAllReels(): List<ReelEntity> {
        return try {
            val snapshot = db.collection("community_reels_fb").get().await()
            snapshot.documents.mapNotNull { doc ->
                val author = doc.get("author") as? Map<*, *>
                val authorId = author?.get("user_id") as? String ?: ""
                val authorUsername = author?.get("username") as? String ?: ""
                val authorDisplayName = author?.get("display_name") as? String ?: ""
                val authorAvatarUrl = author?.get("avatar_url") as? String

                val video = doc.get("video") as? Map<*, *>
                val thumbnailUrl = video?.get("thumbnail") as? String ?: ""

                ReelEntity(
                    videoId = doc.getString("video_id") ?: doc.id,
                    caption = doc.getString("caption") ?: "",
                    authorId = authorId,
                    authorUsername = authorUsername,
                    authorDisplayName = authorDisplayName,
                    authorAvatarUrl = authorAvatarUrl,
                    likesCount = (doc.getLong("likes_count") ?: 0L).toInt(),
                    commentsCount = (doc.getLong("comment_count") ?: 0L).toInt(),
                    shareCount = (doc.getLong("share_count") ?: 0L).toInt(),
                    thumbnailUrl = thumbnailUrl
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
