package com.veganbeauty.app.features.home.adapter;

import android.view.View;
import android.widget.ImageView;

import com.veganbeauty.app.data.local.entities.ProductEntity;

public interface HomeProductCartListener {

    void onProductClick(ProductEntity product);

    void onQuickAddToCart(ProductEntity product, View cartButton, ImageView productImage);

    void onCartLongPress(ProductEntity product);

    void onBuyNow(ProductEntity product);
}
