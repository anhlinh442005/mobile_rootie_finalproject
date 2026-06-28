package com.veganbeauty.app.features.shop.home;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.repository.ProductRepository;

public class ShopHomeViewModelFactory implements ViewModelProvider.Factory {

    private final ProductRepository productRepository;

    public ShopHomeViewModelFactory(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public static ShopHomeViewModelFactory create(Context context) {
        RootieDatabase db = RootieDatabase.getDatabase(context.getApplicationContext());
        ProductRepository repository = new ProductRepository(
                db.productDao(),
                new LocalJsonReader(context.getApplicationContext())
        );
        return new ShopHomeViewModelFactory(repository);
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ShopHomeViewModel.class)) {
            return (T) new ShopHomeViewModel(productRepository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
