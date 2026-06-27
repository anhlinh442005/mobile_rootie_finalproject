package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.dao.StoreDao;
import com.veganbeauty.app.data.local.entities.StoreEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.local.LocalJsonReader;

import java.util.List;
import kotlinx.coroutines.flow.Flow;

public class StoreRepository {
    private final StoreDao storeDao;
    private final LocalJsonReader localJsonReader;
    private final FirestoreService firestoreService;
    private final Flow<List<StoreEntity>> allStores;

    public StoreRepository(StoreDao storeDao, LocalJsonReader localJsonReader, FirestoreService firestoreService) {
        this.storeDao = storeDao;
        this.localJsonReader = localJsonReader;
        this.firestoreService = firestoreService;
        this.allStores = storeDao.getAllStores();
    }

    public StoreRepository(StoreDao storeDao, LocalJsonReader localJsonReader) {
        this(storeDao, localJsonReader, new FirestoreService());
    }

    public Flow<List<StoreEntity>> getAllStores() {
        return allStores;
    }

    public void refreshStores() {
        // Translation for suspend function logic.
        new Thread(() -> {
            try {
                List<StoreEntity> remoteStores = firestoreService.fetchAllStores();
                if (remoteStores != null && !remoteStores.isEmpty()) {
                    storeDao.insertStores(remoteStores);
                } else {
                    List<StoreEntity> localStores = localJsonReader.getAllStores();
                    if (localStores != null && !localStores.isEmpty()) {
                        storeDao.insertStores(localStores);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    List<StoreEntity> localStores = localJsonReader.getAllStores();
                    if (localStores != null && !localStores.isEmpty()) {
                        storeDao.insertStores(localStores);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}
