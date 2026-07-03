package com.veganbeauty.app.features.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;
import com.veganbeauty.app.features.myskin.MySkinFragment;
import com.veganbeauty.app.features.profile.AccountProfileFragment;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;
import com.veganbeauty.app.ui.widget.NavAiSkinGlowAnimator;

public class BottomNavHelper {

    private static final int ACTIVE_COLOR = Color.parseColor("#3E4D44");
    private static final int INACTIVE_COLOR = Color.parseColor("#8FA882");
    private static final float ACTIVE_SCALE = 1.12f;
    private static final long ANIM_DURATION_MS = 220L;

    private static final int[] TAB_IDS = {
            R.id.nav_home,
            R.id.nav_shop,
            R.id.nav_myskin,
            R.id.nav_community,
            R.id.nav_account
    };

    public interface OnTabSelectedListener {
        void onTabSelected(int tabId);
    }

    public static void setup(Fragment fragment, View root, int activeTabId, OnTabSelectedListener onTabSelected) {
        for (int viewId : TAB_IDS) {
            ViewGroup tab = root.findViewById(viewId);
            if (tab == null) {
                continue;
            }
            tab.setOnClickListener(v -> {
                if (viewId == activeTabId) {
                    return;
                }
                if (!ProfileSession.isLoggedIn(root.getContext()) && viewId == R.id.nav_account) {
                    showLoginRequiredDialog(root.getContext());
                    return;
                }
                if (onTabSelected != null) {
                    onTabSelected.onTabSelected(viewId);
                }
            });
        }

        highlightTab(root, activeTabId, false);

        View glowView = root.findViewById(R.id.view_ai_skin_glow);
        View mySkinTab = root.findViewById(R.id.nav_myskin);
        if (glowView != null && mySkinTab != null) {
            NavAiSkinGlowAnimator.attach(glowView, mySkinTab);
        }
    }

    public static void highlightTab(View root, int activeTabId) {
        highlightTab(root, activeTabId, true);
    }

    public static void highlightTab(View root, int activeTabId, boolean animate) {
        for (int tabId : TAB_IDS) {
            ViewGroup tab = root.findViewById(tabId);
            if (tab == null) {
                continue;
            }

            if (tabId == R.id.nav_myskin) {
                ImageView icon = tab.findViewById(R.id.ivMySkin);
                if (icon != null) {
                    icon.clearColorFilter();
                    icon.setImageResource(R.drawable.ic_skin_mainbar_nav);
                }
                continue;
            }

            boolean isActive = tabId == activeTabId;
            ImageView icon = tab.findViewById(getIconId(tabId));
            TextView label = tab.findViewById(getLabelId(tabId));
            applyTabVisualState(icon, label, isActive, animate);
        }
    }

    private static void applyTabVisualState(ImageView icon, TextView label, boolean isActive, boolean animate) {
        int targetColor = isActive ? ACTIVE_COLOR : INACTIVE_COLOR;
        float targetScale = isActive ? ACTIVE_SCALE : 1f;

        if (icon != null) {
            if (animate) {
                animateColor(icon, isActive);
                icon.animate()
                        .scaleX(targetScale)
                        .scaleY(targetScale)
                        .setDuration(ANIM_DURATION_MS)
                        .setInterpolator(new FastOutSlowInInterpolator())
                        .start();
            } else {
                icon.setColorFilter(targetColor);
                icon.setScaleX(targetScale);
                icon.setScaleY(targetScale);
            }
        }

        if (label != null) {
            if (animate) {
                animateTextColor(label, isActive);
                label.animate()
                        .scaleX(isActive ? 1.04f : 1f)
                        .scaleY(isActive ? 1.04f : 1f)
                        .setDuration(ANIM_DURATION_MS)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            } else {
                label.setTextColor(targetColor);
                label.setScaleX(isActive ? 1.04f : 1f);
                label.setScaleY(isActive ? 1.04f : 1f);
            }
            label.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private static void animateColor(ImageView icon, boolean becomingActive) {
        int from = becomingActive ? INACTIVE_COLOR : ACTIVE_COLOR;
        int to = becomingActive ? ACTIVE_COLOR : INACTIVE_COLOR;
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        animator.setDuration(ANIM_DURATION_MS);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(a -> icon.setColorFilter((int) a.getAnimatedValue()));
        animator.start();
    }

    private static void animateTextColor(TextView label, boolean becomingActive) {
        int from = becomingActive ? INACTIVE_COLOR : ACTIVE_COLOR;
        int to = becomingActive ? ACTIVE_COLOR : INACTIVE_COLOR;
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
        animator.setDuration(ANIM_DURATION_MS);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(a -> label.setTextColor((int) a.getAnimatedValue()));
        animator.start();
    }

    private static int getLabelId(int tabId) {
        if (tabId == R.id.nav_home) return R.id.tv_nav_home;
        if (tabId == R.id.nav_shop) return R.id.tv_nav_shop;
        if (tabId == R.id.nav_community) return R.id.tv_nav_community;
        if (tabId == R.id.nav_account) return R.id.tv_nav_account;
        return 0;
    }

    private static int getIconId(int tabId) {
        if (tabId == R.id.nav_home) return R.id.iv_nav_home;
        if (tabId == R.id.nav_shop) return R.id.iv_nav_shop;
        if (tabId == R.id.nav_community) return R.id.iv_nav_community;
        if (tabId == R.id.nav_account) return R.id.iv_nav_account;
        return 0;
    }

    public static void showLoginRequiredDialog(Context context) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_login_required, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();

        View btnConfirm = dialogView.findViewById(R.id.btnConfirmLogin);
        View btnCancel = dialogView.findViewById(R.id.btnCancelLogin);

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(context, HomeWelcomeActivity.class);
                intent.putExtra("DIRECT_LOGIN", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialog.show();
    }

    public static void navigate(Fragment fragment, int tabId) {
        boolean isLoggedIn = ProfileSession.isLoggedIn(fragment.requireContext());
        Fragment target = null;

        if (tabId == R.id.nav_home) {
            target = new HomeFragment();
        } else if (tabId == R.id.nav_shop) {
            target = new ShopHomeFragment();
        } else if (tabId == R.id.nav_myskin) {
            target = new MySkinFragment();
        } else if (tabId == R.id.nav_community) {
            target = isLoggedIn ? new ComLoadingFragment() : new CommunityBeautyHubFragment();
        } else if (tabId == R.id.nav_account) {
            target = new AccountProfileFragment();
        }

        if (target != null) {
            View root = fragment.getView();
            if (root != null) {
                View navRoot = root.findViewById(R.id.bottom_nav);
                if (navRoot != null) {
                    highlightTab(navRoot, tabId, true);
                }
            }
            fragment.getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, target)
                    .commitAllowingStateLoss();
        }
    }
}
