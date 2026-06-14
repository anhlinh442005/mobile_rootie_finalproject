package com.veganbeauty.app.data.repository

import com.veganbeauty.app.data.local.dao.StoreDao
import com.veganbeauty.app.data.local.entities.StoreEntity
import com.veganbeauty.app.data.remote.FirestoreService
import com.veganbeauty.app.data.local.LocalJsonReader
import kotlinx.coroutines.flow.Flow

class StoreRepository(
    private val storeDao: StoreDao,
    private val localJsonReader: LocalJsonReader,
    private val firestoreService: FirestoreService = FirestoreService()
) {
    val allStores: Flow<List<StoreEntity>> = storeDao.getAllStores()

    suspend fun refreshStores() {
        try {
            val remoteStores = firestoreService.fetchAllStores()
            if (remoteStores.isNotEmpty()) {
                storeDao.insertStores(remoteStores)
            } else {
                val localStores = localJsonReader.getAllStores()
                if (localStores.isNotEmpty()) {
                    storeDao.insertStores(localStores)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val localStores = localJsonReader.getAllStores()
                if (localStores.isNotEmpty()) {
                    storeDao.insertStores(localStores)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
