package com.veganbeauty.app.features.shop.home;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.LifecycleOwnerKt;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.databinding.ShopHomeBinding;
import com.veganbeauty.app.features.home.HomeHeaderHelper;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel;
import com.veganbeauty.app.features.shop.product.CartBottomSheetFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.features.shop.product.ShopVoucherFragment;
import com.veganbeauty.app.features.shop.product.SuggestedProductsBottomSheet;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;

import java.util.ArrayList;
import java.util.List;

public class ShopHomeFragment extends RootieFragment {

    private ShopHomeBinding binding;
    private ShopHomeViewModel viewModel;

    private final Handler sliderHandler = new Handler(Looper.getMainLooper());
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (binding != null) {
                int current = binding.vpBanner.getCurrentItem();
                int total = binding.vpBanner.getAdapter() != null ? binding.vpBanner.getAdapter().getItemCount() : 0;
                if (total > 0) {
                    binding.vpBanner.setCurrentItem((current + 1) % total);
                }
            }
        }
    };

    private final ShopHomeBannerAdapter bannerAdapter = new ShopHomeBannerAdapter();
    private ShopHomeCategoryAdapter categoryAdapter;
    private ShopListAdapter productAdapter;

    private String cartVoucherCode = null;
    private long cartVoucherDiscount = 0L;
    private boolean isHeaderVisible = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopHomeBinding.inflate(inflater, container, false);
        com.veganbeauty.app.utils.ProductImageCache.preload(requireContext());
        setupViewModel();
        
        categoryAdapter = new ShopHomeCategoryAdapter(category -> {
            navigateToCategoryList(category);
        });

        productAdapter = new ShopListAdapter(
            product -> {
                navigateToDetail(product);
            },
            product -> {
                if (product.getStock() <= 0) {
                    android.widget.Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(product, new ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                    @Override
                    public void onAddToCartClick(com.veganbeauty.app.data.local.entities.ProductEntity p, int quantity) {
                        CartHelper.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), p, quantity);
                    }
                    @Override
                    public void onBuyNowClick(com.veganbeauty.app.data.local.entities.ProductEntity p, int quantity) {
                        com.veganbeauty.app.data.local.entities.CartItemEntity checkoutItem = new com.veganbeauty.app.data.local.entities.CartItemEntity(
                                p.getId(), p.getName(), p.getMainImage(), p.getPrice(), quantity, true
                        );
                        ArrayList<com.veganbeauty.app.data.local.entities.CartItemEntity> list = new ArrayList<>();
                        list.add(checkoutItem);
                        com.veganbeauty.app.features.shop.product.ShopCheckoutFragment checkoutFragment = com.veganbeauty.app.features.shop.product.ShopCheckoutFragment.newInstance(list, "", 0L);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_container, checkoutFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });
                bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
            },
            true
        );

        return binding.getRoot();
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(
                requireActivity(),
                ShopHomeViewModelFactory.create(requireContext())
        ).get(ShopHomeViewModel.class);
    }

    @Override
    public void setupUI(View view) {
        binding.vpBanner.setAdapter(bannerAdapter);
        binding.vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateBannerDots(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 4000);
            }
        });

        binding.rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvCategories.setAdapter(categoryAdapter);

        binding.rvSuggestedProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSuggestedProducts.setAdapter(productAdapter);

        binding.nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            View child = v.getChildAt(0);
            if (child != null && scrollY >= child.getMeasuredHeight() - v.getMeasuredHeight() - 400) {
                productAdapter.duplicateItems();
            }
            if (scrollY > 1200) {
                binding.fabBackToTop.setVisibility(View.VISIBLE);
            } else {
                binding.fabBackToTop.setVisibility(View.GONE);
            }

            int dy = scrollY - oldScrollY;
            View header = binding.getRoot().findViewById(R.id.home_header);
            if (header != null) {
                if (scrollY <= 0) {
                    if (!isHeaderVisible) {
                        isHeaderVisible = true;
                        header.animate().translationY(0).setDuration(200).start();
                    }
                } else if (dy > 10 && isHeaderVisible) {
                    isHeaderVisible = false;
                    float translationY = -header.getHeight();
                    if (translationY == 0) {
                        float density = getResources().getDisplayMetrics().density;
                        translationY = -52 * density;
                    }
                    header.animate().translationY(translationY).setDuration(200).start();
                } else if (dy < -10 && !isHeaderVisible) {
                    isHeaderVisible = true;
                    header.animate().translationY(0).setDuration(200).start();
                }
            }
        });

        binding.fabBackToTop.setOnClickListener(v -> binding.nestedScrollView.smoothScrollTo(0, 0));

        binding.btnViewAll.setOnClickListener(v -> {
            SuggestedProductsBottomSheet suggestedBottomSheet = SuggestedProductsBottomSheet.newInstance();
            suggestedBottomSheet.show(getParentFragmentManager(), SuggestedProductsBottomSheet.TAG);
        });

        HomeHeaderHelper.setup(this, binding.getRoot());

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            cartVoucherCode = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            cartVoucherDiscount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_shop, tabId -> {
            BottomNavHelper.navigate(this, tabId);
        });
    }

    @Override
    public void observeViewModel() {
        viewModel.banners.observe(getViewLifecycleOwner(), banners -> {
            bannerAdapter.submitList(banners);
            setupBannerDots(banners.size());
        });

        viewModel.categories.observe(getViewLifecycleOwner(), categories -> {
            categoryAdapter.submitList(categories);
        });

        viewModel.suggestedProducts.observe(getViewLifecycleOwner(), products -> {
            if (products == null) return;
            List<ProductEntity> filtered = new ArrayList<>();
            for (ProductEntity p : products) {
                String cat = p.getCategory();
                if (p.isNew() || p.getPrice() >= 500000 || (cat != null && cat.toLowerCase().contains("combo"))) {
                    filtered.add(p);
                }
            }
            productAdapter.submitList(filtered);
        });
    }

    private void navigateToDetail(ProductEntity product) {
        ProductDetailLauncher.open(this, product);
    }

    private void navigateToCategoryList(CategoryUiModel category) {
        ShopListFragment listFragment = new ShopListFragment();
        Bundle args = new Bundle();
        args.putString("CATEGORY_NAME", category.getName());
        listFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, listFragment)
                .addToBackStack(null)
                .commit();
    }

    private void setupBannerDots(int count) {
        if (binding == null) return;
        binding.llBannerDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            int size = getResources().getDimensionPixelSize(R.dimen.home_banner_dot_size);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.home_banner_dot_margin));
            dot.setLayoutParams(params);
            dot.setBackground(createDotDrawable(false));
            binding.llBannerDots.addView(dot);
        }
        updateBannerDots(0);
    }

    private void updateBannerDots(int activeIndex) {
        if (binding == null) return;
        for (int i = 0; i < binding.llBannerDots.getChildCount(); i++) {
            binding.llBannerDots.getChildAt(i).setBackground(createDotDrawable(i == activeIndex));
        }
    }

    private GradientDrawable createDotDrawable(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(active ? 0xFFFFFFFF : 0x88FFFFFF);
        return drawable;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerAdapter.getItemCount() > 0) {
            sliderHandler.removeCallbacks(sliderRunnable);
            sliderHandler.postDelayed(sliderRunnable, 4000);
        }
    }

    @Override
    public void onPause() {
        sliderHandler.removeCallbacks(sliderRunnable);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        sliderHandler.removeCallbacks(sliderRunnable);
        super.onDestroyView();
        binding = null;
    }
}
