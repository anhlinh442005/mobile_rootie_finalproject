package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.dao.CommunityDao
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.data.local.entities.UserEntity
import com.veganbeauty.app.data.local.entities.YtVideoEntity
import com.veganbeauty.app.data.local.entities.IngredientEntity
import com.veganbeauty.app.data.local.entities.CommunityBlogEntity
import com.veganbeauty.app.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

class CommunityRepository(
    private val communityDao: CommunityDao,
    private val localJsonReader: LocalJsonReader,
    private val firestoreService: FirestoreService
) {
    // Single Source of Truth
    val allPosts: Flow<List<CommunityPostEntity>> = communityDao.getAllPosts()
    val allUsers: Flow<List<UserEntity>> = communityDao.getAllUsers()
    val allReels: Flow<List<ReelEntity>> = communityDao.getAllReels()
    val exploreVideos: Flow<List<YtVideoEntity>> = communityDao.getAllExploreVideos()
    val allIngredients: Flow<List<IngredientEntity>> = communityDao.getAllIngredients()
    val allBlogs: Flow<List<CommunityBlogEntity>> = communityDao.getAllBlogs()

    // Sync Data
    suspend fun refreshCommunityData() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // UNCONDITIONALLY WIPE ALL LOCAL DATABASE TABLES FOR A CLEAN SYNC
                communityDao.deleteAllPosts()
                communityDao.deleteAllUsers()
                communityDao.deleteAllExploreVideos()

                // 1. Always load local assets first (fresh data from JSON files)
                loadFromLocalAssets()
                
                // TẠM THỜI TẮT ĐỒNG BỘ CÁC BẢNG KHÁC THEO YÊU CẦU
                /*
                try {
                    firestoreService.forceSyncLocalPostsToFirebase(
                        localJsonReader.getRawPostsJson(),
                        localJsonReader.getRawNewsJson()
                    )
                } catch (e: Exception) { e.printStackTrace() }
                
                try { firestoreService.forceSyncCollection("community_reels_fb", localJsonReader.getRawReelsJson(), "video_id") } catch (e: Exception) { e.printStackTrace() }
                try { firestoreService.forceSyncCollection("ingredients", localJsonReader.getRawIngredientsJson(), "slug") } catch (e: Exception) { e.printStackTrace() }
                try { firestoreService.forceSyncCollection("products", localJsonReader.getRawProductsJson(), "id", "products") } catch (e: Exception) { e.printStackTrace() }
                */
                
                // MỞ LẠI ĐỒNG BỘ CHO USERS (Vì vừa sửa SĐT), CÙNG VỚI SKIN_HISTORY VÀ SKIN_BOOKINGS
                try { firestoreService.forceSyncCollection("users", localJsonReader.getRawUsersJson(), "user_id") } catch (e: Exception) { e.printStackTrace() }
                try { firestoreService.forceSyncCollection("skin_history", localJsonReader.getRawSkinHistoryJson(), "id") } catch (e: Exception) { e.printStackTrace() }
                try { firestoreService.forceSyncCollection("skin_bookings", localJsonReader.getRawSkinBookingsJson(), "id", "bookings") } catch (e: Exception) { e.printStackTrace() }

                // NOTE: We do NOT fetch posts/users back from Firebase.
                // Local JSON files are the SINGLE SOURCE OF TRUTH for posts and users.
                // Firebase is used only for: (1) real-time comment sync, (2) backup.
                // Fetching back from Firebase would overwrite fresh JSON data with potentially stale Firebase data.

            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error, local assets already loaded above
            }
        }
    }

    private suspend fun loadFromLocalAssets() {
        try {
            val localUsers = localJsonReader.getUsers()
            val localPosts = localJsonReader.getCommunityPosts().map { post ->
                val matchedUser = localUsers.find { it.user_id == post.authorId || it.username.equals(post.authorUsername, ignoreCase = true) }
                if (matchedUser?.avatar?.isNotEmpty() == true) {
                    post.copy(authorAvatarUrl = matchedUser.avatar)
                } else post
            }
            val rawLocalReels = localJsonReader.getReels()
            val localReels = rawLocalReels.map { reel ->
                val matchedUser = localUsers.find { it.user_id == reel.authorId || it.username.equals(reel.authorUsername, ignoreCase = true) }
                if (matchedUser?.avatar?.isNotEmpty() == true) {
                    reel.copy(authorAvatarUrl = matchedUser.avatar)
                } else reel
            }
            
            val rawLocalVideos = localJsonReader.getExploreVideos()
            val localVideos = rawLocalVideos.map { video ->
                val matchedUser = localUsers.find { it.username.equals(video.username, ignoreCase = true) }
                if (matchedUser?.avatar?.isNotEmpty() == true) {
                    video.copy(avatarUrl = matchedUser.avatar)
                } else video
            }

            if (localUsers.isNotEmpty()) {
                communityDao.deleteAllUsers()
                communityDao.insertUsers(localUsers)
            }
            if (localPosts.isNotEmpty()) {
                communityDao.deleteAllPosts()
                communityDao.insertPosts(localPosts)
            }
            if (localReels.isNotEmpty()) communityDao.insertReels(localReels)
            if (localVideos.isNotEmpty()) {
                communityDao.deleteAllExploreVideos()
                communityDao.insertExploreVideos(localVideos)
            }
            
            val localIngredients = localJsonReader.getIngredients()
            // Lưu ý: Blog không load ở đây nữa - BeautyHubFragment tự lazy load từ file (tránh OOM với file 174MB)
            if (localIngredients.isNotEmpty()) communityDao.insertIngredients(localIngredients)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
