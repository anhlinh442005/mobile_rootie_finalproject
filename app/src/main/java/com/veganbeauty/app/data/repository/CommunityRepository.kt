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
        try {
            // 1. Always load local assets first (fresh data from JSON files)
            loadFromLocalAssets()
            
            // Temporary sync: Upload local JSON to Firestore so it gets updated
            firestoreService.uploadAllExploreVideos(localJsonReader.getExploreVideos())

            // 2. Then try to sync from Firebase Firestore (overrides local if available)
            val remoteUsers = firestoreService.fetchAllUsers()
            val remotePosts = firestoreService.fetchAllCommunityPosts()
            val remoteReels = firestoreService.fetchAllReels()
            
            // Lấy data từ Firebase (bị comment lại để không ghi đè JSON mới ở máy)
            // val rawRemoteVideos = firestoreService.fetchAllExploreVideos()
            val rawRemoteVideos = emptyList<YtVideoEntity>()
            val remoteVideos = rawRemoteVideos.map { video ->
                val matchedUser = remoteUsers.find { it.username.equals(video.username, ignoreCase = true) }
                if (matchedUser?.avatar?.isNotEmpty() == true) {
                    video.copy(avatarUrl = matchedUser.avatar)
                } else video
            }
            
            val remoteIngredients = firestoreService.fetchAllIngredients()

            if (remoteUsers.isNotEmpty()) {
                communityDao.insertUsers(remoteUsers)
            }
            if (remotePosts.isNotEmpty()) {
                communityDao.insertPosts(remotePosts)
            }
            if (remoteReels.isNotEmpty()) {
                communityDao.insertReels(remoteReels)
            }
            if (remoteVideos.isNotEmpty()) {
                communityDao.deleteAllExploreVideos()
                communityDao.insertExploreVideos(remoteVideos)
            }
            if (remoteIngredients.isNotEmpty()) {
                communityDao.insertIngredients(remoteIngredients)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error, local assets already loaded above
        }
    }

    private suspend fun loadFromLocalAssets() {
        try {
            val localUsers = localJsonReader.getUsers()
            val localPosts = localJsonReader.getCommunityPosts()
            val localReels = localJsonReader.getReels()
            
            val rawLocalVideos = localJsonReader.getExploreVideos()
            val localVideos = rawLocalVideos.map { video ->
                val matchedUser = localUsers.find { it.username.equals(video.username, ignoreCase = true) }
                if (matchedUser?.avatar?.isNotEmpty() == true) {
                    video.copy(avatarUrl = matchedUser.avatar)
                } else video
            }

            if (localUsers.isNotEmpty()) communityDao.insertUsers(localUsers)
            if (localPosts.isNotEmpty()) communityDao.insertPosts(localPosts)
            if (localReels.isNotEmpty()) communityDao.insertReels(localReels)
            if (localVideos.isNotEmpty()) {
                communityDao.deleteAllExploreVideos()
                communityDao.insertExploreVideos(localVideos)
            }
            
            val localIngredients = localJsonReader.getIngredients()
            val localBlogs = localJsonReader.getCommunityBlogs(10)
            if (localIngredients.isNotEmpty()) communityDao.insertIngredients(localIngredients)
            if (localBlogs.isNotEmpty()) communityDao.insertBlogs(localBlogs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
