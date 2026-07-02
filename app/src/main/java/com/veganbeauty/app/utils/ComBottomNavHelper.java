package com.veganbeauty.app.utils;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.local.ProfileSession;
import com.veganbeauty.app.features.community.beauty_hub.CommunityBeautyHubFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityExploreFragment;
import com.veganbeauty.app.features.community.com_feed.CommunityFeedFragment;
import com.veganbeauty.app.features.community.message.CommunityMessageFragment;
import com.veganbeauty.app.features.community.profile.CommunityProfileFragment;
import com.veganbeauty.app.features.home.BottomNavHelper;
import com.veganbeauty.app.ui.widget.NavAiSkinGlowAnimator;

public final class ComBottomNavHelper {

    public static final int TAB_FEED = R.id.nav_com_feed;
    public static final int TAB_HUB = R.id.nav_com_hub;
    public static final int TAB_EXPLORE = R.id.nav_com_explore;
    public static final int TAB_CHAT = R.id.nav_com_chat;
    public static final int TAB_PROFILE = R.id.nav_com_profile;

    public static final int INTERCEPT_NOT_HANDLED = 0;
    public static final int INTERCEPT_CONSUME = 1;
    public static final int INTERCEPT_CONSUME_NO_HIGHLIGHT = 2;

    public interface TabClickInterceptor {
        int onTabClick(int tabId);
    }

    private static final int[] TAB_IDS = {
            TAB_FEED, TAB_HUB, TAB_EXPLORE, TAB_CHAT, TAB_PROFILE
    };
    private static final int[] ICON_IDS = {
            R.id.iv_nav_com_feed,
            R.id.iv_nav_com_hub,
            R.id.iv_nav_com_explore,
            R.id.iv_nav_com_chat,
            R.id.iv_nav_com_profile
    };
    private static final int[] LABEL_IDS = {
            R.id.tv_nav_com_feed,
            R.id.tv_nav_com_hub,
            R.id.tv_nav_com_explore,
            R.id.tv_nav_com_chat,
            R.id.tv_nav_com_profile
    };
    private static final int[] GLOW_IDS = {
            R.id.view_glow_com_feed,
            R.id.view_glow_com_hub,
            R.id.view_glow_com_explore,
            R.id.view_glow_com_chat,
            R.id.view_glow_com_profile
    };

    private ComBottomNavHelper() {
    }

    public static void setup(@NonNull Fragment fragment, @NonNull View navRoot, int activeTabId) {
        setup(fragment, navRoot, activeTabId, null);
    }

    public static void setup(
            @NonNull Fragment fragment,
            @NonNull View navRoot,
            int activeTabId,
            @Nullable TabClickInterceptor interceptor
    ) {
        highlightTab(navRoot, activeTabId, navRoot);
        Context context = navRoot.getContext();

        for (int tabId : TAB_IDS) {
            View tab = navRoot.findViewById(tabId);
            if (tab == null) {
                continue;
            }
            tab.setOnClickListener(v -> {
                if (tabId == TAB_CHAT && !ProfileSession.isLoggedIn(context)) {
                    BottomNavHelper.showLoginRequiredDialog(context);
                    return;
                }
                if (interceptor != null) {
                    int interceptResult = interceptor.onTabClick(tabId);
                    if (interceptResult == INTERCEPT_CONSUME) {
                        highlightTab(navRoot, tabId, navRoot);
                        return;
                    }
                    if (interceptResult == INTERCEPT_CONSUME_NO_HIGHLIGHT) {
                        return;
                    }
                }
                if (tabId == activeTabId) {
                    return;
                }
                navigate(fragment, tabId);
            });
        }
    }

    public static void highlightTab(@NonNull View navRoot, int activeTabId, @NonNull View lifecycleView) {
        Context context = navRoot.getContext();
        int activeColor = ContextCompat.getColor(context, R.color.primary);
        int inactiveColor = ContextCompat.getColor(context, R.color.tertiary);

        for (int i = 0; i < TAB_IDS.length; i++) {
            boolean isActive = TAB_IDS[i] == activeTabId;
            ImageView icon = navRoot.findViewById(ICON_IDS[i]);
            TextView label = navRoot.findViewById(LABEL_IDS[i]);
            View glow = navRoot.findViewById(GLOW_IDS[i]);

            if (icon != null) {
                icon.setColorFilter(isActive ? activeColor : inactiveColor);
            }
            if (label != null) {
                label.setTextColor(isActive ? activeColor : inactiveColor);
                label.getPaint().setFakeBoldText(isActive);
                label.invalidate();
            }
            if (glow != null) {
                if (isActive) {
                    glow.setVisibility(View.VISIBLE);
                    NavAiSkinGlowAnimator.attach(glow, lifecycleView);
                } else {
                    glow.setVisibility(View.INVISIBLE);
                    NavAiSkinGlowAnimator.detach(glow);
                }
            }
        }
    }

    private static void navigate(@NonNull Fragment fragment, int tabId) {
        Fragment target;
        if (tabId == TAB_FEED) {
            target = new CommunityFeedFragment();
        } else if (tabId == TAB_HUB) {
            target = new CommunityBeautyHubFragment();
        } else if (tabId == TAB_EXPLORE) {
            target = new CommunityExploreFragment();
        } else if (tabId == TAB_CHAT) {
            target = new CommunityMessageFragment();
        } else if (tabId == TAB_PROFILE) {
            target = new CommunityProfileFragment();
        } else {
            return;
        }

        fragment.getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, target)
                .commitAllowingStateLoss();
    }
}
