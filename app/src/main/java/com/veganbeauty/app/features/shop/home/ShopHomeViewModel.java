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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShopHomeViewModel extends RootieViewModel {

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
        return ShopCategoryHelper.buildHomeCategories(products);
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
