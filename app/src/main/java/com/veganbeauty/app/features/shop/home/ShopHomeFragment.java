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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.CartItemEntity;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.ShopHomeBinding;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment;
import com.veganbeauty.app.features.shop.home.models.CategoryUiModel;
import com.veganbeauty.app.features.shop.product.CartBottomSheetFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet;
import com.veganbeauty.app.features.shop.product.ShopCheckoutFragment;
import com.veganbeauty.app.features.shop.product.ShopVoucherFragment;
import com.veganbeauty.app.features.shop.product.SuggestedProductsBottomSheet;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;
import com.veganbeauty.app.features.shop.product.list.ShopListAdapter;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;
import com.veganbeauty.app.features.shop.search.ShopSearchFragment;

import java.util.ArrayList;
import java.util.List;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.flow.FlowCollector;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ShopHomeBinding.inflate(inflater, container, false);
        setupViewModel();
        
        categoryAdapter = new ShopHomeCategoryAdapter(category -> {
            navigateToCategoryList(category);
            return kotlin.Unit.INSTANCE;
        });

        productAdapter = new ShopListAdapter(
            product -> {
                navigateToDetail(product);
                return kotlin.Unit.INSTANCE;
            },
            product -> {
                ChooseQuantityBottomSheet bottomSheet = new ChooseQuantityBottomSheet(product, (p, quantity) -> {
                    CartHelper.INSTANCE.addToCart(requireContext(), LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), p, quantity);
                    return kotlin.Unit.INSTANCE;
                }, (p, quantity) -> {
                    CartItemEntity checkoutItem = new CartItemEntity(p.getId(), p.getName(), p.getMainImage(), p.getPrice(), quantity, true);
                    ArrayList<CartItemEntity> list = new ArrayList<>();
                    list.add(checkoutItem);
                    ShopCheckoutFragment checkoutFragment = ShopCheckoutFragment.Companion.newInstance(list);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, checkoutFragment)
                            .addToBackStack(null)
                            .commit();
                    return kotlin.Unit.INSTANCE;
                });
                bottomSheet.show(getParentFragmentManager(), ChooseQuantityBottomSheet.TAG);
                return kotlin.Unit.INSTANCE;
            },
            true
        );

        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ShopHomeViewModel(repository);
            }
        }).get(ShopHomeViewModel.class);
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
        });

        binding.fabBackToTop.setOnClickListener(v -> binding.nestedScrollView.smoothScrollTo(0, 0));

        binding.btnViewAll.setOnClickListener(v -> {
            SuggestedProductsBottomSheet suggestedBottomSheet = SuggestedProductsBottomSheet.Companion.newInstance();
            suggestedBottomSheet.show(getParentFragmentManager(), SuggestedProductsBottomSheet.TAG);
        });

        binding.flCartContainer.setOnClickListener(v -> {
            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.Companion.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        getParentFragmentManager().setFragmentResultListener(ShopVoucherFragment.REQUEST_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            cartVoucherCode = bundle.getString(ShopVoucherFragment.RESULT_VOUCHER_CODE);
            cartVoucherDiscount = bundle.getLong(ShopVoucherFragment.RESULT_VOUCHER_DISCOUNT, 0L);

            CartBottomSheetFragment cartSheet = CartBottomSheetFragment.Companion.newInstance(cartVoucherCode, cartVoucherDiscount);
            cartSheet.show(getParentFragmentManager(), CartBottomSheetFragment.TAG);
        });

        View.OnClickListener searchClickListener = v -> {
            ShopSearchFragment searchFragment = new ShopSearchFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, searchFragment)
                    .addToBackStack(null)
                    .commit();
        };

        binding.llSearchBar.setOnClickListener(searchClickListener);
        binding.etSearch.setFocusable(false);
        binding.etSearch.setClickable(true);
        binding.etSearch.setOnClickListener(searchClickListener);

        binding.ivQrScan.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, new BarcodeScanFragment())
                .addToBackStack(null)
                .commit());

        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_shop, tabId -> {
            BottomNavHelper.navigate(this, tabId);
            return null;
        });
    }

    @Override
    public void observeViewModel() {
        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    viewModel.getBanners().collect(new FlowCollector<List<com.veganbeauty.app.features.shop.home.models.BannerUiModel>>() {
                        @Nullable
                        @Override
                        public Object emit(List<com.veganbeauty.app.features.shop.home.models.BannerUiModel> banners, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            bannerAdapter.submitList(banners);
                            setupBannerDots(banners.size());
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    viewModel.getCategories().collect(new FlowCollector<List<CategoryUiModel>>() {
                        @Nullable
                        @Override
                        public Object emit(List<CategoryUiModel> categories, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            categoryAdapter.submitList(categories);
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));

        viewModel.getSuggestedProducts().observe(getViewLifecycleOwner(), products -> {
            List<ProductEntity> filtered = new ArrayList<>();
            for (ProductEntity p : products) {
                if (p.isNew() || p.getPrice() >= 500000 || p.getCategory().toLowerCase().contains("combo")) {
                    filtered.add(p);
                }
            }
            productAdapter.submitList(filtered);
        });

        LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()).launchWhenStarted((scope, cont) ->
                BuildersKt.withContext(Dispatchers.getMain(), (s2, c2) -> {
                    RootieDatabase db = RootieDatabase.getDatabase(requireContext());
                    db.cartDao().getAllCartItems().collect(new FlowCollector<List<CartItemEntity>>() {
                        @Nullable
                        @Override
                        public Object emit(List<CartItemEntity> items, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                            int totalQty = 0;
                            for (CartItemEntity it : items) totalQty += it.getQuantity();
                            if (totalQty > 0) {
                                binding.tvCartBadge.setVisibility(View.VISIBLE);
                                binding.tvCartBadge.setText(String.valueOf(totalQty));
                            } else {
                                binding.tvCartBadge.setVisibility(View.GONE);
                            }
                            return kotlin.Unit.INSTANCE;
                        }
                    }, c2);
                    return kotlin.Unit.INSTANCE;
                }, cont));
    }

    private void navigateToDetail(ProductEntity product) {
        ShopDetailFragment detailFragment = new ShopDetailFragment();
        detailFragment.setProduct(product);

        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
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
