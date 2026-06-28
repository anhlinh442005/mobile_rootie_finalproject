package com.veganbeauty.app.features.shop.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieViewModel;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.features.shop.home.models.BannerUiModel;
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShopHomeViewModel extends RootieViewModel {

    private static final List<String> SKINCARE_IDS = Arrays.asList(
            "f5877af6a55f88bcf57c17b4", "389971929086b2ce7fba9dd0",
            "36cbf3f5c4b7a299ce2a2d0c", "4e20d6bbc1203015ee2ecd48",
            "b1b6cd208332d4f1e015a26c", "7667d982515426a9d88b787b",
            "bb88a3306cf95af20d073594", "9882d5fa14c74dd053e17f33",
            "c211afa24702f5d1ff86fe42"
    );
    private static final List<String> BODY_IDS = Arrays.asList(
            "7c70e845e829b374e57ee7b1", "b703bb813e660aa88076ee5a",
            "8fce5340c618672aa1ae7fb3"
    );
    private static final List<String> LIPS_IDS = Arrays.asList(
            "755731e01d8c579c633ae4d2", "ded17e0716783c133b1a5b9a"
    );
    private static final List<String> COMBO_IDS = Arrays.asList(
            "7176b5e7966be88daf95cfd4", "f40c1f05dcf4059f25fb89a1",
            "e0754dabb88699e92481e123", "bd1c0ff76b19b1b5a3130a79"
    );

    private final ProductRepository productRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<BannerUiModel>> _banners = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<BannerUiModel>> banners = _banners;

    private final MutableLiveData<List<CategoryUiModel>> _categories = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<CategoryUiModel>> categories = _categories;

    private final MutableLiveData<List<ProductEntity>> _suggestedProducts = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<ProductEntity>> suggestedProducts = _suggestedProducts;

    private androidx.lifecycle.Observer<List<ProductEntity>> observer;
    private LiveData<List<ProductEntity>> allProductsLive;

    public ShopHomeViewModel(ProductRepository productRepository) {
        this.productRepository = productRepository;
        applyDefaultUi();
        refreshProducts();
        observeProductCounts();
    }

    public void preloadUiData(List<ProductEntity> localProducts) {
        List<ProductEntity> products = localProducts != null ? localProducts : new ArrayList<>();
        _banners.postValue(buildBanners());
        _categories.postValue(buildCategories(products));
        _suggestedProducts.postValue(products);
    }

    private void applyDefaultUi() {
        _banners.setValue(buildBanners());
        _categories.setValue(buildCategories(new ArrayList<>()));
    }

    private static List<BannerUiModel> buildBanners() {
        List<BannerUiModel> bannerList = new ArrayList<>();
        bannerList.add(new BannerUiModel("1", R.drawable.shop_banner_1));
        bannerList.add(new BannerUiModel("2", R.drawable.shop_banner_2));
        bannerList.add(new BannerUiModel("3", R.drawable.shop_banner_3));
        return bannerList;
    }

    private static List<CategoryUiModel> buildCategories(List<ProductEntity> products) {
        int skincareCount = 0;
        int bodyCount = 0;
        int lipsCount = 0;
        int comboCount = 0;

        if (products != null) {
            for (ProductEntity product : products) {
                String[] productCategoryIds = product.getCategoryIds() != null
                        ? product.getCategoryIds().split(",")
                        : new String[0];
                boolean isSkincare = false;
                boolean isBody = false;
                boolean isLips = false;
                boolean isCombo = false;

                for (String id : productCategoryIds) {
                    String trimmed = id.trim();
                    if (SKINCARE_IDS.contains(trimmed)) isSkincare = true;
                    if (BODY_IDS.contains(trimmed)) isBody = true;
                    if (LIPS_IDS.contains(trimmed)) isLips = true;
                    if (COMBO_IDS.contains(trimmed)) isCombo = true;
                }

                if (isSkincare) skincareCount++;
                if (isBody) bodyCount++;
                if (isLips) lipsCount++;
                if (isCombo) comboCount++;
            }
        }

        List<CategoryUiModel> categoryList = new ArrayList<>();
        categoryList.add(new CategoryUiModel("c1", "Chăm sóc da", R.drawable.ic_skincare, skincareCount));
        categoryList.add(new CategoryUiModel("c2", "Tắm & Dưỡng thể", R.drawable.ic_shower, bodyCount));
        categoryList.add(new CategoryUiModel("c4", "Dưỡng môi", R.drawable.ic_lips, lipsCount));
        categoryList.add(new CategoryUiModel("c5", "Combo/Giftbox", R.drawable.ic_combo, comboCount));
        return categoryList;
    }

    private void observeProductCounts() {
        allProductsLive = androidx.lifecycle.FlowLiveDataConversions.asLiveData(productRepository.getAllProducts());

        observer = products -> {
            if (products == null || products.isEmpty()) return;
            executor.execute(() -> {
                try {
                    _suggestedProducts.postValue(products);
                    _categories.postValue(buildCategories(products));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        };
        allProductsLive.observeForever(observer);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (allProductsLive != null && observer != null) {
            allProductsLive.removeObserver(observer);
        }
        executor.shutdown();
    }

    private void refreshProducts() {
        executor.execute(() -> {
            try {
                List<ProductEntity> localProducts = productRepository.getLocalJsonReader().getAllProducts();
                if (localProducts != null && !localProducts.isEmpty()) {
                    List<ProductEntity> current = _suggestedProducts.getValue();
                    if (current == null || current.isEmpty()) {
                        preloadUiData(localProducts);
                    }
                    productRepository.syncAllProductImagesFromAssets(localProducts);
                }
                productRepository.refreshProducts(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
