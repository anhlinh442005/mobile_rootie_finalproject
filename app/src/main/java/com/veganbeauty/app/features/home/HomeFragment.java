package com.veganbeauty.app.features.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.veganbeauty.app.R;
import com.veganbeauty.app.core.base.RootieFragment;
import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.ProductEntity;
import com.veganbeauty.app.data.local.entities.VoucherEntity;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.HomeFragmentBinding;
import com.veganbeauty.app.features.account.reward.AccountRewardFragment;
import com.veganbeauty.app.features.profile.AccountVoucherFragment;
import com.veganbeauty.app.features.community.beauty_hub.IngredientFragment;
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment;
import com.veganbeauty.app.features.home.adapter.HomeBannerAdapter;
import com.veganbeauty.app.features.home.adapter.HomeProductCartListener;
import com.veganbeauty.app.features.home.adapter.HomeBannerItem;
import com.veganbeauty.app.features.home.adapter.HomeBestsellerAdapter;
import com.veganbeauty.app.features.home.adapter.HomeCategoryAdapter;
import com.veganbeauty.app.features.home.adapter.HomeCategoryItem;
import com.veganbeauty.app.features.home.adapter.HomeFlashsaleAdapter;
import com.veganbeauty.app.features.home.adapter.HomeProductCardAdapter;
import com.veganbeauty.app.features.home.adapter.HomeProductGridAdapter;
import com.veganbeauty.app.features.home.adapter.HomeShortcutAdapter;
import com.veganbeauty.app.features.home.adapter.HomeShortcutItem;
import com.veganbeauty.app.features.home.adapter.HomeTopSearchAdapter;
import com.veganbeauty.app.features.home.adapter.HomeVoucherCardAdapter;
import com.veganbeauty.app.features.home.adapter.HomeVoucherCategoryAdapter;
import com.veganbeauty.app.features.home.HomeHeaderHelper;
import com.veganbeauty.app.features.myskin.AccountSyncHelper;
import com.veganbeauty.app.features.myskin.ChooseBranchFragment;
import com.veganbeauty.app.features.myskin.MySkinFragment;
import com.veganbeauty.app.features.myskin.SkinHistoryFragment;
import com.veganbeauty.app.features.myskin.SkinScanFragment;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;
import com.veganbeauty.app.features.shop.ShopViewModel;
import com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.detail.ProductDetailLauncher;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;
import com.veganbeauty.app.features.shop.store.ShopStoreSystemFragment;
import com.veganbeauty.app.features.weather.SkinWeatherForecastFragment;
import com.veganbeauty.app.utils.CartFlyAnimationHelper;

import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends RootieFragment {

    private HomeFragmentBinding binding;
    private ShopViewModel viewModel;

    private HomeProductCardAdapter recentAdapter;
    private HomeProductGridAdapter recommendationsAdapter;
    private HomeBestsellerAdapter bestsellerAdapter;
    private HomeTopSearchAdapter topSearchAdapter;
    private HomeFlashsaleAdapter flashSaleAdapter;
    private HomeCategoryAdapter categoryAdapter;
    private HomeBannerAdapter bannerAdapter;
    private HomeShortcutAdapter shortcutAdapter;
    private HomeVoucherCategoryAdapter voucherCategoryAdapter;
    private HomeVoucherCardAdapter voucherCardAdapter;

    private List<VoucherEntity> allVouchers = new ArrayList<>();
    private String selectedVoucherCategory = "Tất cả";

    private boolean isShortcutsExpanded = false;
    private CountDownTimer flashSaleTimer;
    private List<ProductEntity> allProducts = new ArrayList<>();
    private int recommendationLimit = 6;
    private List<ProductEntity> allRecommendationProducts = new ArrayList<>();
    private List<HomeShortcutItem> allShortcuts;
    private HomeHeaderScrollHelper headerScrollHelper;

    private final HomeProductCartListener homeProductCartListener = new HomeProductCartListener() {
        @Override
        public void onProductClick(ProductEntity product) {
            openProductDetail(product);
        }

        @Override
        public void onQuickAddToCart(ProductEntity product, View cartButton, ImageView productImage) {
            quickAddToCart(product, cartButton, productImage);
        }

        @Override
        public void onCartLongPress(ProductEntity product) {
            showChooseQuantitySheet(product);
        }

        @Override
        public void onBuyNow(ProductEntity product) {
            buyNowToCheckout(product, 1);
        }
    };

    private static boolean hasShownQuizPopupThisSession = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = HomeFragmentBinding.inflate(inflater, container, false);
        setupViewModel();
        return binding.getRoot();
    }

    private void setupViewModel() {
        RootieDatabase db = RootieDatabase.getDatabase(requireContext());
        ProductRepository repository = new ProductRepository(db.productDao(), new LocalJsonReader(requireContext()));
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ShopViewModel(repository);
            }
        }).get(ShopViewModel.class);
    }

    @Override
    public void setupUI(View view) {
        buildShortcuts();
        setupRecyclerViews();
        setupBanner();
        setupShortcuts();
        setupHeaderActions();
        setupBottomNav();
        setupPromoClicks();
        setupParallaxScroll();
        startFlashSaleTimer();
        setupVideoPromo();
        binding.tvHomeSloganMarquee.setSelected(true);

        binding.cardHomeRoutineWidget.setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new SkinReminderFragment())
                        .addToBackStack(null).commit());

        binding.btnLoadMoreRecommendations.setOnClickListener(v -> {
            recommendationLimit += 6;
            updateRecommendationsList();
        });

        setupStreakWidget();
        setupQuizReminder();
        setupVouchers();
    }

    private void buildShortcuts() {
        allShortcuts = Arrays.asList(
                new HomeShortcutItem("Quét sản phẩm", R.drawable.ic_qrscan, () -> navigateTo(new BarcodeScanFragment())),
                new HomeShortcutItem("Soi Da AI", R.drawable.ic_ai_outline, () -> navigateTo(new SkinScanFragment())),
                new HomeShortcutItem("Đặt lịch soi da", R.drawable.ic_calendar_outline, () -> navigateTo(new ChooseBranchFragment())),
                new HomeShortcutItem("Routine của tôi", R.drawable.ic_flower, () -> navigateTo(new MySkinFragment())),
                new HomeShortcutItem("Dự báo da hôm nay", R.drawable.ic_water_drop_outline, () -> navigateIfLoggedIn(new SkinWeatherForecastFragment())),
                new HomeShortcutItem("Kiểm tra dị ứng", R.drawable.ic_shield_outline, () -> navigateTo(new QuizTestIntroFragment())),
                new HomeShortcutItem("Hồ sơ làn da", R.drawable.ic_clipboard_outline, () -> navigateIfLoggedIn(new SkinHistoryFragment())),
                new HomeShortcutItem("Nhắc chăm da", R.drawable.ic_bell, () -> navigateIfLoggedIn(new SkinReminderFragment())),
                new HomeShortcutItem("Đổi quà Rootie Xu", R.drawable.ic_gift, () -> navigateTo(new AccountRewardFragment())),
                new HomeShortcutItem("Cửa hàng gần bạn", R.drawable.ic_store_outline, () -> navigateTo(new ShopStoreSystemFragment())),
                new HomeShortcutItem("Beauty Explore", R.drawable.ic_explore, () -> navigateTo(new ComLoadingFragment())),
                new HomeShortcutItem("Tra cứu thành phần", R.drawable.ic_leaf, () -> navigateTo(new IngredientFragment()))
        );
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null).commitAllowingStateLoss();
    }

    private void navigateIfLoggedIn(Fragment fragment) {
        if (!ProfileSession.isLoggedIn(requireContext())) {
            BottomNavHelper.showLoginRequiredDialog(requireContext());
        } else {
            navigateTo(fragment);
        }
    }

    private void updateRecommendationsList() {
        List<ProductEntity> items = allRecommendationProducts.subList(0, Math.min(recommendationLimit, allRecommendationProducts.size()));
        recommendationsAdapter.submitList(items);
        binding.btnLoadMoreRecommendations.setVisibility(recommendationLimit >= allRecommendationProducts.size() ? View.GONE : View.VISIBLE);
    }

    private void setupShortcuts() {
        updateShortcutsList();
        binding.ivExpandShortcuts.setOnClickListener(v -> {
            isShortcutsExpanded = !isShortcutsExpanded;
            updateShortcutsList();
        });
    }

    private void updateShortcutsList() {
        if (isShortcutsExpanded) {
            shortcutAdapter.submitList(allShortcuts);
            binding.ivExpandShortcuts.setImageResource(R.drawable.ic_arrow_doubleup);
        } else {
            shortcutAdapter.submitList(allShortcuts.subList(0, Math.min(6, allShortcuts.size())));
            binding.ivExpandShortcuts.setImageResource(R.drawable.ic_arrow_doubledown);
        }
    }

    private void setupVideoPromo() {
        String videoUrl = "https://image.cocoonvietnam.com/uploads/2_282afe8dca.mp4";
        binding.vvPromoSunscreen.setVideoPath(videoUrl);
        binding.vvPromoSunscreen.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            mp.setVolume(0f, 0f);
            mp.start();
        });
    }

    private void startFlashSaleTimer() {
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
        int endHour = -1;
        if (currentHour >= 1 && currentHour < 5) endHour = 5;
        else if (currentHour >= 9 && currentHour < 13) endHour = 13;
        else if (currentHour >= 16) endHour = 24;

        long timeRemaining = 0L;
        if (endHour != -1) {
            Calendar endCal = Calendar.getInstance();
            if (endHour == 24) { endCal.add(Calendar.DAY_OF_YEAR, 1); endCal.set(Calendar.HOUR_OF_DAY, 0); }
            else endCal.set(Calendar.HOUR_OF_DAY, endHour);
            endCal.set(Calendar.MINUTE, 0); endCal.set(Calendar.SECOND, 0); endCal.set(Calendar.MILLISECOND, 0);
            timeRemaining = endCal.getTimeInMillis() - System.currentTimeMillis();
        }

        if (timeRemaining > 0) {
            flashSaleTimer = new CountDownTimer(timeRemaining, 1000) {
                @Override public void onTick(long ms) { updateTimerUI(ms); }
                @Override public void onFinish() { updateTimerUI(0); }
            }.start();
        } else {
            updateTimerUI(0);
        }
    }

    private void updateTimerUI(long millis) {
        if (binding == null) return;
        long s = (millis / 1000) % 60, m = (millis / 60000) % 60, h = (millis / 3600000) % 24;
        String hourStr = String.format(Locale.getDefault(), "%02d", h);
        String minuteStr = String.format(Locale.getDefault(), "%02d", m);
        String secondStr = String.format(Locale.getDefault(), "%02d", s);
        binding.flashsaleV2Section.tvFlashV2Hour.setText(hourStr);
        binding.flashsaleV2Section.tvFlashV2Minute.setText(minuteStr);
        binding.flashsaleV2Section.tvFlashV2Second.setText(secondStr);
    }

    private void updateFlashSaleV2State(boolean hasProducts) {
        if (binding == null) return;
        binding.flashsaleV2Section.llFlashV2Countdown.setVisibility(hasProducts ? View.VISIBLE : View.GONE);
    }

    private void setupParallaxScroll() {
        int bottomPad = (int) getResources().getDimension(R.dimen.home_nav_content_height);
        headerScrollHelper = new HomeHeaderScrollHelper(
                binding.homeHeader.getRoot(),
                binding.homeScrollView,
                bottomPad
        );
        headerScrollHelper.install();

        binding.homeScrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (headerScrollHelper != null) {
                headerScrollHelper.onScroll(scrollY, oldScrollY);
            }

            float screenCenterY = scrollY + v.getHeight() / 2f;
            float cardCenterY = binding.cvPromoLotus.getTop() + binding.cvPromoLotus.getHeight() / 2f;
            float dist = cardCenterY - screenCenterY;
            binding.llPromoLotusRight.setTranslationY(-dist * 0.15f);
            binding.ivPromoLotus3.setTranslationY(-dist * 0.15f);
            binding.ivPromoLotus1.setTranslationY(dist * 0.15f);
            binding.ivPromoLotus2.setTranslationY(dist * 0.08f);
        });
    }

    private void setupBanner() {
        List<Integer> bannerImages = Arrays.asList(R.drawable.imv_banner_jp, R.drawable.imv_banner_au, R.drawable.imv_banner_songxanh, R.drawable.imv_banner_taiwan);
        List<HomeBannerItem> banners = new ArrayList<>();
        for (int res : bannerImages) banners.add(new HomeBannerItem(res));
        bannerAdapter.submitBanners(banners);
        binding.vpBanner.setAdapter(bannerAdapter);
        setupBannerDots(banners.size());
        binding.vpBanner.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { updateBannerDots(position); }
        });
    }

    private void setupBannerDots(int count) {
        binding.llBannerDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            int size = getResources().getDimensionPixelSize(R.dimen.home_banner_dot_size);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(size, size);
            p.setMarginEnd(getResources().getDimensionPixelSize(R.dimen.home_banner_dot_margin));
            dot.setLayoutParams(p);
            dot.setBackground(createDotDrawable(false));
            binding.llBannerDots.addView(dot);
        }
        updateBannerDots(0);
    }

    private void updateBannerDots(int activeIndex) {
        for (int i = 0; i < binding.llBannerDots.getChildCount(); i++) {
            binding.llBannerDots.getChildAt(i).setBackground(createDotDrawable(i == activeIndex));
        }
    }

    private GradientDrawable createDotDrawable(boolean active) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(active ? 0xFFFFFFFF : 0x88FFFFFF);
        return d;
    }

    private void setupRecyclerViews() {
        recentAdapter = new HomeProductCardAdapter(homeProductCartListener);
        recommendationsAdapter = new HomeProductGridAdapter(homeProductCartListener);
        bestsellerAdapter = new HomeBestsellerAdapter(homeProductCartListener);
        topSearchAdapter = new HomeTopSearchAdapter(this::openProductDetail);
        flashSaleAdapter = new HomeFlashsaleAdapter(homeProductCartListener);
        categoryAdapter = new HomeCategoryAdapter(this::navigateToCategoryList);
        bannerAdapter = new HomeBannerAdapter();
        shortcutAdapter = new HomeShortcutAdapter();

        binding.rvShortcuts.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.rvShortcuts.setAdapter(shortcutAdapter);
        binding.rvShortcuts.setNestedScrollingEnabled(false);

        binding.flashsaleV2Section.rvFlashSaleV2.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.flashsaleV2Section.rvFlashSaleV2.setAdapter(flashSaleAdapter);
        binding.flashsaleV2Section.rvFlashSaleV2.setNestedScrollingEnabled(false);

        binding.rvRecentActivity.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvRecentActivity.setAdapter(recentAdapter);

        binding.rvRecommendations.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvRecommendations.setAdapter(recommendationsAdapter);
        binding.rvRecommendations.setNestedScrollingEnabled(false);

        binding.rvBestsellers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBestsellers.setAdapter(bestsellerAdapter);
        binding.rvBestsellers.setNestedScrollingEnabled(false);

        binding.rvTopSearch.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvTopSearch.setAdapter(topSearchAdapter);
        binding.rvTopSearch.setNestedScrollingEnabled(false);

        binding.rvCategories.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvCategories.setAdapter(categoryAdapter);
        binding.rvCategories.setNestedScrollingEnabled(false);

        voucherCategoryAdapter = new HomeVoucherCategoryAdapter(this::filterVouchersByCategory);
        voucherCardAdapter = new HomeVoucherCardAdapter();
        binding.rvVoucherCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvVoucherCategories.setAdapter(voucherCategoryAdapter);
        binding.rvVouchers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvVouchers.setAdapter(voucherCardAdapter);
    }

    private void setupVouchers() {
        binding.tvVoucherSeeAll.setOnClickListener(v -> navigateTo(new AccountVoucherFragment()));
        loadVouchersFromFirebase();
    }

    private void loadVouchersFromFirebase() {
        new FirestoreService().fetchVouchers(vouchers -> {
            if (!isAdded() || binding == null) return;
            bindVouchers(vouchers);
        });
    }

    private void bindVouchers(List<VoucherEntity> vouchers) {
        allVouchers = vouchers != null ? vouchers : new ArrayList<>();
        if (allVouchers.isEmpty()) {
            binding.sectionVouchers.setVisibility(View.GONE);
            return;
        }
        binding.sectionVouchers.setVisibility(View.VISIBLE);
        voucherCategoryAdapter.submitCategories(buildVoucherCategories(allVouchers));
        filterVouchersByCategory("Tất cả");
    }

    private List<String> buildVoucherCategories(List<VoucherEntity> vouchers) {
        Set<String> categories = new LinkedHashSet<>();
        categories.add("Tất cả");
        String[] presets = {"🔥 Flash Sale", "Chăm sóc da", "Combo & Quà", "Freeship", "Giảm giá"};
        for (String preset : presets) {
            for (VoucherEntity voucher : vouchers) {
                if (matchesCategory(voucher, preset)) {
                    categories.add(preset);
                    break;
                }
            }
        }
        for (VoucherEntity voucher : vouchers) {
            String category = voucher.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                categories.add(category.trim());
            }
        }
        return new ArrayList<>(categories);
    }

    private boolean matchesCategory(VoucherEntity voucher, String filter) {
        if ("Tất cả".equals(filter)) return true;
        String category = voucher.getCategory() != null ? voucher.getCategory().toLowerCase(Locale.ROOT) : "";
        String type = voucher.getType() != null ? voucher.getType().toLowerCase(Locale.ROOT) : "";
        String normalized = filter.toLowerCase(Locale.ROOT).replace("🔥 ", "");
        return category.contains(normalized)
                || type.contains(normalized.replace(" ", "_"))
                || ("freeship".equals(normalized) && (type.contains("free") || category.contains("free")));
    }

    private void filterVouchersByCategory(String category) {
        selectedVoucherCategory = category;
        if ("Tất cả".equals(category)) {
            voucherCardAdapter.submitList(new ArrayList<>(allVouchers));
            return;
        }
        List<VoucherEntity> filtered = new ArrayList<>();
        for (VoucherEntity voucher : allVouchers) {
            if (matchesCategory(voucher, category)
                    || category.equalsIgnoreCase(voucher.getCategory())) {
                filtered.add(voucher);
            }
        }
        voucherCardAdapter.submitList(filtered);
    }

    private void setupHeaderActions() {
        HomeHeaderHelper.setup(this, binding.getRoot());
        binding.tvRecentSeeAll.setOnClickListener(v -> openShop());
    }

    private void setupPromoClicks() {
        binding.btnPromoSunscreen.setOnClickListener(v -> openFeaturedProduct("chống nắng", "bí đao"));
        binding.btnPromoLotus.setOnClickListener(v -> openFeaturedProduct("sen", "tinh chất"));
        View btnAbout = binding.getRoot().findViewById(R.id.btn_discover_about_us);
        if (btnAbout != null) btnAbout.setOnClickListener(v -> navigateTo(new HomeAboutUsFragment()));
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_home, tabId -> BottomNavHelper.navigate(this, tabId));
    }

    @Override
    public void observeViewModel() {
        viewModel.products.observe(getViewLifecycleOwner(), products -> {
            if (products == null) return;
            allProducts = products;
            bindProductSections(products);
        });
    }

    private void bindProductSections(List<ProductEntity> products) {
        List<ProductEntity> flashSaleProducts = new ArrayList<>();
        List<ProductEntity> shuffled = new ArrayList<>();
        if (!products.isEmpty()) {
            shuffled = new ArrayList<>(products);
            Collections.shuffle(shuffled);
            flashSaleProducts = shuffled.subList(0, Math.min(4, shuffled.size()));
        }
        flashSaleAdapter.submitList(flashSaleProducts);
        updateFlashSaleV2State(!flashSaleProducts.isEmpty());

        if (products.isEmpty()) return;
        recentAdapter.submitList(products.subList(0, Math.min(8, products.size())));

        allRecommendationProducts = shuffled.subList(0, Math.min(20, shuffled.size()));
        recommendationLimit = 6;
        updateRecommendationsList();

        List<ProductEntity> sorted = new ArrayList<>(products);
        sorted.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
        bestsellerAdapter.submitList(sorted.subList(0, Math.min(3, sorted.size())));
        topSearchAdapter.submitList(products.subList(0, Math.min(3, products.size())));

        List<String> catNames = new ArrayList<>();
        List<HomeCategoryItem> categories = new ArrayList<>();
        for (ProductEntity p : products) {
            String cat = p.getCategory();
            if (cat != null && !cat.isBlank() && !cat.equals("Chăm Sóc Tóc") && !cat.equals("Chăm Sóc Mái Tóc") && !catNames.contains(cat)) {
                catNames.add(cat);
                int icon;
                switch (cat) {
                    case "Combo & Bộ Sản Phẩm": icon = R.drawable.ic_gift; break;
                    case "Chăm sóc da": case "Chăm Sóc Da Mặt": icon = R.drawable.ic_flower; break;
                    case "Tắm & Dưỡng Thể": case "Chăm Sóc Cơ Thể": icon = R.drawable.ic_water; break;
                    case "Dưỡng Môi": case "Chăm Sóc Môi": icon = R.drawable.ic_lips; break;
                    default: icon = R.drawable.ic_grid; break;
                }
                categories.add(new HomeCategoryItem(cat, icon));
                if (categories.size() >= 4) break;
            }
        }
        categoryAdapter.submitList(categories);
    }

    private ProductEntity findProduct(List<ProductEntity> products, String... keywords) {
        for (ProductEntity p : products) {
            String name = p.getName().toLowerCase();
            boolean match = true;
            for (String kw : keywords) { if (!name.contains(kw.toLowerCase())) { match = false; break; } }
            if (match) return p;
        }
        return null;
    }

    private void openFeaturedProduct(String... keywords) {
        ProductEntity p = findProduct(allProducts, keywords);
        if (p != null) openProductDetail(p); else openShop();
    }

    private void openProductDetail(ProductEntity product) {
        ProductDetailLauncher.open(this, product);
    }

    private void quickAddToCart(ProductEntity product, View cartButton, ImageView productImage) {
        if (!CartHelper.addToCart(requireContext(), getViewLifecycleOwner(), product, 1, false)) {
            return;
        }
        if (getActivity() == null) {
            return;
        }
        View cartTarget = binding.homeHeader.getRoot().findViewById(R.id.home_header_cart_btn);
        if (cartTarget != null) {
            CartFlyAnimationHelper.flyToCart(getActivity(), cartButton, cartTarget, productImage);
        }
    }

    private void buyNowToCheckout(ProductEntity product, int quantity) {
        if (product.getStock() <= 0) {
            android.widget.Toast.makeText(requireContext(), "Sản phẩm hiện đã hết hàng", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        com.veganbeauty.app.data.local.entities.CartItemEntity checkoutItem = new com.veganbeauty.app.data.local.entities.CartItemEntity(
                product.getId(),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                quantity,
                true
        );
        ArrayList<com.veganbeauty.app.data.local.entities.CartItemEntity> list = new ArrayList<>();
        list.add(checkoutItem);
        com.veganbeauty.app.features.shop.product.ShopCheckoutFragment checkoutFragment =
                com.veganbeauty.app.features.shop.product.ShopCheckoutFragment.newInstance(list, "", 0L);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, checkoutFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showChooseQuantitySheet(ProductEntity product) {
        com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet bottomSheet = new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                product,
                new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                    @Override
                    public void onAddToCartClick(ProductEntity p, int quantity) {
                        CartHelper.addToCart(requireContext(), getViewLifecycleOwner(), p, quantity);
                    }

                    @Override
                    public void onBuyNowClick(ProductEntity p, int quantity) {
                        buyNowToCheckout(p, quantity);
                    }
                }
        );
        bottomSheet.show(getParentFragmentManager(), com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.TAG);
    }

    private void openShop() {
        getParentFragmentManager().beginTransaction().replace(R.id.main_container, new ShopListFragment()).commit();
    }

    private void navigateToCategoryList(HomeCategoryItem category) {
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

    private void setupStreakWidget() {
        if (getContext() == null) return;
        int streak = ProfileSession.getSkinStreak(requireContext());
        binding.tvHomeStreakCount.setText("Chuỗi chăm da của bạn: " + streak + " ngày 🔥");
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        boolean morning = ProfileSession.isMorningRewardAwarded(requireContext(), today);
        boolean evening = ProfileSession.isEveningRewardAwarded(requireContext(), today);
        String desc;
        if (morning && evening) desc = "Tuyệt vời! Bạn đã hoàn thành tất cả routine hôm nay! 🎉";
        else if (morning) desc = "Bạn đã hoàn thành Routine Sáng! Đừng quên Routine Tối nhé! 🌙";
        else if (evening) desc = "Bạn đã hoàn thành Routine Tối! Đừng quên Routine Sáng ngày mai nhé! ☀️";
        else desc = "Hôm nay bạn chưa hoàn thành Routine nào, bấm để bắt đầu chăm da nhé! ✨";
        binding.tvHomeStreakDesc.setText(desc);
    }

    private void setupQuizReminder() {
        if (getContext() == null) return;
        long lastTestTime = ProfileSession.getLastSkinTestTime(requireContext());
        long sevenDaysMs = 7L * 24 * 60 * 60 * 1000;
        boolean needsTest = true; // Ép hiển thị mặc định theo yêu cầu

        if (needsTest) {
            // Bỏ qua check đã dismiss để luôn hiện khi load lại app
            binding.quizTestWeeklyReminderLayout.getRoot().setVisibility(View.VISIBLE);
            binding.quizTestWeeklyReminderLayout.getRoot().setOnClickListener(v -> navigateToQuizIntro());

            if (!hasShownQuizPopupThisSession) {
                hasShownQuizPopupThisSession = true;
                View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quiz_test_weekly_reminder, null);
                android.app.Dialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(dv).create();
                if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dv.findViewById(R.id.btn_dialog_confirm).setOnClickListener(v -> { dialog.dismiss(); navigateToQuizIntro(); });
                dv.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            }
        } else {
            binding.quizTestWeeklyReminderLayout.getRoot().setVisibility(View.GONE);
            if (lastTestTime == 0L && !hasShownQuizPopupThisSession) {
                hasShownQuizPopupThisSession = true;
                binding.getRoot().postDelayed(() -> {
                    if (!isAdded()) return;
                    View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quiz_test_new_user, null);
                    android.app.Dialog dialog = new MaterialAlertDialogBuilder(requireContext()).setView(dv).create();
                    if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dv.findViewById(R.id.btn_dialog_confirm).setOnClickListener(v -> { dialog.dismiss(); navigateToQuizIntro(); });
                    dv.findViewById(R.id.btn_dialog_cancel).setOnClickListener(v -> dialog.dismiss());
                    dialog.show();

                    ObjectAnimator sx = ObjectAnimator.ofFloat(dv, "scaleX", 0.7f, 1.05f, 1f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(dv, "scaleY", 0.7f, 1.05f, 1f);
                    AnimatorSet anim = new AnimatorSet(); anim.playTogether(sx, sy); anim.setDuration(500);
                    anim.setInterpolator(new OvershootInterpolator(1.5f)); anim.start();

                    View badge = dv.findViewById(R.id.layout_badge);
                    ObjectAnimator bounce = ObjectAnimator.ofFloat(badge, "translationY", 0f, -20f, 0f);
                    bounce.setDuration(1200); bounce.setRepeatCount(ObjectAnimator.INFINITE);
                    bounce.setInterpolator(new AccelerateDecelerateInterpolator()); bounce.start();
                }, 5000);
            }
        }
    }

    private void navigateToQuizIntro() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new QuizTestIntroFragment()).addToBackStack(null).commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupStreakWidget();
        setupQuizReminder();
        AccountSyncHelper.sync(requireContext(), null);
        loadVouchersFromFirebase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (flashSaleTimer != null) flashSaleTimer.cancel();
        binding = null;
    }
}
