package com.veganbeauty.app.features.home;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.FlowLiveDataConversions;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;
import com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment;
import com.veganbeauty.app.features.shop.product.CartBottomSheetFragment;
import com.veganbeauty.app.features.shop.search.ShopSearchFragment;

import java.util.List;

public final class HomeHeaderHelper {

    private HomeHeaderHelper() {
    }

    public static void setup(Fragment fragment, View root) {
        View searchBar = root.findViewById(R.id.home_header_search_bar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v -> openSearch(fragment));
        }

        View qrBtn = root.findViewById(R.id.home_header_qr_btn);
        if (qrBtn != null) {
            qrBtn.setOnClickListener(v -> openQrScan(fragment));
        }

        View cartBtn = root.findViewById(R.id.home_header_cart_btn);
        if (cartBtn != null) {
            cartBtn.setOnClickListener(v -> {
                CartBottomSheetFragment cartSheet = CartBottomSheetFragment.newInstance(null, 0L);
                cartSheet.show(fragment.getParentFragmentManager(), CartBottomSheetFragment.TAG);
            });
        }

        TextView cartBadge = root.findViewById(R.id.home_header_cart_badge);
        if (cartBadge != null) {
            bindCartBadge(fragment, cartBadge);
        }

        View notificationBtn = NotificationBadgeHelper.findBellContainer(root);
        if (notificationBtn != null) {
            notificationBtn.setOnClickListener(v -> {
                fragment.getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        TextView badge = NotificationBadgeHelper.findBadgeView(root);
        if (badge != null) {
            NotificationBadgeHelper.styleBadge(badge, fragment.requireContext());
            bindNotificationBadge(fragment, badge);
        }
    }

    private static void openSearch(Fragment fragment) {
        fragment.getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new ShopSearchFragment())
                .addToBackStack(null)
                .commit();
    }

    private static void openQrScan(Fragment fragment) {
        fragment.getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new BarcodeScanFragment())
                .addToBackStack(null)
                .commit();
    }

    private static void bindCartBadge(Fragment fragment, TextView badge) {
        if (fragment.getContext() == null) {
            return;
        }
        FlowLiveDataConversions.asLiveData(
                RootieDatabase.getDatabase(fragment.requireContext()).cartDao().getAllCartItems()
        ).observe(fragment.getViewLifecycleOwner(), items -> updateCartBadge(badge, items));
    }

    private static void updateCartBadge(TextView badge, List<CartItemEntity> items) {
        int totalQty = 0;
        if (items != null) {
            for (CartItemEntity item : items) {
                totalQty += item.getQuantity();
            }
        }
        if (totalQty > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(totalQty > 99 ? "99+" : String.valueOf(totalQty));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private static void bindNotificationBadge(Fragment fragment, TextView badge) {
        if (fragment.getContext() == null) {
            return;
        }
        FlowLiveDataConversions.asLiveData(
                NotificationRepository.getInstance(fragment.requireContext()).getUnreadCount()
        ).observe(fragment.getViewLifecycleOwner(), count ->
                NotificationBadgeHelper.updateBadgeCount(badge, count));
    }
}
