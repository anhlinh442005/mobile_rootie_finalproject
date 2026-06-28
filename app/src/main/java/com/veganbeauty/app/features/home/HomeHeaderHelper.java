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

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.flow.FlowCollector;

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

        View notificationBtn = root.findViewById(R.id.home_header_notification_btn);
        if (notificationBtn != null) {
            notificationBtn.setOnClickListener(v -> {
                fragment.getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        TextView badge = root.findViewById(R.id.home_header_notification_badge);
        if (badge != null) {
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
        new Thread(() -> {
            try {
                Thread.sleep(300);
                Context ctx = fragment.getContext();
                if (ctx == null) return;

                NotificationRepository.getInstance(ctx)
                        .getUnreadCount()
                        .collect(new FlowCollector<Integer>() {
                            @Override
                            public Object emit(Integer count, Continuation<? super kotlin.Unit> continuation) {
                                if (fragment.getActivity() != null) {
                                    fragment.getActivity().runOnUiThread(() -> {
                                        if (count != null && count > 0) {
                                            badge.setText(String.valueOf(count));
                                            badge.setVisibility(View.VISIBLE);
                                        } else {
                                            badge.setVisibility(View.GONE);
                                        }
                                    });
                                }
                                return kotlin.Unit.INSTANCE;
                            }
                        }, new Continuation<kotlin.Unit>() {
                            @Override
                            public CoroutineContext getContext() {
                                return EmptyCoroutineContext.INSTANCE;
                            }

                            @Override
                            public void resumeWith(Object o) {
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
