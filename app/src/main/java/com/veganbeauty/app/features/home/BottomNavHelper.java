package com.veganbeauty.app.features.home;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.com_feed.ComLoadingFragment;
import com.veganbeauty.app.features.home.welcome.HomeWelcomeActivity;
import com.veganbeauty.app.features.myskin.MySkinFragment;
import com.veganbeauty.app.features.profile.AccountProfileFragment;
import com.veganbeauty.app.features.shop.home.ShopHomeFragment;

public class BottomNavHelper {

    private static final String ACTIVE_COLOR = "#677559";
    private static final String INACTIVE_COLOR = "#DDDFC4";

    public interface OnTabSelectedListener {
        void onTabSelected(int tabId);
    }

    public static void setup(Fragment fragment, View root, int activeTabId, OnTabSelectedListener onTabSelected) {
        int[] tabs = {R.id.nav_home, R.id.nav_shop, R.id.nav_myskin, R.id.nav_community, R.id.nav_account};

        for (int viewId : tabs) {
            ViewGroup tab = root.findViewById(viewId);
            if (tab != null) {
                tab.setOnClickListener(v -> {
                    if (viewId != activeTabId) {
                        boolean isLoggedIn = ProfileSession.isLoggedIn(root.getContext());
                        if (!isLoggedIn && viewId == R.id.nav_account) {
                            showLoginRequiredDialog(root.getContext());
                            return;
                        }
                        if (onTabSelected != null) {
                            onTabSelected.onTabSelected(viewId);
                        }
                    }
                });
            }
        }

        highlightTab(root, activeTabId);
    }

    public static void highlightTab(View root, int activeTabId) {
        int[] tabIds = {R.id.nav_home, R.id.nav_shop, R.id.nav_myskin, R.id.nav_community, R.id.nav_account};

        for (int tabId : tabIds) {
            ViewGroup tab = root.findViewById(tabId);
            if (tab == null) continue;

            ImageView icon = null;
            TextView label = null;

            if (tab.getChildCount() > 0 && tab.getChildAt(0) instanceof ImageView) {
                icon = (ImageView) tab.getChildAt(0);
            }
            if (tab.getChildCount() > 1 && tab.getChildAt(1) instanceof TextView) {
                label = (TextView) tab.getChildAt(1);
            }

            boolean isActive = (tabId == activeTabId);
            String color = isActive ? ACTIVE_COLOR : INACTIVE_COLOR;

            if (tabId == R.id.nav_myskin) {
                if (icon != null) icon.clearColorFilter();
                ImageView ivMySkin = tab.findViewById(R.id.ivMySkin);
                View vMySkinShadow = tab.findViewById(R.id.vMySkinShadow);

                if (ivMySkin != null && ivMySkin.getTag(R.id.nav_myskin) instanceof Animator) {
                    ((Animator) ivMySkin.getTag(R.id.nav_myskin)).cancel();
                }
                if (vMySkinShadow != null && vMySkinShadow.getTag(R.id.nav_myskin) instanceof Animator) {
                    ((Animator) vMySkinShadow.getTag(R.id.nav_myskin)).cancel();
                }

                if (isActive) {
                    if (ivMySkin != null) {
                        ivMySkin.setImageResource(R.drawable.ic_skin_mainbar);
                        ivMySkin.setScaleX(1f);
                        ivMySkin.setScaleY(1f);
                    }

                    if (vMySkinShadow != null) {
                        vMySkinShadow.setVisibility(View.VISIBLE);
                        ObjectAnimator shadowAnim = ObjectAnimator.ofPropertyValuesHolder(
                                vMySkinShadow,
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.3f, 1.0f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.3f, 1.0f),
                                PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0.1f, 0.6f)
                        );
                        shadowAnim.setDuration(2000);
                        shadowAnim.setRepeatCount(ObjectAnimator.INFINITE);
                        shadowAnim.start();
                        vMySkinShadow.setTag(R.id.nav_myskin, shadowAnim);
                    }
                } else {
                    if (ivMySkin != null) {
                        ivMySkin.setImageResource(R.drawable.ic_skin_mainbar_nonactive);
                    }
                    if (vMySkinShadow != null) {
                        vMySkinShadow.setVisibility(View.GONE);
                        vMySkinShadow.setScaleX(1f);
                        vMySkinShadow.setScaleY(1f);
                    }

                    if (ivMySkin != null) {
                        ObjectAnimator iconAnim = ObjectAnimator.ofPropertyValuesHolder(
                                ivMySkin,
                                PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f),
                                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
                        );
                        iconAnim.setDuration(2000);
                        iconAnim.setRepeatCount(ObjectAnimator.INFINITE);
                        iconAnim.start();
                        ivMySkin.setTag(R.id.nav_myskin, iconAnim);
                    }
                }
                if (label != null) {
                    label.setTextColor(Color.parseColor(color));
                    label.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
                }
            } else {
                if (icon != null) {
                    icon.setColorFilter(Color.parseColor(color));
                    if (isActive) {
                        icon.setBackgroundResource(R.drawable.shape_nav_active_bg);
                    } else {
                        icon.setBackground(null);
                    }
                }
                if (label != null) {
                    label.setTextColor(Color.parseColor(color));
                    label.setTypeface(null, isActive ? Typeface.BOLD : Typeface.NORMAL);
                }
            }
        }
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
            fragment.getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, target)
                    .commit();
        }
    }
}
