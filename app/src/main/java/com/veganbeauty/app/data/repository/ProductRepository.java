package com.veganbeauty.app.data.repository;



import com.veganbeauty.app.data.local.LocalJsonReader;

import com.veganbeauty.app.data.local.dao.ProductDao;

import com.veganbeauty.app.data.local.dao.UserProductExpiryDao;

import com.veganbeauty.app.data.local.entities.ProductEntity;

import com.veganbeauty.app.data.local.entities.UserProductExpiryEntity;

import com.veganbeauty.app.data.remote.FirestoreService;

import com.veganbeauty.app.utils.ProductImageCache;



import java.util.ArrayList;

import java.util.HashMap;

import java.util.List;

import java.util.Map;



import kotlinx.coroutines.flow.Flow;



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

    public List<ProductEntity> getAllProductsSync() {
        List<ProductEntity> fromDb = productDao.getAllProductsSync();
        if (fromDb != null && !fromDb.isEmpty()) {
            return fromDb;
        }
        List<ProductEntity> fromAssets = localJsonReader.getAllProducts();
        return fromAssets != null ? fromAssets : new ArrayList<>();
    }

    public List<ProductEntity> ensureProductsLoaded() {
        refreshProducts(true);
        return getAllProductsSync();
    }



    public LocalJsonReader getLocalJsonReader() {

        return localJsonReader;

    }



    public Flow<List<UserProductExpiryEntity>> getExpiryProductsForUser(String userId) {

        if (userProductExpiryDao == null) {

            throw new IllegalStateException("UserProductExpiryDao must be provided");

        }

        return userProductExpiryDao.getProductsByUserIdFlow(userId);

    }



    public ProductEntity getExpiryProductById(String userId, String productId) {

        if (userProductExpiryDao == null) {

            throw new IllegalStateException("UserProductExpiryDao must be provided");

        }

        UserProductExpiryEntity entity = userProductExpiryDao.getProductById(userId, productId);

        return entity != null ? entity.toProductEntity() : null;

    }



    public void deleteExpiryProduct(String userId, String productId) {

        if (userProductExpiryDao == null) {

            throw new IllegalStateException("UserProductExpiryDao must be provided");

        }

        userProductExpiryDao.deleteUserProductExpiry(userId, productId);

    }



    public void seedExpiryProductsIfEmpty(String userId) {

        if (userProductExpiryDao == null) return;

        if (userProductExpiryDao.getProductCountByUserId(userId) == 0) {

            List<UserProductExpiryEntity> mockList = new ArrayList<>();

            mockList.add(new UserProductExpiryEntity(

                    0,

                    userId,

                    "0b8fadbc1bd44562f75704e6",

                    "Nước tẩy trang sen Hậu Giang 500ml",

                    "https://image.cocoonvietnam.com/uploads/Avatar_Website_Nuoc_tay_trang_sen_Artboard_7_copy_ac0bf66b46.jpg",

                    "Cocoon Vietnam",

                    309000,

                    "Chăm sóc da",

                    "101106",

                    555,

                    "10/06/2026"

            ));

            mockList.add(new UserProductExpiryEntity(

                    0,

                    userId,

                    "dd23909f6a123054c8cf62f4",

                    "Dầu tẩy trang hoa hồng 310ml",

                    "https://image.cocoonvietnam.com/uploads/Template_Website_Dau_Tay_Trang_310ml_b098c76143.jpg",

                    "Cocoon Vietnam",

                    339000,

                    "Chăm sóc da",

                    "101110",

                    962,

                    "04/07/2026"

            ));

            mockList.add(new UserProductExpiryEntity(

                    0,

                    userId,

                    "dc312be5eec4d740ae26acbb",

                    "Nước tẩy trang bí đao 500ml",

                    "https://image.cocoonvietnam.com/uploads/z4394607766854_2aca12462b79836bb49c3bf9aeef6bd1_1_fe9bcfe8db.jpg",

                    "Cocoon Vietnam",

                    299000,

                    "Chăm sóc da",

                    "101108",

                    999,

                    "15/09/2026"

            ));

            mockList.add(new UserProductExpiryEntity(

                    0,

                    userId,

                    "812ea7faa15bac41e52d4170",

                    "Sữa rửa mặt sen Hậu Giang 310ml",

                    "https://image.cocoonvietnam.com/uploads/Avatar_1_dcfa1bbf4e.jpg",

                    "Cocoon Vietnam",

                    339000,

                    "Chăm sóc da",

                    "101114",

                    479,

                    "20/05/2026"

            ));

            userProductExpiryDao.insertUserProducts(mockList);

        }

    }



    public ProductEntity getProductByBarcode(String barcode) {

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

    }



    public void seedProductsFromAssets() {
        try {
            int count = productDao.getProductCount();
            if (count == 0) {
                List<ProductEntity> localProducts = localJsonReader.getAllProducts();
                if (localProducts != null && !localProducts.isEmpty()) {
                    productDao.insertProducts(localProducts);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void refreshProducts(boolean force) {
        try {
            // Fetch live products from Firebase Firestore
            List<ProductEntity> remoteProducts = firestoreService.fetchAllProducts();
            if (remoteProducts != null && !remoteProducts.isEmpty()) {
                productDao.insertProducts(remoteProducts);
            } else {
                // Fallback to assets JSON only if remote fetch failed/empty
                List<ProductEntity> assetProducts = localJsonReader.getAllProducts();
                if (assetProducts != null && !assetProducts.isEmpty()) {
                    List<ProductEntity> dbProducts = productDao.getAllProductsSync();
                    if (dbProducts == null || dbProducts.isEmpty()) {
                        productDao.insertProducts(assetProducts);
                    } else {
                        List<ProductEntity> toInsert = new ArrayList<>();
                        for (ProductEntity asset : assetProducts) {
                            ProductEntity existing = productDao.getProductById(asset.getId());
                            if (existing == null) {
                                toInsert.add(asset);
                            } else {
                                asset.setStock(existing.getStock());
                                asset.setSold(existing.getSold());
                                toInsert.add(asset);
                            }
                        }
                        if (!toInsert.isEmpty()) {
                            productDao.insertProducts(toInsert);
                        }
                    }
                }
            }

            try {
                List<ProductEntity> assetProducts = localJsonReader.getAllProducts();
                if (assetProducts != null) {
                    syncAllProductImagesFromAssets(assetProducts);
                }
            } catch (Exception imgEx) {
                // Ignore image sync error if JSON is deleted
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    public void backfillProductImagesFromAssets(List<ProductEntity> assetProducts) {

        syncAllProductImagesFromAssets(assetProducts);

    }



    public void syncAllProductImagesFromAssets(List<ProductEntity> assetProducts) {

        if (assetProducts == null || assetProducts.isEmpty()) {

            assetProducts = localJsonReader.getAllProducts();

        }

        if (assetProducts == null || assetProducts.isEmpty()) return;



        Map<String, ProductEntity> assetById = new HashMap<>();

        Map<String, ProductEntity> assetBySku = new HashMap<>();

        for (ProductEntity asset : assetProducts) {

            if (asset.getId() != null && !asset.getId().isEmpty()) {

                assetById.put(asset.getId(), asset);

            }

            if (asset.getSku() != null && !asset.getSku().isEmpty()) {

                assetBySku.put(asset.getSku(), asset);

            }

        }



        List<ProductEntity> dbProducts = productDao.getAllProductsSync();

        if (dbProducts == null || dbProducts.isEmpty()) return;



        List<ProductEntity> toUpdate = new ArrayList<>();

        for (ProductEntity dbProduct : dbProducts) {

            ProductEntity asset = assetById.get(dbProduct.getId());

            if (asset == null && dbProduct.getSku() != null) {

                asset = assetBySku.get(dbProduct.getSku());

            }

            if (asset == null) continue;

            if (applyAssetImages(dbProduct, asset)) {

                toUpdate.add(dbProduct);

            }

        }



        if (!toUpdate.isEmpty()) {

            productDao.insertProducts(toUpdate);

        }

    }



    private void mergeProductImages(List<ProductEntity> remoteProducts, List<ProductEntity> assetProducts) {

        if (assetProducts == null || assetProducts.isEmpty()) {

            assetProducts = localJsonReader.getAllProducts();

        }

        Map<String, ProductEntity> assetById = new HashMap<>();

        Map<String, ProductEntity> assetBySku = new HashMap<>();

        if (assetProducts != null) {

            for (ProductEntity asset : assetProducts) {

                if (asset.getId() != null && !asset.getId().isEmpty()) {

                    assetById.put(asset.getId(), asset);

                }

                if (asset.getSku() != null && !asset.getSku().isEmpty()) {

                    assetBySku.put(asset.getSku(), asset);

                }

            }

        }



        for (ProductEntity remote : remoteProducts) {

            ProductEntity asset = assetById.get(remote.getId());

            if (asset == null && remote.getSku() != null) {

                asset = assetBySku.get(remote.getSku());

            }



            if (asset != null) {

                applyAssetImages(remote, asset);

                continue;

            }



            ProductEntity existing = productDao.getProductById(remote.getId());

            if (existing != null) {

                applyAssetImages(remote, existing);

            }

        }

    }



    private boolean applyAssetImages(ProductEntity target, ProductEntity source) {

        boolean changed = false;

        String sourceImage = resolveBestImageUrl(source);

        if (!ProductImageCache.isValidImageUrl(target.getMainImage()) && ProductImageCache.isValidImageUrl(sourceImage)) {

            target.setMainImage(sourceImage);

            changed = true;

        }



        List<String> targetAlbum = target.getAlbum();

        List<String> sourceAlbum = source.getAlbum();

        if ((targetAlbum == null || targetAlbum.isEmpty()) && sourceAlbum != null && !sourceAlbum.isEmpty()) {

            target.setAlbum(new ArrayList<>(sourceAlbum));

            changed = true;

        }



        if (!ProductImageCache.isValidImageUrl(target.getMainImage())) {

            String fallback = resolveBestImageUrl(target);

            if (ProductImageCache.isValidImageUrl(fallback)) {

                target.setMainImage(fallback);

                changed = true;

            }

        }

        return changed;

    }



    private String resolveBestImageUrl(ProductEntity product) {

        if (product == null) return "";

        if (ProductImageCache.isValidImageUrl(product.getMainImage())) {

            return product.getMainImage().trim();

        }

        List<String> album = product.getAlbum();

        if (album != null) {

            for (String item : album) {

                if (ProductImageCache.isValidImageUrl(item)) {

                    return item.trim();

                }

            }

        }

        return "";

    }

}


