package com.veganbeauty.app.utils;

import android.content.Context;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.entities.ProductEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProductImageCache {

    private static volatile Map<String, String> imageByProductId = new HashMap<>();
    private static volatile Map<String, String> imageBySku = new HashMap<>();
    private static volatile boolean loaded = false;

    private ProductImageCache() {
    }

    public static void preload(Context context) {
        ensureLoaded(context);
    }

    public static String getImageUrl(Context context, String productId, String sku) {
        ensureLoaded(context);
        if (productId != null) {
            String byId = imageByProductId.get(productId);
            if (isValidImageUrl(byId)) return byId;
        }
        if (sku != null && !sku.isEmpty()) {
            String bySku = imageBySku.get(sku);
            if (isValidImageUrl(bySku)) return bySku;
        }
        return "";
    }

    private static void ensureLoaded(Context context) {
        if (loaded || context == null) return;
        synchronized (ProductImageCache.class) {
            if (loaded) return;
            try {
                List<ProductEntity> products = new LocalJsonReader(context.getApplicationContext()).getAllProducts();
                Map<String, String> idMap = new HashMap<>();
                Map<String, String> skuMap = new HashMap<>();
                if (products != null) {
                    for (ProductEntity product : products) {
                        String url = resolveFromProduct(product);
                        if (!isValidImageUrl(url)) continue;
                        if (product.getId() != null && !product.getId().isEmpty()) {
                            idMap.put(product.getId(), url);
                        }
                        if (product.getSku() != null && !product.getSku().isEmpty()) {
                            skuMap.put(product.getSku(), url);
                        }
                    }
                }
                imageByProductId = idMap;
                imageBySku = skuMap;
                loaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String resolveFromProduct(ProductEntity product) {
        if (product == null) return "";
        if (isValidImageUrl(product.getMainImage())) {
            return product.getMainImage().trim();
        }
        List<String> album = product.getAlbum();
        if (album != null) {
            for (String item : album) {
                if (isValidImageUrl(item)) return item.trim();
            }
        }
        return "";
    }

    public static boolean isValidImageUrl(String url) {
        if (url == null) return false;
        String trimmed = url.trim();
        return trimmed.startsWith("http://") || trimmed.startsWith("https://");
    }
}
