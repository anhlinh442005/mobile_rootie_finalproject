package com.veganbeauty.app.data.repository;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.dao.ProductDao;
import com.veganbeauty.app.data.local.dao.UserProductExpiryDao;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;
import com.veganbeauty.app.data.remote.FirestoreService;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

public class ProductRepository {

    private final ProductDao productDao;
    private final LocalJsonReader localJsonReader;
    private final FirestoreService firestoreService;
    private final UserProductExpiryDao userProductExpiryDao;

    private static long lastSyncTime = 0L;
    private static final long CACHE_DURATION = 5 * 60 * 1000L; // 5 minutes

    public ProductRepository(
            ProductDao productDao,
            LocalJsonReader localJsonReader,
            FirestoreService firestoreService,
            UserProductExpiryDao userProductExpiryDao
    ) {
        this.productDao = productDao;
        this.localJsonReader = localJsonReader;
        this.firestoreService = firestoreService != null ? firestoreService : new FirestoreService();
        this.userProductExpiryDao = userProductExpiryDao;
    }

    public ProductRepository(ProductDao productDao, LocalJsonReader localJsonReader) {
        this(productDao, localJsonReader, new FirestoreService(), null);
    }

    public Flow<List<ProductEntity>> getAllProducts() {
        return productDao.getAllProducts();
    }

    public Flow<List<ProductEntity>> getExpiryProductsForUser(String userId) {
        if (userProductExpiryDao == null) {
            throw new IllegalStateException("UserProductExpiryDao must be provided");
        }
        return FlowKt.map(userProductExpiryDao.getProductsByUserIdFlow(userId), list -> {
            List<ProductEntity> result = new ArrayList<>();
            for (UserProductExpiryEntity entity : list) {
                result.add(entity.toProductEntity());
            }
            return result;
        });
    }

    public Object getExpiryProductById(String userId, String productId, kotlin.coroutines.Continuation<? super ProductEntity> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(kotlinx.coroutines.Dispatchers.getIO(), (scope, suspendContinuation) -> {
            if (userProductExpiryDao == null) {
                throw new IllegalStateException("UserProductExpiryDao must be provided");
            }
            UserProductExpiryEntity entity = userProductExpiryDao.getProductById(userId, productId);
            return entity != null ? entity.toProductEntity() : null;
        }, continuation);
    }

    public Object deleteExpiryProduct(String userId, String productId, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(kotlinx.coroutines.Dispatchers.getIO(), (scope, suspendContinuation) -> {
            if (userProductExpiryDao == null) {
                throw new IllegalStateException("UserProductExpiryDao must be provided");
            }
            userProductExpiryDao.deleteUserProductExpiry(userId, productId);
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    public Object seedExpiryProductsIfEmpty(String userId, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(kotlinx.coroutines.Dispatchers.getIO(), (scope, suspendContinuation) -> {
            if (userProductExpiryDao == null) return kotlin.Unit.INSTANCE;
            if (userProductExpiryDao.getProductCountByUserId(userId) == 0) {
                List<UserProductExpiryEntity> mockList = new ArrayList<>();
                mockList.add(new UserProductExpiryEntity(
                        userId,
                        "0b8fadbc1bd44562f75704e6",
                        "Nước tẩy trang sen Hậu Giang 500ml",
                        "https://image.cocoonvietnam.com/uploads/Avatar_Website_Nuoc_tay_trang_sen_Artboard_7_copy_ac0bf66b46.jpg",
                        "Cocoon Vietnam",
                        309000,
                        "Chăm sóc da",
                        "101106",
                        555,
                        "10/06/2026",
                        0
                ));
                mockList.add(new UserProductExpiryEntity(
                        userId,
                        "dd23909f6a123054c8cf62f4",
                        "Dầu tẩy trang hoa hồng 310ml",
                        "https://image.cocoonvietnam.com/uploads/Template_Website_Dau_Tay_Trang_310ml_b098c76143.jpg",
                        "Cocoon Vietnam",
                        339000,
                        "Chăm sóc da",
                        "101110",
                        962,
                        "04/07/2026",
                        0
                ));
                mockList.add(new UserProductExpiryEntity(
                        userId,
                        "dc312be5eec4d740ae26acbb",
                        "Nước tẩy trang bí đao 500ml",
                        "https://image.cocoonvietnam.com/uploads/z4394607766854_2aca12462b79836bb49c3bf9aeef6bd1_1_fe9bcfe8db.jpg",
                        "Cocoon Vietnam",
                        299000,
                        "Chăm sóc da",
                        "101108",
                        999,
                        "15/09/2026",
                        0
                ));
                mockList.add(new UserProductExpiryEntity(
                        userId,
                        "812ea7faa15bac41e52d4170",
                        "Sữa rửa mặt sen Hậu Giang 310ml",
                        "https://image.cocoonvietnam.com/uploads/Avatar_1_dcfa1bbf4e.jpg",
                        "Cocoon Vietnam",
                        339000,
                        "Chăm sóc da",
                        "101114",
                        479,
                        "20/05/2026",
                        0
                ));
                userProductExpiryDao.insertUserProducts(mockList);
            }
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }

    public Object getProductByBarcode(String barcode, kotlin.coroutines.Continuation<? super ProductEntity> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(kotlinx.coroutines.Dispatchers.getIO(), (scope, suspendContinuation) -> {
            String normalized = barcode != null ? barcode.trim() : "";
            if (normalized.isEmpty()) return null;

            ProductEntity localEntity = productDao.getProductByBarcode(normalized);
            if (localEntity != null) return localEntity;

            try {
                ProductEntity remote = firestoreService.fetchProductByBarcode(normalized);
                if (remote != null) {
                    List<ProductEntity> list = new ArrayList<>();
                    list.add(remote);
                    productDao.insertProducts(list);
                }
                return remote;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }, continuation);
    }

    public Object refreshProducts(boolean force, kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return kotlinx.coroutines.BuildersKt.withContext(kotlinx.coroutines.Dispatchers.getIO(), (scope, suspendContinuation) -> {
            long currentTime = System.currentTimeMillis();
            int count = productDao.getProductCount();

            if (count == 0) {
                try {
                    List<ProductEntity> localProducts = localJsonReader.getAllProducts();
                    if (localProducts != null && !localProducts.isEmpty()) {
                        productDao.insertProducts(localProducts);
                        count = productDao.getProductCount();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (!force && count > 0 && (currentTime - lastSyncTime < CACHE_DURATION)) {
                return kotlin.Unit.INSTANCE;
            }

            try {
                List<ProductEntity> remoteProducts = firestoreService.fetchAllProducts();
                if (remoteProducts != null && !remoteProducts.isEmpty()) {
                    productDao.insertProducts(remoteProducts);
                    lastSyncTime = currentTime;
                } else {
                    List<ProductEntity> localProducts = localJsonReader.getAllProducts();
                    if (localProducts != null && !localProducts.isEmpty()) {
                        productDao.insertProducts(localProducts);
                        lastSyncTime = currentTime;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    List<ProductEntity> localProducts = localJsonReader.getAllProducts();
                    if (localProducts != null && !localProducts.isEmpty()) {
                        productDao.insertProducts(localProducts);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return kotlin.Unit.INSTANCE;
        }, continuation);
    }
}
