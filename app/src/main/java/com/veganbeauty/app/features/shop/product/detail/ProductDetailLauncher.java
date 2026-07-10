package com.veganbeauty.app.features.shop.product.detail;

import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.entities.ProductEntity;

public final class ProductDetailLauncher {

    private ProductDetailLauncher() {
    }

    public static void open(@Nullable Fragment fragment, @Nullable ProductEntity product) {
        if (product == null) return;
        if (fragment != null) {
            FragmentActivity activity = fragment.getActivity();
            if (activity != null) {
                open(activity, product);
                return;
            }
        }
        open(fragment, product.getId());
    }

    public static void open(@Nullable FragmentActivity activity, @Nullable ProductEntity product) {
        if (activity == null || product == null) return;
        String productId = product.getId();
        if (productId == null || productId.isEmpty()) return;
        try {
            ShopDetailFragment detail = ShopDetailFragment.newInstance(productId);
            detail.setProduct(product);
            activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.main_container, detail)
                    .addToBackStack("shop_product_detail")
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    public static void open(@Nullable Fragment fragment, @Nullable String productId) {
        if (fragment == null || productId == null || productId.isEmpty()) return;
        FragmentActivity activity = fragment.getActivity();
        if (activity != null) {
            open(activity, productId);
            return;
        }
        try {
            FragmentManager fm = fragment.getParentFragmentManager();
            fm.beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.main_container, ShopDetailFragment.newInstance(productId))
                    .addToBackStack("shop_product_detail")
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
            showError(fragment);
        }
    }

    public static void open(@Nullable FragmentActivity activity, @Nullable String productId) {
        if (activity == null || productId == null || productId.isEmpty()) return;
        try {
            activity.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out,
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                    )
                    .replace(R.id.main_container, ShopDetailFragment.newInstance(productId))
                    .addToBackStack("shop_product_detail")
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(activity, "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
        }
    }

    private static void showError(@Nullable Fragment fragment) {
        if (fragment == null || fragment.getContext() == null) return;
        Toast.makeText(fragment.getContext(), "Không thể mở chi tiết sản phẩm", Toast.LENGTH_SHORT).show();
    }
}
