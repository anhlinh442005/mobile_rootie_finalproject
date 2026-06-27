package com.veganbeauty.app.features.home;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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
import com.veganbeauty.app.data.repository.ProductRepository;
import com.veganbeauty.app.databinding.HomeFragmentBinding;
import com.veganbeauty.app.features.account.reward.AccountRewardFragment;
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment;
import com.veganbeauty.app.features.home.adapter.HomeBannerAdapter;
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
import com.veganbeauty.app.features.myskin.ChooseBranchFragment;
import com.veganbeauty.app.features.myskin.MySkinFragment;
import com.veganbeauty.app.features.myskin.SkinHistoryFragment;
import com.veganbeauty.app.features.myskin.SkinScanFragment;
import com.veganbeauty.app.features.quiz.QuizTestIntroFragment;
import com.veganbeauty.app.features.routine.SkinReminderFragment;
import com.veganbeauty.app.features.shop.ShopViewModel;
import com.veganbeauty.app.features.shop.barcode.BarcodeScanFragment;
import com.veganbeauty.app.features.shop.product.CartHelper;
import com.veganbeauty.app.features.shop.product.detail.ShopDetailFragment;
import com.veganbeauty.app.features.shop.product.list.ShopListFragment;
import com.veganbeauty.app.features.shop.store.ShopStoreSystemFragment;
import com.veganbeauty.app.features.weather.SkinWeatherForecastFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private boolean isShortcutsExpanded = false;
    private CountDownTimer flashSaleTimer;
    private List<ProductEntity> allProducts = new ArrayList<>();
    private int recommendationLimit = 6;
    private List<ProductEntity> allRecommendationProducts = new ArrayList<>();
    private List<HomeShortcutItem> allShortcuts;

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
        setupBanner();
        setupRecyclerViews();
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
    }

    private void buildShortcuts() {
        allShortcuts = Arrays.asList(
                new HomeShortcutItem("Quét sản phẩm", R.drawable.ic_qrscan, () -> navigateTo(new BarcodeScanFragment())),
                new HomeShortcutItem("Soi Da AI", R.drawable.ic_ai_outline, () -> navigateTo(new SkinScanFragment())),
                new HomeShortcutItem("Đặt lịch soi da", R.drawable.ic_calendar_outline, () -> navigateTo(new ChooseBranchFragment())),
                new HomeShortcutItem("Routine của tôi", R.drawable.ic_skincare, () -> navigateTo(new MySkinFragment())),
                new HomeShortcutItem("Dự báo da hôm nay", R.drawable.ic_water_drop_outline, () -> navigateIfLoggedIn(new SkinWeatherForecastFragment())),
                new HomeShortcutItem("Kiểm tra dị ứng", R.drawable.ic_shield_outline, () -> navigateTo(new QuizTestIntroFragment())),
                new HomeShortcutItem("Hồ sơ làn da", R.drawable.ic_clipboard_outline, () -> navigateIfLoggedIn(new SkinHistoryFragment())),
                new HomeShortcutItem("Nhắc chăm da", R.drawable.ic_bell, () -> navigateIfLoggedIn(new SkinReminderFragment())),
                new HomeShortcutItem("Đổi quà Rootie Xu", R.drawable.ic_gift, () -> navigateTo(new AccountRewardFragment())),
                new HomeShortcutItem("Cửa hàng gần bạn", R.drawable.ic_store_outline, () -> navigateTo(new ShopStoreSystemFragment())),
                new HomeShortcutItem("Beauty Explore", R.drawable.ic_reel_outline, () -> navigateTo(new ComLoadingFragment())),
                new HomeShortcutItem("Tra cứu thành phần", R.drawable.ic_shortcut_ingredient, () -> Toast.makeText(getContext(), "Tính năng đang phát triển", Toast.LENGTH_SHORT).show())
        );
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null).commit();
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
            binding.ivExpandShortcuts.setImageResource(R.drawable.ic_double_chevron_up);
        } else {
            shortcutAdapter.submitList(allShortcuts.subList(0, Math.min(8, allShortcuts.size())));
            binding.ivExpandShortcuts.setImageResource(R.drawable.ic_double_chevron_down);
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
        binding.tvFlashHour.setText(String.format(Locale.getDefault(), "%02d", h));
        binding.tvFlashMinute.setText(String.format(Locale.getDefault(), "%02d", m));
        binding.tvFlashSecond.setText(String.format(Locale.getDefault(), "%02d", s));
    }

    private void setupParallaxScroll() {
        binding.homeScrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float screenCenterY = scrollY + v.getHeight() / 2f;
            float cardCenterY = binding.cvPromoLotus.getTop() + binding.cvPromoLotus.getHeight() / 2f;
            float dist = cardCenterY - screenCenterY;
            binding.tvPromoLotusLeft.setTranslationY(-dist * 0.15f);
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
        recentAdapter = new HomeProductCardAdapter(this::openProductDetail, this::addToCart);
        recommendationsAdapter = new HomeProductGridAdapter(this::openProductDetail, this::addToCart);
        bestsellerAdapter = new HomeBestsellerAdapter(this::openProductDetail, this::addToCart);
        topSearchAdapter = new HomeTopSearchAdapter(this::openProductDetail);
        flashSaleAdapter = new HomeFlashsaleAdapter(this::openProductDetail, this::addToCart);
        categoryAdapter = new HomeCategoryAdapter();
        bannerAdapter = new HomeBannerAdapter();
        shortcutAdapter = new HomeShortcutAdapter();

        binding.rvShortcuts.setLayoutManager(new GridLayoutManager(getContext(), 4));
        binding.rvShortcuts.setAdapter(shortcutAdapter);
        binding.rvShortcuts.setNestedScrollingEnabled(false);

        binding.rvFlashSale.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvFlashSale.setAdapter(flashSaleAdapter);

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

        binding.rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupHeaderActions() {
        View searchBar = binding.getRoot().findViewById(R.id.home_header_search_bar);
        if (searchBar != null) searchBar.setOnClickListener(v -> Toast.makeText(getContext(), "Tính năng tìm kiếm đang phát triển", Toast.LENGTH_SHORT).show());
        View qrBtn = binding.getRoot().findViewById(R.id.home_header_qr_btn);
        if (qrBtn != null) qrBtn.setOnClickListener(v -> Toast.makeText(getContext(), "Mở trình quét mã QR", Toast.LENGTH_SHORT).show());
        binding.tvRecentSeeAll.setOnClickListener(v -> openShop());
        binding.tvCategoriesSeeAll.setOnClickListener(v -> openShop());
    }

    private void setupPromoClicks() {
        binding.btnPromoSunscreen.setOnClickListener(v -> openFeaturedProduct("chống nắng", "bí đao"));
        binding.btnPromoLotus.setOnClickListener(v -> openFeaturedProduct("sen", "tinh chất"));
        View btnAbout = binding.getRoot().findViewById(R.id.btn_discover_about_us);
        if (btnAbout != null) btnAbout.setOnClickListener(v -> navigateTo(new HomeAboutUsFragment()));
    }

    private void setupBottomNav() {
        BottomNavHelper.setup(this, binding.getRoot(), R.id.nav_home, BottomNavHelper::navigate);
    }

    @Override
    public void observeViewModel() {
        viewModel.getProducts().observe(getViewLifecycleOwner(), products -> {
            allProducts = products;
            bindProductSections(products);
        });
    }

    private void bindProductSections(List<ProductEntity> products) {
        if (products.isEmpty()) return;
        List<ProductEntity> shuffled = new ArrayList<>(products);
        Collections.shuffle(shuffled);
        flashSaleAdapter.submitList(shuffled.subList(0, Math.min(4, shuffled.size())));
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
                    case "Combo & Bộ Sản Phẩm": icon = R.drawable.ic_combo; break;
                    case "Chăm sóc da": case "Chăm Sóc Da Mặt": icon = R.drawable.ic_skincare; break;
                    case "Tắm & Dưỡng Thể": case "Chăm Sóc Cơ Thể": icon = R.drawable.ic_shower; break;
                    case "Dưỡng Môi": case "Chăm Sóc Môi": icon = R.drawable.ic_lips; break;
                    default: icon = R.drawable.ic_grid; break;
                }
                categories.add(new HomeCategoryItem(cat, icon));
                if (categories.size() >= 6) break;
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
        ShopDetailFragment f = new ShopDetailFragment();
        f.setProduct(product);
        navigateTo(f);
    }

    private void addToCart(ProductEntity product) {
        com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet bottomSheet = new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet(
                product,
                new com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.OnQuantitySelectedListener() {
                    @Override
                    public void onAddToCartClick(ProductEntity p, int quantity) {
                        CartHelper.addToCart(requireContext(), getViewLifecycleOwner(), p, quantity);
                    }

                    @Override
                    public void onBuyNowClick(ProductEntity p, int quantity) {
                        com.veganbeauty.app.data.local.entities.CartItemEntity checkoutItem = new com.veganbeauty.app.data.local.entities.CartItemEntity(
                                p.getId(),
                                p.getName(),
                                p.getMainImage(),
                                p.getPrice(),
                                quantity,
                                true
                        );
                        ArrayList<com.veganbeauty.app.data.local.entities.CartItemEntity> list = new ArrayList<>();
                        list.add(checkoutItem);
                        com.veganbeauty.app.features.shop.product.ShopCheckoutFragment checkoutFragment = com.veganbeauty.app.features.shop.product.ShopCheckoutFragment.newInstance(list);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.main_container, checkoutFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                }
        );
        bottomSheet.show(getParentFragmentManager(), com.veganbeauty.app.features.shop.product.ChooseQuantityBottomSheet.TAG);
    }

    private void openShop() {
        getParentFragmentManager().beginTransaction().replace(R.id.main_container, new ShopListFragment()).commit();
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
        boolean needsTest = lastTestTime != 0L && (System.currentTimeMillis() - lastTestTime >= sevenDaysMs);

        if (needsTest) {
            if (!ProfileSession.isQuizReminderDismissedWeekly(requireContext())) {
                binding.quizTestWeeklyReminderLayout.getRoot().setVisibility(View.VISIBLE);
                binding.quizTestWeeklyReminderLayout.getRoot().setOnClickListener(v -> navigateToQuizIntro());
                binding.quizTestWeeklyReminderLayout.quizTestBtnDismissReminder.setOnClickListener(v -> {
                    ProfileSession.setQuizReminderDismissedWeekly(requireContext(), true);
                    binding.quizTestWeeklyReminderLayout.getRoot().setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Đã ẩn nhắc nhở tuần này", Toast.LENGTH_SHORT).show();
                });
            } else {
                binding.quizTestWeeklyReminderLayout.getRoot().setVisibility(View.GONE);
            }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (flashSaleTimer != null) flashSaleTimer.cancel();
        binding = null;
    }
}
