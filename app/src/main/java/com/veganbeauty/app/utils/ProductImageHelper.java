package com.veganbeauty.app.utils;

import android.graphics.Color;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.veganbeauty.app.data.local.entities.ProductEntity;

import java.util.List;

public final class ProductImageHelper {

    private ProductImageHelper() {
    }

    public static void clearToWhitePlaceholder(@NonNull ImageView imageView) {
        Glide.with(imageView).clear(imageView);
        imageView.setImageDrawable(null);
        imageView.setBackgroundColor(Color.WHITE);
    }

    @NonNull
    public static String resolveImageUrl(ProductEntity product) {
        if (product == null) return "";
        String imageUrl = product.getMainImage();
        if (ProductImageCache.isValidImageUrl(imageUrl)) {
            return imageUrl.trim();
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

    public static void loadProductImage(@NonNull ImageView imageView, ProductEntity product) {
        clearToWhitePlaceholder(imageView);

        String imageUrl = resolveImageUrl(product);
        if (imageUrl.isEmpty() && product != null) {
            imageUrl = ProductImageCache.getImageUrl(
                    imageView.getContext(),
                    product.getId(),
                    product.getSku()
            );
        }
        if (imageUrl.isEmpty()) {
            return;
        }
        Glide.with(imageView.getContext())
                .load(imageUrl)
                .placeholder(android.R.color.white)
                .error(android.R.color.white)
                .into(imageView);
    }
}
