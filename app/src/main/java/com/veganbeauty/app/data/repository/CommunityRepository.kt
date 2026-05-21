package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.LocalJsonReader
import com.veganbeauty.app.data.local.dao.CommunityDao
import com.veganbeauty.app.data.local.entities.CommunityPostEntity
import com.veganbeauty.app.data.local.entities.ReelEntity
import com.veganbeauty.app.data.local.entities.UserEntity
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

    // Sync Data
    suspend fun refreshCommunityData() {
        try {
            // 1. Try to sync from Firebase Firestore
            val remoteUsers = firestoreService.fetchAllUsers()
            val remotePosts = firestoreService.fetchAllCommunityPosts()
            val remoteReels = firestoreService.fetchAllReels()

            if (remoteUsers.isNotEmpty()) {
                communityDao.insertUsers(remoteUsers)
            }
            if (remotePosts.isNotEmpty()) {
                communityDao.insertPosts(remotePosts)
            }
            if (remoteReels.isNotEmpty()) {
                communityDao.insertReels(remoteReels)
            }

            // If remote is completely empty, fall back to local assets
            if (remoteUsers.isEmpty() && remotePosts.isEmpty() && remoteReels.isEmpty()) {
                loadFromLocalAssets()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of error (e.g. no connection), fall back to local JSON
            loadFromLocalAssets()
        }
    }

    private suspend fun loadFromLocalAssets() {
        try {
            val localUsers = localJsonReader.getUsers()
            val localPosts = localJsonReader.getCommunityPosts()
            val localReels = localJsonReader.getReels()

            if (localUsers.isNotEmpty()) communityDao.insertUsers(localUsers)
            if (localPosts.isNotEmpty()) communityDao.insertPosts(localPosts)
            if (localReels.isNotEmpty()) communityDao.insertReels(localReels)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
