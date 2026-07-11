package com.veganbeauty.app.features.home;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.FlowLiveDataConversions;

import com.veganbeauty.app.R;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;

import kotlinx.coroutines.flow.Flow;

/**
 * Single entry for the account/general notification bell outside community.
 * Community screens keep their own destination and unread flow.
 */
public final class NotificationBellHelper {

    private NotificationBellHelper() {
    }

    public static void openAccountInbox(Fragment fragment) {
        if (fragment == null || !fragment.isAdded()) {
            return;
        }
        fragment.getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AccountNotificationFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public static void bindClick(@Nullable View bell, Fragment fragment) {
        if (bell == null || fragment == null) {
            return;
        }
        // Icon children (esp. ImageButton) must not swallow taps meant for the container.
        if (bell instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) bell;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof android.widget.ImageView
                        && child.getId() != R.id.viewNotificationBadge
                        && child.getId() != R.id.home_header_notification_badge) {
                    child.setClickable(false);
                    child.setFocusable(false);
                }
            }
        }
        bell.setClickable(true);
        bell.setFocusable(true);
        bell.setOnClickListener(v -> openAccountInbox(fragment));
    }

    public static void bindBadge(
            Fragment fragment,
            @Nullable TextView badge,
            @Nullable Flow<Integer> unreadCountFlow
    ) {
        if (fragment == null || badge == null || unreadCountFlow == null || fragment.getContext() == null) {
            return;
        }
        NotificationBadgeHelper.styleBadge(badge, fragment.requireContext());
        FlowLiveDataConversions.asLiveData(unreadCountFlow)
                .observe(fragment.getViewLifecycleOwner(), count ->
                        NotificationBadgeHelper.updateBadgeCount(badge, count));
    }
}
