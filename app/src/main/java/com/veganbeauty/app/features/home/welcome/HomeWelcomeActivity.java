package com.veganbeauty.app.features.home.welcome;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.veganbeauty.app.MainActivity;
import com.veganbeauty.app.R;
import com.veganbeauty.app.databinding.HomeForgotPasswordSheetBinding;
import com.veganbeauty.app.databinding.HomeLoginSheetBinding;
import com.veganbeauty.app.databinding.HomeWelcomeSheetBinding;
import com.veganbeauty.app.databinding.HomeRegisterSheetBinding;
import com.veganbeauty.app.features.auth.AuthViewModel;
import com.veganbeauty.app.features.auth.FreshDemoAccountSeeder;
import com.veganbeauty.app.features.ai.ChatHistoryHelper;
import com.veganbeauty.app.features.home.HomeFragment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class HomeWelcomeActivity extends AppCompatActivity {

    private enum FlowPhase {
        /** Trang logo + sheet peek (có lớp phủ primary + thanh kéo). */
        SPLASH,
        /** 3 trang thông tin sau khi kéo lên. */
        ONBOARDING,
        /** Form đăng nhập — không dùng lớp phủ peek. */
        LOGIN,
        /** Form đăng ký. */
        REGISTER,
        /** Quên mật khẩu. */
        FORGOT_PASSWORD
    }

    private FrameLayout bottomSheet;
    private FrameLayout sheetContent;
    private View peekContainer;
    private View peekScrim;
    private View peekHandle;
    private BottomSheetBehavior<FrameLayout> sheetBehavior;
    private View splashContent;
    private ImageView logoIcon;
    private ImageView logoText;

    private FlowPhase phase = FlowPhase.SPLASH;
    private boolean welcomeSheetLockedExpanded = false;
    @Nullable
    private HomeWelcomeSheetBinding welcomeBinding;
    @Nullable
    private HomeLoginSheetBinding loginBinding;
    @Nullable
    private HomeRegisterSheetBinding registerBinding;
    @Nullable
    private HomeForgotPasswordSheetBinding forgotPasswordBinding;
    @Nullable
    private android.os.CountDownTimer otpCountDownTimer;
    private String generatedOtp = "";
    @Nullable
    private android.os.CountDownTimer regOtpCountDownTimer;
    private String generatedRegOtp = "";
    @Nullable
    private ViewPager2 welcomePager;
    @Nullable
    private HomeWelcomePagerAdapter pagerAdapter;
    private AuthViewModel authViewModel;
    private float sheetCornerRadiusPx;
    private int bottomSystemInset = 0;

    private final int[] welcomeTitles = {
            R.string.home_welcome_1_title,
            R.string.home_welcome_2_title,
            R.string.home_welcome_3_title
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (com.veganbeauty.app.data.local.ProfileSession.INSTANCE.isLoggedIn(this)) {
            navigateToMain();
            finish();
            return;
        }
        setContentView(R.layout.home_welcome_activity);

        sheetCornerRadiusPx = getResources().getDimension(R.dimen.home_sheet_corner_radius);

        bottomSheet = findViewById(R.id.home_bottom_sheet);
        sheetContent = findViewById(R.id.home_sheet_content);
        peekContainer = findViewById(R.id.home_peek_container);
        peekScrim = findViewById(R.id.home_peek_scrim);
        peekHandle = findViewById(R.id.home_peek_handle);
        splashContent = findViewById(R.id.home_splash_content);
        logoIcon = findViewById(R.id.home_logo_icon);
        logoText = findViewById(R.id.home_logo_text);

        setupInsets();
        setupBottomSheet();
        setupViewModel();
        syncTeamUsers();
        
        if (getIntent() != null && getIntent().getBooleanExtra("DIRECT_LOGIN", false)) {
            // Set static logo state
            logoIcon.setAlpha(1f);
            logoIcon.setScaleX(1f);
            logoIcon.setScaleY(1f);
            logoIcon.setTranslationY(0f);
            
            logoText.setAlpha(1f);
            logoText.setVisibility(View.VISIBLE);
            logoText.setTranslationY(0f);
            
            // Skip splash and jump straight to login
            transitionToLogin();
        } else {
            startSplashSequence();
        }
    }

    private void setupViewModel() {
        com.veganbeauty.app.data.local.RootieDatabase db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(this);
        
        // Force sync users.json to ensure test accounts are available after destructive migration
        new Thread(() -> {
            try {
                String jsonString = new java.io.BufferedReader(
                    new java.io.InputStreamReader(getAssets().open("users.json")))
                    .lines().collect(java.util.stream.Collectors.joining("\n"));
                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);
                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    String userId = obj.optString("user_id", "");
                    if (userId.isEmpty()) {
                        userId = java.util.UUID.randomUUID().toString();
                    }
                    com.veganbeauty.app.data.local.entities.UserEntity existing = db.userDao().getUserByIdSync(userId);
                    if (existing != null) continue;

                    com.veganbeauty.app.data.local.entities.UserEntity user = new com.veganbeauty.app.data.local.entities.UserEntity(
                        userId,
                        obj.optString("username", ""),
                        obj.optString("full_name", ""),
                        obj.optString("email", ""),
                        obj.optString("phone", ""),
                        obj.optString("password", ""),
                        obj.optString("avatar", null),
                        obj.optString("primary_image", null)
                    );
                    db.userDao().insertUserSync(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        com.veganbeauty.app.data.repository.AuthRepository repository = new com.veganbeauty.app.data.repository.AuthRepository(db.userDao(), getApplicationContext());
        com.veganbeauty.app.features.auth.AuthViewModelFactory factory = new com.veganbeauty.app.features.auth.AuthViewModelFactory(repository);
        authViewModel = new androidx.lifecycle.ViewModelProvider(this, factory).get(com.veganbeauty.app.features.auth.AuthViewModel.class);

        authViewModel.loginState.observe(this, state -> {
            if (state instanceof com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Loading) {
                setLoginInProgress(true);
            } else if (state instanceof com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Success) {
                setLoginInProgress(false);
                com.veganbeauty.app.data.local.entities.UserEntity user = ((com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Success) state)
                        .getUser();
                Runnable completeLogin = () -> {
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setLoggedIn(HomeWelcomeActivity.this, true);
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setUserId(HomeWelcomeActivity.this, user.getUser_id());
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setFullName(HomeWelcomeActivity.this, user.getFull_name());
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setEmail(HomeWelcomeActivity.this, user.getEmail());
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setPhone(HomeWelcomeActivity.this, user.getPhone());
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setUsername(HomeWelcomeActivity.this, user.getUsername());
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setAvatar(
                            HomeWelcomeActivity.this,
                            com.veganbeauty.app.utils.ProfileSessionHelper.resolveAvatarUrl(user)
                    );
                    com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setPrimaryImage(
                            HomeWelcomeActivity.this,
                            user.getPrimary_image() != null ? user.getPrimary_image() : ""
                    );
                    navigateToMain();
                    if (!FreshDemoAccountSeeder.isDemoAccount(user.getUser_id(), user.getEmail())) {
                        com.veganbeauty.app.utils.SyncDataHelper.syncRewardPointsFromFirestore(HomeWelcomeActivity.this);
                        com.veganbeauty.app.utils.SyncDataHelper.syncUserProfileFromFirestore(HomeWelcomeActivity.this, null);
                    }
                };

                if (FreshDemoAccountSeeder.isDemoAccount(user.getUser_id(), user.getEmail())) {
                    new Thread(() -> {
                        FreshDemoAccountSeeder.resetLocalDataBlocking(getApplicationContext());
                        runOnUiThread(() -> {
                            ChatHistoryHelper.clearChatHistory(HomeWelcomeActivity.this);
                            HomeFragment.resetQuizPopupSessionFlag();
                            com.veganbeauty.app.features.shop.product.CartHelper.clearCart(HomeWelcomeActivity.this);
                            com.veganbeauty.app.data.local.ProfileSession.activateUserSession(
                                    HomeWelcomeActivity.this, user.getUser_id());
                            com.veganbeauty.app.data.local.SkinHistoryLocalStore.hydrateProfileFromHistoryIfNeeded(
                                    HomeWelcomeActivity.this);
                            com.veganbeauty.app.data.local.ProfileSession.ensureSkinTestTimestampFromProfile(
                                    HomeWelcomeActivity.this);
                            completeLogin.run();
                            com.veganbeauty.app.utils.ProfileSessionHelper.restoreLocalAvatarIfPresent(
                                    HomeWelcomeActivity.this);
                        });
                    }).start();
                } else {
                    com.veganbeauty.app.data.local.ProfileSession.activateUserSession(this, user.getUser_id());
                    com.veganbeauty.app.utils.AddressBookHelper.loadForUser(this, user.getUser_id());
                    ChatHistoryHelper.clearChatHistory(this);
                    HomeFragment.resetQuizPopupSessionFlag();
                    com.veganbeauty.app.features.shop.product.CartHelper.clearCart(this);
                    completeLogin.run();
                }
            } else if (state instanceof com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Error) {
                setLoginInProgress(false);
                android.widget.Toast.makeText(this,
                        ((com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Error) state).getMessage(),
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        authViewModel.registerState.observe(this, state -> {
            if (state instanceof com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Success) {
                android.widget.Toast.makeText(this, "Đăng ký thành công", android.widget.Toast.LENGTH_SHORT).show();
                transitionToLogin();
            } else if (state instanceof com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Error) {
                android.widget.Toast.makeText(this,
                        ((com.veganbeauty.app.features.auth.AuthViewModel.AuthState.Error) state).getMessage(),
                        android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_coordinator), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            bottomSystemInset = bars.bottom;
            splashContent.setPadding(splashContent.getPaddingLeft(), bars.top, splashContent.getPaddingRight(),
                    splashContent.getPaddingBottom());

            if (welcomeBinding != null) {
                welcomeBinding.homeWelcomeBottomPanel.setPadding(
                        welcomeBinding.homeWelcomeBottomPanel.getPaddingLeft(),
                        welcomeBinding.homeWelcomeBottomPanel.getPaddingTop(),
                        welcomeBinding.homeWelcomeBottomPanel.getPaddingRight(),
                        (int) (24 * getResources().getDisplayMetrics().density) + bottomSystemInset);
            }
            if (loginBinding != null) {
                android.view.View container = loginBinding.homeLoginContent;
                if (container != null) {
                    container.setPadding(
                            container.getPaddingLeft(),
                            container.getPaddingTop(),
                            container.getPaddingRight(),
                            (int) (28 * getResources().getDisplayMetrics().density) + bottomSystemInset);
                }
            }
            if (registerBinding != null) {
                android.view.View container = ((android.view.ViewGroup) registerBinding.getRoot()).getChildAt(0);
                if (container != null) {
                    container.setPadding(
                            container.getPaddingLeft(),
                            container.getPaddingTop(),
                            container.getPaddingRight(),
                            (int) (32 * getResources().getDisplayMetrics().density) + bottomSystemInset);
                }
            }
            if (forgotPasswordBinding != null && forgotPasswordBinding.homeForgotContent != null) {
                android.view.View container = forgotPasswordBinding.homeForgotContent;
                container.setPadding(
                        container.getPaddingLeft(),
                        container.getPaddingTop(),
                        container.getPaddingRight(),
                        (int) (28 * getResources().getDisplayMetrics().density) + bottomSystemInset);
            }
            return insets;
        });
    }

    private void setupBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(false);
        sheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.home_sheet_peek_height));
        sheetBehavior.setExpandedOffset(0);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View sheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        if (phase != FlowPhase.LOGIN && phase != FlowPhase.REGISTER) {
                            lockWelcomeSheetExpanded();
                            if (phase == FlowPhase.SPLASH) {
                                enterOnboardingUi();
                            }
                        }
                        if (phase == FlowPhase.LOGIN && loginBinding != null) {
                            loginBinding.homeLoginForm.setVisibility(View.VISIBLE);
                        }
                        if (phase == FlowPhase.REGISTER && registerBinding != null) {
                            registerBinding.homeRegisterScroll.scrollTo(0, 0);
                        }
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        if (welcomeSheetLockedExpanded && phase == FlowPhase.ONBOARDING) {
                            bottomSheet.post(() -> sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
                            return;
                        }
                        updateSheetCorners(0f);
                        if (phase != FlowPhase.LOGIN && phase != FlowPhase.REGISTER) {
                            updateDragTransition(0f);
                            showSplashContent();
                        }
                        break;
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        updateSheetAlphaFromSlide();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View sheet, float slideOffset) {
                float offset = Math.max(0f, Math.min(1f, slideOffset));
                if (phase != FlowPhase.LOGIN && phase != FlowPhase.REGISTER) {
                    updateDragTransition(offset);
                    if (offset >= 0.95f && !welcomeSheetLockedExpanded) {
                        lockWelcomeSheetExpanded();
                        if (phase == FlowPhase.SPLASH) {
                            enterOnboardingUi();
                        }
                    } else if (phase == FlowPhase.SPLASH && offset > 0.15f) {
                        enterOnboardingUi();
                    }
                }
                updateSheetCorners(offset);
                if (phase == FlowPhase.LOGIN && offset > 0.3f && loginBinding != null) {
                    loginBinding.homeLoginForm.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void startSplashSequence() {
        splashContent.post(() -> {
            float density = getResources().getDisplayMetrics().density;
            // Khoảng cách đẩy (push distance): Icon sẽ đứng thấp hơn 60dp so với vị trí
            // cuối cùng
            float pushDistance = 60f * density;

            logoIcon.setAlpha(0f);
            logoIcon.setScaleX(0.5f);
            logoIcon.setScaleY(0.5f);
            logoIcon.setTranslationY(pushDistance); // Đứng ở vị trí trung tâm chờ

            logoText.setAlpha(1f);
            logoText.setVisibility(View.INVISIBLE);
            // Text bắt đầu từ tít bên dưới đáy màn hình
            logoText.setTranslationY(splashContent.getHeight());

            // Giai đoạn 1: Icon nảy ra ở vị trí trung tâm
            logoIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationY(pushDistance)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .withEndAction(() -> {
                        logoText.setVisibility(View.VISIBLE);

                        // Giai đoạn 2: Text chạy nhanh từ đáy màn hình lên, "chạm" vào đít icon
                        logoText.animate().translationY(pushDistance)
                                .setDuration(350).setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .withEndAction(() -> {
                                    // Giai đoạn 3: Text đẩy Icon cùng tiến lên vị trí cuối cùng (translationY = 0)
                                    logoText.animate().translationY(0f)
                                            .setDuration(450)
                                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                                            .start();

                                    logoIcon.animate().translationY(0f)
                                            .setDuration(450)
                                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                                            .withEndAction(this::showSplashPeekSheet)
                                            .start();
                                }).start();
                    }).start();
        });
    }

    /** Trang logo: sheet peek + lớp phủ primary 30% + thanh 60x5. */
    private void showSplashPeekSheet() {
        phase = FlowPhase.SPLASH;
        inflateWelcomeSheet();
        setWelcomeBottomPanelVisible(false);

        bottomSheet.setBackground(null);
        bottomSheet.setClipToOutline(false);
        bottomSheet.setAlpha(1f);
        bottomSheet.setVisibility(View.VISIBLE);
        showPeekOverlay(true);
        sheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.home_sheet_peek_height));
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        updateSheetCorners(0f);
        updateDragTransition(0f);
    }

    private void showPeekOverlay(boolean visible) {
        peekContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            peekScrim.setAlpha(1f);
            peekHandle.setAlpha(1f);
        }
    }

    private void enterOnboardingUi() {
        if (phase == FlowPhase.ONBOARDING)
            return;
        phase = FlowPhase.ONBOARDING;
        setWelcomeBottomPanelVisible(true);
        if (welcomeBinding != null && welcomeBinding.homeWelcomeBottomPanel.getAlpha() < 1f) {
            animateSlideUpFromBottom(welcomeBinding.homeWelcomeBottomPanel, () -> {
            });
        }
        if (welcomeSheetLockedExpanded) {
            ensureWelcomeSheetExpanded();
        }
    }

    /** Giữ sheet full màn khi đã kéo hết; không hở logo phía trên. */
    private void lockWelcomeSheetExpanded() {
        welcomeSheetLockedExpanded = true;
        sheetBehavior.setSkipCollapsed(true);
        updateSheetCorners(1f);
        updateDragTransition(1f);
        hideSplashContent();
    }

    private void ensureWelcomeSheetExpanded() {
        if (phase != FlowPhase.ONBOARDING && phase != FlowPhase.SPLASH)
            return;
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        lockWelcomeSheetExpanded();
    }

    private void hideSplashContent() {
        splashContent.setVisibility(View.GONE);
    }

    private void showSplashContent() {
        if (phase == FlowPhase.SPLASH && !welcomeSheetLockedExpanded) {
            splashContent.setVisibility(View.VISIBLE);
            splashContent.setAlpha(1f);
        }
    }

    private void setWelcomeBottomPanelVisible(boolean visible) {
        if (welcomeBinding == null)
            return;
        welcomeBinding.homeWelcomeBottomPanel.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        if (!visible) {
            welcomeBinding.homeWelcomeBottomPanel.setAlpha(0f);
        }
    }

    private void animateSlideUpFromBottom(View view, Runnable onEnd) {
        float distance = 48f * getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationY(distance);
        view.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(550)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(onEnd)
                .start();
    }

    /** Login/register sheets need full height — avoids clipped form and scroll glitches. */
    private void expandAuthSheet() {
        if (phase != FlowPhase.LOGIN && phase != FlowPhase.REGISTER) {
            return;
        }
        sheetBehavior.setSkipCollapsed(true);
        bottomSheet.post(() -> sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));
    }

    private void inflateWelcomeSheet() {
        sheetContent.removeAllViews();
        welcomeBinding = HomeWelcomeSheetBinding.inflate(
                LayoutInflater.from(this),
                sheetContent,
                true);
        loginBinding = null;
        registerBinding = null;

        welcomeBinding.homeWelcomeBottomPanel.setPadding(
                welcomeBinding.homeWelcomeBottomPanel.getPaddingLeft(),
                welcomeBinding.homeWelcomeBottomPanel.getPaddingTop(),
                welcomeBinding.homeWelcomeBottomPanel.getPaddingRight(),
                (int) (24 * getResources().getDisplayMetrics().density) + bottomSystemInset);

        List<HomeWelcomePagerAdapter.WelcomePage> pages = Arrays.asList(
                new HomeWelcomePagerAdapter.WelcomePage(R.drawable.ic_welcome_1),
                new HomeWelcomePagerAdapter.WelcomePage(R.drawable.ic_welcome_2),
                new HomeWelcomePagerAdapter.WelcomePage(R.drawable.ic_welcome_3));

        pagerAdapter = new HomeWelcomePagerAdapter(pages);

        welcomePager = welcomeBinding.homeWelcomePager;
        welcomePager.setAdapter(pagerAdapter);
        welcomePager.setOffscreenPageLimit(1);
        welcomePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateWelcomeUi(position);
                if (welcomeSheetLockedExpanded) {
                    ensureWelcomeSheetExpanded();
                }
            }
        });

        setupIndicators();
        updateWelcomeUi(0);

        welcomeBinding.homeWelcomeBtnContinue.setOnClickListener(v -> {
            int current = welcomePager.getCurrentItem();
            if (current < 2) {
                if (welcomeSheetLockedExpanded) {
                    ensureWelcomeSheetExpanded();
                }
                welcomePager.setCurrentItem(current + 1, true);
                bottomSheet.post(this::ensureWelcomeSheetExpanded);
            } else {
                transitionToLogin();
            }
        });

        welcomeBinding.homeWelcomeBtnBack.setOnClickListener(v -> {
            int current = welcomePager.getCurrentItem();
            if (current > 0) {
                if (welcomeSheetLockedExpanded) {
                    ensureWelcomeSheetExpanded();
                }
                welcomePager.setCurrentItem(current - 1, true);
                bottomSheet.post(this::ensureWelcomeSheetExpanded);
            }
        });
    }

    private void setupIndicators() {
        if (welcomeBinding == null)
            return;

        LinearLayout container = welcomeBinding.homeWelcomeIndicators;
        container.removeAllViews();
        int dotMargin = (int) (6 * getResources().getDisplayMetrics().density);
        float density = getResources().getDisplayMetrics().density;

        for (int index = 0; index < 3; index++) {
            boolean isActive = index == 0;
            View dot = new View(this);
            dot.setBackground(ContextCompat.getDrawable(
                    this,
                    isActive ? R.drawable.home_indicator_pill : R.drawable.home_indicator_dot));

            int width = (int) ((isActive ? 24 : 8) * density);
            int height = (int) (8 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
            if (index > 0) {
                lp.setMarginStart(dotMargin);
            }
            dot.setLayoutParams(lp);
            container.addView(dot);
        }
    }

    private void updateWelcomeUi(int position) {
        if (welcomeBinding == null)
            return;

        welcomeBinding.homeWelcomeTitle.setText(welcomeTitles[position]);
        welcomeBinding.homeWelcomeBtnContinue.setText(getString(
                position == 2 ? R.string.home_btn_continue_shopping : R.string.home_btn_continue));
        welcomeBinding.homeWelcomeBtnBack.setVisibility(position > 0 ? View.VISIBLE : View.GONE);

        LinearLayout container = welcomeBinding.homeWelcomeIndicators;
        float density = getResources().getDisplayMetrics().density;
        for (int i = 0; i < container.getChildCount(); i++) {
            View dot = container.getChildAt(i);
            boolean active = i == position;
            dot.setBackground(ContextCompat.getDrawable(
                    this,
                    active ? R.drawable.home_indicator_pill : R.drawable.home_indicator_dot));
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) dot.getLayoutParams();
            lp.width = (int) ((active ? 24 : 8) * density);
            dot.setLayoutParams(lp);
        }
    }

    /**
     * Khi kéo sheet: welcome mờ dần, overlay + thanh kéo (trong sheet) mờ dần,
     * ảnh nội dung sheet hiện dần theo cử chỉ kéo.
     */
    private void updateDragTransition(float slideOffset) {
        if (phase == FlowPhase.LOGIN || phase == FlowPhase.REGISTER || phase == FlowPhase.FORGOT_PASSWORD) {
            showPeekOverlay(false);
            sheetContent.setAlpha(1f);
            return;
        }

        float offset = Math.max(0f, Math.min(1f, slideOffset));
        float overlayAlpha = 1f - offset;

        boolean overlayVisible = overlayAlpha > 0.02f && !welcomeSheetLockedExpanded;
        showPeekOverlay(overlayVisible);
        if (overlayVisible) {
            peekScrim.setAlpha(overlayAlpha);
            peekHandle.setAlpha(overlayAlpha);
        }

        if (!welcomeSheetLockedExpanded) {
            if (welcomeBinding != null) {
                welcomeBinding.homeWelcomePager.setAlpha(1f);
            }
        }

        if (welcomeSheetLockedExpanded || offset >= 0.98f) {
            hideSplashContent();
        } else if (phase == FlowPhase.SPLASH || phase == FlowPhase.ONBOARDING) {
            splashContent.setVisibility(View.VISIBLE);
            splashContent.setAlpha(1f - offset);
            splashContent.setTranslationY(-offset * splashContent.getHeight() * 0.5f);
        }
    }

    private void updateSheetAlphaFromSlide() {
        if (phase == FlowPhase.LOGIN || phase == FlowPhase.REGISTER)
            return;

        View parent = (View) bottomSheet.getParent();
        if (parent == null)
            return;

        float peek = sheetBehavior.getPeekHeight();
        float parentHeight = parent.getHeight();
        if (parentHeight <= peek)
            return;

        float collapsedTop = parentHeight - peek;
        float range = Math.max(collapsedTop, 1f);
        float offset = Math.max(0f, Math.min(1f, (collapsedTop - bottomSheet.getTop()) / range));

        updateDragTransition(offset);
        updateSheetCorners(offset);
    }

    private void updateSheetCorners(float slideOffset) {
        if (phase == FlowPhase.LOGIN || phase == FlowPhase.REGISTER || phase == FlowPhase.FORGOT_PASSWORD) {
            sheetContent.setBackground(null);
            return;
        }

        // Always keep the corners rounded
        float radius = sheetCornerRadiusPx;

        ShapeAppearanceModel shapeModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                .setTopRightCorner(CornerFamily.ROUNDED, radius)
                .setBottomLeftCorner(CornerFamily.ROUNDED, 0f)
                .setBottomRightCorner(CornerFamily.ROUNDED, 0f)
                .build();

        ColorStateList fillColor = ColorStateList.valueOf(Color.parseColor("#FEFBF4"));

        MaterialShapeDrawable drawable = new MaterialShapeDrawable(shapeModel);
        drawable.setFillColor(fillColor);
        drawable.setStroke(1.5f * getResources().getDisplayMetrics().density,
                ContextCompat.getColor(this, R.color.black));

        sheetContent.setBackground(drawable);
        sheetContent.setClipToOutline(true);
    }

    private void transitionToLogin() {
        phase = FlowPhase.LOGIN;
        welcomeSheetLockedExpanded = false;
        sheetBehavior.setSkipCollapsed(false);

        splashContent.setVisibility(View.VISIBLE);
        splashContent.setAlpha(1f);
        splashContent.setTranslationY(0f);

        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheet.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    inflateLoginSheet();
                    bottomSheet.setAlpha(1f);
                    showPeekOverlay(false);
                    sheetBehavior.setPeekHeight((int) (220 * getResources().getDisplayMetrics().density));
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheet.setVisibility(View.VISIBLE);
                    updateSheetCorners(0f);
                    if (loginBinding != null) {
                        loginBinding.homeLoginScroll.scrollTo(0, 0);
                        animateSlideUpFromBottom(loginBinding.getRoot(), this::expandAuthSheet);
                    }
                })
                .start();
    }

    private void inflateLoginSheet() {
        sheetContent.removeAllViews();
        welcomeBinding = null;
        registerBinding = null;
        welcomePager = null;

        loginBinding = HomeLoginSheetBinding.inflate(
                LayoutInflater.from(this),
                sheetContent,
                true);

        android.view.View loginContainer = loginBinding.homeLoginContent;
        if (loginContainer != null) {
            loginContainer.setPadding(
                    loginContainer.getPaddingLeft(),
                    loginContainer.getPaddingTop(),
                    loginContainer.getPaddingRight(),
                    (int) (28 * getResources().getDisplayMetrics().density) + bottomSystemInset);
        }

        String registerText = getString(R.string.home_no_account) + " " + getString(R.string.home_register);
        SpannableString spannable = new SpannableString(registerText);
        String registerLabel = getString(R.string.home_register);
        int start = registerText.indexOf(registerLabel);
        if (start >= 0) {
            spannable.setSpan(
                    new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            transitionToRegister();
                        }

                        @Override
                        public void updateDrawState(@NonNull android.text.TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setTypeface(Typeface.DEFAULT_BOLD);
                            ds.setColor(ContextCompat.getColor(HomeWelcomeActivity.this, R.color.primary_light));
                        }
                    },
                    start,
                    registerText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        loginBinding.homeRegisterLink.setText(spannable);
        loginBinding.homeRegisterLink.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        loginBinding.homeRegisterLink.setHighlightColor(Color.TRANSPARENT);

        loginBinding.homeGuestLink.setOnClickListener(v -> {
            com.veganbeauty.app.data.local.ProfileSession.INSTANCE.setLoggedIn(HomeWelcomeActivity.this, false);
            com.veganbeauty.app.features.shop.product.CartHelper.clearCart(HomeWelcomeActivity.this);
            navigateToMain();
        });
        loginBinding.homeForgotPassword.setOnClickListener(v -> transitionToForgotPassword());
        loginBinding.homeBtnLogin.setOnClickListener(v -> {
            String email = loginBinding.homeInputEmail.getText() != null
                    ? loginBinding.homeInputEmail.getText().toString().trim()
                    : "";
            String password = loginBinding.homeInputPassword.getText() != null
                    ? loginBinding.homeInputPassword.getText().toString().trim()
                    : "";
            authViewModel.login(email, password);
        });

        loginBinding.homeLoginGoogle.setOnClickListener(v -> {
            android.widget.Toast.makeText(HomeWelcomeActivity.this, "Tính năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
        });

        loginBinding.homeLoginFacebook.setOnClickListener(v -> {
            android.widget.Toast.makeText(HomeWelcomeActivity.this, "Tính năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void transitionToRegister() {
        phase = FlowPhase.REGISTER;
        welcomeSheetLockedExpanded = false;
        sheetBehavior.setSkipCollapsed(false);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        splashContent.setVisibility(View.VISIBLE);
        splashContent.setAlpha(1f);
        splashContent.setTranslationY(0f);

        bottomSheet.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    inflateRegisterSheet();
                    bottomSheet.setAlpha(1f);
                    showPeekOverlay(false);
                    sheetBehavior.setPeekHeight((int) (220 * getResources().getDisplayMetrics().density));
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheet.setVisibility(View.VISIBLE);
                    updateSheetCorners(0f);
                    if (registerBinding != null) {
                        registerBinding.homeRegisterScroll.scrollTo(0, 0);
                        animateSlideUpFromBottom(registerBinding.getRoot(), this::expandAuthSheet);
                    }
                })
                .start();
    }

    private void inflateRegisterSheet() {
        sheetContent.removeAllViews();
        welcomeBinding = null;
        loginBinding = null;
        welcomePager = null;

        registerBinding = com.veganbeauty.app.databinding.HomeRegisterSheetBinding.inflate(
                LayoutInflater.from(this),
                sheetContent,
                true);

        android.view.View registerContainer = ((android.view.ViewGroup) registerBinding.getRoot()).getChildAt(0);
        if (registerContainer != null) {
            registerContainer.setPadding(
                    registerContainer.getPaddingLeft(),
                    registerContainer.getPaddingTop(),
                    registerContainer.getPaddingRight(),
                    (int) (32 * getResources().getDisplayMetrics().density) + bottomSystemInset);
        }

        String loginText = "Đã có tài khoản? Đăng nhập";
        SpannableString spannable = new SpannableString(loginText);
        String loginLabel = "Đăng nhập";
        int start = loginText.indexOf(loginLabel);
        if (start >= 0) {
            spannable.setSpan(
                    new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            transitionToLogin();
                        }

                        @Override
                        public void updateDrawState(@NonNull android.text.TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setTypeface(Typeface.DEFAULT_BOLD);
                            ds.setColor(ContextCompat.getColor(HomeWelcomeActivity.this, R.color.primary_light));
                        }
                    },
                    start,
                    loginText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        registerBinding.homeLoginLink.setText(spannable);
        registerBinding.homeLoginLink.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        registerBinding.homeLoginLink.setHighlightColor(Color.TRANSPARENT);

        String termsText = "Tôi đã đọc và đồng ý với\nĐiều khoản sử dụng và Chính sách bảo mật";
        SpannableString termsSpannable = new SpannableString(termsText);
        String termsLabel = "Điều khoản sử dụng và Chính sách bảo mật";
        int termsStart = termsText.indexOf(termsLabel);
        if (termsStart >= 0) {
            termsSpannable.setSpan(
                    new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            showTermsDialog();
                        }

                        @Override
                        public void updateDrawState(@NonNull android.text.TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setUnderlineText(false);
                            ds.setTypeface(Typeface.DEFAULT_BOLD);
                            ds.setColor(ContextCompat.getColor(HomeWelcomeActivity.this, R.color.primary_light));
                        }
                    },
                    termsStart,
                    termsText.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        registerBinding.homeCheckTerms.setText(termsSpannable);
        registerBinding.homeCheckTerms.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        registerBinding.homeCheckTerms.setHighlightColor(Color.TRANSPARENT);

        registerBinding.homeBtnSendOtpReg.setOnClickListener(v -> {
            String phone = registerBinding.homeInputPhoneReg.getText() != null
                    ? registerBinding.homeInputPhoneReg.getText().toString().trim()
                    : "";
            if (phone.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập số điện thoại", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            
            generatedRegOtp = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
            sendSmsNotification(generatedRegOtp, "Mã OTP tạo tài khoản của bạn là: " + generatedRegOtp
                    + ". Không chia sẻ mã này cho bất kỳ ai.");
            android.widget.Toast.makeText(this, "Đã gửi OTP qua tin nhắn SMS", android.widget.Toast.LENGTH_SHORT).show();
            
            registerBinding.homeInputOtpReg.setVisibility(View.VISIBLE);
            
            if (regOtpCountDownTimer != null) {
                regOtpCountDownTimer.cancel();
            }
            regOtpCountDownTimer = new android.os.CountDownTimer(5 * 60 * 1000L, 1000L) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (registerBinding == null) return;
                    long min = millisUntilFinished / 60000;
                    long sec = (millisUntilFinished % 60000) / 1000;
                    registerBinding.homeBtnSendOtpReg.setText(String.format(Locale.US, "%02d:%02d", min, sec));
                    registerBinding.homeBtnSendOtpReg.setEnabled(false);
                }
                @Override
                public void onFinish() {
                    if (registerBinding == null) return;
                    registerBinding.homeBtnSendOtpReg.setText("Gửi OTP");
                    registerBinding.homeBtnSendOtpReg.setEnabled(true);
                }
            }.start();
        });

        registerBinding.homeBtnRegister.setOnClickListener(v -> {
            String fullName = registerBinding.homeInputFullname.getText() != null
                    ? registerBinding.homeInputFullname.getText().toString().trim()
                    : "";
            String email = registerBinding.homeInputEmailPhone.getText() != null
                    ? registerBinding.homeInputEmailPhone.getText().toString().trim()
                    : "";
            String password = registerBinding.homeInputPassword.getText() != null
                    ? registerBinding.homeInputPassword.getText().toString().trim()
                    : "";
            String confirmPassword = registerBinding.homeInputConfirmPassword.getText() != null
                    ? registerBinding.homeInputConfirmPassword.getText().toString().trim()
                    : "";
            String phone = registerBinding.homeInputPhoneReg.getText() != null
                    ? registerBinding.homeInputPhoneReg.getText().toString().trim()
                    : "";
            String enteredOtp = registerBinding.homeInputOtpReg.getText() != null
                    ? registerBinding.homeInputOtpReg.getText().toString().trim()
                    : "";
            boolean acceptedTerms = registerBinding.homeCheckTerms.isChecked();

            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng điền đủ thông tin", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(confirmPassword)) {
                android.widget.Toast.makeText(this, "Mật khẩu không khớp", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (!acceptedTerms) {
                android.widget.Toast.makeText(this, "Bạn phải đồng ý với Điều khoản", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (generatedRegOtp.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhấn Gửi OTP và nhập mã xác nhận", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (!enteredOtp.equals(generatedRegOtp)) {
                android.widget.Toast.makeText(this, "Mã OTP không đúng", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(fullName, email, phone, password);
        });

        registerBinding.homeRegisterGoogle.setOnClickListener(v -> {
            android.widget.Toast.makeText(HomeWelcomeActivity.this, "Tính năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
        });

        registerBinding.homeRegisterFacebook.setOnClickListener(v -> {
            android.widget.Toast.makeText(HomeWelcomeActivity.this, "Tính năng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void showTermsDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_terms_policy);

        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvContent = dialog.findViewById(R.id.dialog_terms_content);
        tvContent.setText(androidx.core.text.HtmlCompat.fromHtml(getString(R.string.terms_and_policies_text),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY));

        dialog.findViewById(R.id.dialog_btn_close).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.dialog_btn_agree).setOnClickListener(v -> {
            if (registerBinding != null) {
                registerBinding.homeCheckTerms.setChecked(true);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void transitionToForgotPassword() {
        phase = FlowPhase.FORGOT_PASSWORD;
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        splashContent.setVisibility(View.VISIBLE);
        splashContent.setAlpha(1f);
        splashContent.setTranslationY(0f);

        bottomSheet.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> {
                    inflateForgotPasswordSheet();
                    bottomSheet.setAlpha(1f);
                    showPeekOverlay(false);
                    sheetBehavior.setPeekHeight((int) (220 * getResources().getDisplayMetrics().density));
                    sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheet.setVisibility(View.VISIBLE);
                    updateSheetCorners(0f);
                    if (forgotPasswordBinding != null) {
                        animateSlideUpFromBottom(forgotPasswordBinding.getRoot(), () -> {
                        });
                    }
                })
                .start();
    }

    private void inflateForgotPasswordSheet() {
        sheetContent.removeAllViews();
        welcomeBinding = null;
        loginBinding = null;
        registerBinding = null;
        welcomePager = null;

        forgotPasswordBinding = HomeForgotPasswordSheetBinding.inflate(
                LayoutInflater.from(this),
                sheetContent,
                true);

        if (forgotPasswordBinding.homeForgotContent != null) {
            forgotPasswordBinding.homeForgotContent.setPadding(
                    forgotPasswordBinding.homeForgotContent.getPaddingLeft(),
                    forgotPasswordBinding.homeForgotContent.getPaddingTop(),
                    forgotPasswordBinding.homeForgotContent.getPaddingRight(),
                    (int) (28 * getResources().getDisplayMetrics().density) + bottomSystemInset);
        }

        // Bước 0: Chọn phương thức
        forgotPasswordBinding.fpBtnChooseEmail.setOnClickListener(v -> {
            forgotPasswordBinding.fpStep0Container.setVisibility(View.GONE);
            forgotPasswordBinding.fpStep1Container.setVisibility(View.VISIBLE);
            forgotPasswordBinding.fpInputEmail.setHint("Địa chỉ email");
            forgotPasswordBinding.fpStep1Desc.setText("Nhập email đã đăng ký, chúng tôi sẽ gửi mã OTP để xác minh");
            forgotPasswordBinding.fpInputEmail.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            forgotPasswordBinding.fpBtnSendOtp.setTag("email");
        });

        forgotPasswordBinding.fpBtnChoosePhone.setOnClickListener(v -> {
            forgotPasswordBinding.fpStep0Container.setVisibility(View.GONE);
            forgotPasswordBinding.fpStep1Container.setVisibility(View.VISIBLE);
            forgotPasswordBinding.fpInputEmail.setHint("Số điện thoại");
            forgotPasswordBinding.fpStep1Desc.setText("Nhập số điện thoại đã đăng ký, chúng tôi sẽ gửi mã OTP qua tin nhắn");
            forgotPasswordBinding.fpInputEmail.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
            forgotPasswordBinding.fpBtnSendOtp.setTag("phone");
        });

        forgotPasswordBinding.fpBackToLogin0.setOnClickListener(v -> transitionToLogin());

        // Bước 1: Gửi OTP
        forgotPasswordBinding.fpBtnSendOtp.setOnClickListener(v -> {
            boolean isEmail = "email".equals(v.getTag());
            String input = forgotPasswordBinding.fpInputEmail.getText() != null
                    ? forgotPasswordBinding.fpInputEmail.getText().toString().trim()
                    : "";
            
            if (input.isEmpty()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập thông tin", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (isEmail && !android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                android.widget.Toast.makeText(this, "Vui lòng nhập email hợp lệ", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                com.veganbeauty.app.data.local.RootieDatabase db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(this);
                com.veganbeauty.app.data.local.entities.UserEntity user = isEmail 
                    ? db.userDao().getUserByEmailSync(input)
                    : db.userDao().getUserByPhoneSync(input);
                
                runOnUiThread(() -> {
                    if (user == null) {
                        String msg = isEmail ? "Email chưa tạo tài khoản, hãy đăng ký" : "Số điện thoại chưa tạo tài khoản, hãy đăng ký";
                        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Sinh mã OTP 6 chữ số
                    generatedOtp = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
                    
                    if (isEmail) {
                        // Show a toast that it's sending
                        android.widget.Toast.makeText(this, "Đang gửi OTP qua Email...", android.widget.Toast.LENGTH_SHORT).show();
                        // Send Email OTP thật bằng JavaMail
                        com.veganbeauty.app.utils.MailSender.sendOtpEmailAsync(input, generatedOtp, new com.veganbeauty.app.utils.MailSender.MailCallback() {
                            @Override
                            public void onSuccess() {
                                android.widget.Toast.makeText(HomeWelcomeActivity.this, "Mã OTP đã được gửi qua Email thành công", android.widget.Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                e.printStackTrace();
                                android.widget.Toast.makeText(HomeWelcomeActivity.this, "Lỗi gửi email: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        // Gửi SMS qua Notification
                        sendSmsNotification(generatedOtp, "Mã OTP khôi phục mật khẩu của bạn là: " + generatedOtp
                                + ". Không chia sẻ mã này cho bất kỳ ai.");
                        android.widget.Toast.makeText(this, "Đã gửi OTP qua tin nhắn SMS", android.widget.Toast.LENGTH_SHORT).show();
                    }

                    // Chuyển sang bước 2
                    forgotPasswordBinding.fpStep1Container.setVisibility(View.GONE);
                    forgotPasswordBinding.fpStep2Container.setVisibility(View.VISIBLE);
                    String desc = "Mã OTP đã được gửi đến " + input + ". Mã có hiệu lực trong 5 phút.";
                    forgotPasswordBinding.fpOtpDesc.setText(desc);
                    setupOtpAutoFocus();
                    startOtpCountdown();
                });
            }).start();
        });

        // Bước 1: Quay lại chọn phương thức
        forgotPasswordBinding.fpBackToLogin1.setOnClickListener(v -> {
            forgotPasswordBinding.fpStep1Container.setVisibility(View.GONE);
            forgotPasswordBinding.fpStep0Container.setVisibility(View.VISIBLE);
        });

        // Bước 2: Xác minh OTP
        forgotPasswordBinding.fpBtnVerifyOtp.setOnClickListener(v -> {
            String entered = getEnteredOtp();
            if (entered.length() < 6) {
                android.widget.Toast.makeText(this, "Vui lòng nhập đủ 6 số OTP", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            if (!entered.equals(generatedOtp)) {
                android.widget.Toast
                        .makeText(this, "Mã OTP không đúng, vui lòng thử lại", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            cancelOtpCountdown();
            // Chuyển sang bước 3
            forgotPasswordBinding.fpStep2Container.setVisibility(View.GONE);
            forgotPasswordBinding.fpStep3Container.setVisibility(View.VISIBLE);
        });

        // Bước 2: Quay lại đăng nhập
        forgotPasswordBinding.fpBackToLogin2.setOnClickListener(v -> {
            cancelOtpCountdown();
            transitionToLogin();
        });

        // Bước 3: Đặt mật khẩu mới
        forgotPasswordBinding.fpBtnResetPassword.setOnClickListener(v -> {
            String newPw = forgotPasswordBinding.fpInputNewPassword.getText() != null
                    ? forgotPasswordBinding.fpInputNewPassword.getText().toString()
                    : "";
            String confirmPw = forgotPasswordBinding.fpInputConfirmPassword.getText() != null
                    ? forgotPasswordBinding.fpInputConfirmPassword.getText().toString()
                    : "";
            if (newPw.length() < 6) {
                android.widget.Toast
                        .makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPw.equals(confirmPw)) {
                android.widget.Toast.makeText(this, "Mật khẩu nhập lại không khớp", android.widget.Toast.LENGTH_SHORT)
                        .show();
                return;
            }
            
            new Thread(() -> {
                String input = forgotPasswordBinding.fpInputEmail.getText() != null
                        ? forgotPasswordBinding.fpInputEmail.getText().toString().trim()
                        : "";
                boolean isEmail = "email".equals(forgotPasswordBinding.fpBtnSendOtp.getTag());
                
                com.veganbeauty.app.data.local.RootieDatabase db = com.veganbeauty.app.data.local.RootieDatabase.getDatabase(this);
                if (isEmail) {
                    db.userDao().updatePasswordByEmailSync(input, newPw);
                } else {
                    db.userDao().updatePasswordByPhoneSync(input, newPw);
                }
                
                runOnUiThread(() -> {
                    forgotPasswordBinding.fpStep3Container.setVisibility(View.GONE);
                    forgotPasswordBinding.fpStep4Container.setVisibility(View.VISIBLE);
                });
            }).start();
        });

        // Bước 4: Đăng nhập ngay
        forgotPasswordBinding.fpBtnGoLogin.setOnClickListener(v -> transitionToLogin());
    }

    private void sendSmsNotification(String otp, String message) {
        String channelId = "sms_otp_channel";
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "SMS OTP Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher) // Fallback icon if not available
                .setContentTitle("Tin nhắn từ Rootie")
                .setContentText(message)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            // Check for POST_NOTIFICATIONS permission on Android 13+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
                    // Still show toast as fallback if permission is not granted yet
                    android.widget.Toast.makeText(this, "[SMS] " + message, android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
            }
            notificationManager.notify(new Random().nextInt(1000), builder.build());
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "[SMS] " + message, android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private String getEnteredOtp() {
        if (forgotPasswordBinding == null)
            return "";
        String d1 = forgotPasswordBinding.fpOtp1.getText() != null ? forgotPasswordBinding.fpOtp1.getText().toString()
                : "";
        String d2 = forgotPasswordBinding.fpOtp2.getText() != null ? forgotPasswordBinding.fpOtp2.getText().toString()
                : "";
        String d3 = forgotPasswordBinding.fpOtp3.getText() != null ? forgotPasswordBinding.fpOtp3.getText().toString()
                : "";
        String d4 = forgotPasswordBinding.fpOtp4.getText() != null ? forgotPasswordBinding.fpOtp4.getText().toString()
                : "";
        String d5 = forgotPasswordBinding.fpOtp5.getText() != null ? forgotPasswordBinding.fpOtp5.getText().toString()
                : "";
        String d6 = forgotPasswordBinding.fpOtp6.getText() != null ? forgotPasswordBinding.fpOtp6.getText().toString()
                : "";
        return d1 + d2 + d3 + d4 + d5 + d6;
    }

    private void setupOtpAutoFocus() {
        if (forgotPasswordBinding == null)
            return;
        android.widget.EditText[] otpFields = {
                forgotPasswordBinding.fpOtp1,
                forgotPasswordBinding.fpOtp2,
                forgotPasswordBinding.fpOtp3,
                forgotPasswordBinding.fpOtp4,
                forgotPasswordBinding.fpOtp5,
                forgotPasswordBinding.fpOtp6
        };
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }
            });
        }
        otpFields[0].requestFocus();
    }

    private void startOtpCountdown() {
        cancelOtpCountdown();
        otpCountDownTimer = new android.os.CountDownTimer(5 * 60 * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (forgotPasswordBinding == null)
                    return;
                long min = millisUntilFinished / 60000;
                long sec = (millisUntilFinished % 60000) / 1000;
                forgotPasswordBinding.fpOtpCountdown.setText(
                        String.format(Locale.US, "Gửi lại mã sau %02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                if (forgotPasswordBinding == null)
                    return;
                forgotPasswordBinding.fpOtpCountdown.setText("Gửi lại mã OTP");
                forgotPasswordBinding.fpOtpCountdown.setOnClickListener(vv -> {
                    if (forgotPasswordBinding.fpStep2Container.getVisibility() == View.VISIBLE) {
                        generatedOtp = String.format(Locale.US, "%06d", new Random().nextInt(1000000));
                        android.widget.Toast.makeText(HomeWelcomeActivity.this,
                                "[Demo] Mã OTP mới: " + generatedOtp, android.widget.Toast.LENGTH_LONG).show();
                        startOtpCountdown();
                    }
                });
            }
        }.start();
    }

    private void cancelOtpCountdown() {
        if (otpCountDownTimer != null) {
            otpCountDownTimer.cancel();
            otpCountDownTimer = null;
        }
    }

    private void syncTeamUsers() {
        new Thread(() -> {
            try {
                com.veganbeauty.app.data.local.RootieDatabase db = com.veganbeauty.app.data.local.RootieDatabase
                        .getDatabase(getApplicationContext());
                com.veganbeauty.app.data.local.dao.UserDao userDao = db.userDao();

                String jsonString = new java.io.BufferedReader(
                        new java.io.InputStreamReader(getAssets().open("users.json")))
                        .lines().collect(java.util.stream.Collectors.joining("\n"));

                org.json.JSONArray jsonArray = new org.json.JSONArray(jsonString);

                // Clean up old "Test Account" from SQLite
                userDao.deleteUserByUsernameSync("Test Account");

                // Clean up "Test Account" from Firebase
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereEqualTo("username", "Test Account")
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                doc.getReference().delete();
                            }
                        });

                // Only sync team member user IDs
                java.util.Set<String> teamIds = new java.util.HashSet<>(java.util.Arrays.asList(
                        "test_001", "39751498", "87962440", "68751659", "85097162", "48228004"));

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    String userId = obj.optString("user_id", "");
                    if (!teamIds.contains(userId))
                        continue;

                    // Skip the user associated with this device to avoid overriding their profile changes in SQLite
                    String savedUserId = com.veganbeauty.app.data.local.ProfileSession.INSTANCE.getUserId(getApplicationContext());
                    if (savedUserId != null && savedUserId.equals(userId)) {
                        continue;
                    }

                    com.veganbeauty.app.data.local.entities.UserEntity existingUser = userDao.getUserByIdSync(userId);
                    if (existingUser != null) {
                        continue;
                    }

                    com.veganbeauty.app.data.local.entities.UserEntity user = new com.veganbeauty.app.data.local.entities.UserEntity(
                            userId,
                            obj.optString("username", ""),
                            obj.optString("full_name", ""),
                            obj.optString("email", ""),
                            obj.optString("phone", ""),
                            obj.optString("password", ""),
                            obj.optString("avatar", null),
                            obj.optString("primary_image", null)
                        );

                    userDao.insertUserSync(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setLoginInProgress(boolean inProgress) {
        if (loginBinding == null) {
            return;
        }
        loginBinding.homeBtnLogin.setEnabled(!inProgress);
        loginBinding.homeBtnLogin.setText(inProgress ? "Đang đăng nhập..." : getString(R.string.home_btn_login));
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        startActivity(intent);
        finish();
    }
}
