package com.veganbeauty.app.features.home;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.veganbeauty.app.R;

public final class NotificationBadgeHelper {

    public static final int CONTAINER_DP = 36;
    public static final int BELL_DP = 22;
    public static final int BADGE_MIN_DP = 14;
    public static final int BADGE_MARGIN_DP = 0;
    public static final int BADGE_PADDING_HORIZONTAL_DP = 2;

    private NotificationBadgeHelper() {
    }

    public static void styleBadge(TextView badge, Context context) {
        if (badge == null || context == null) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int minSize = (int) (BADGE_MIN_DP * density);
        int horizontalPadding = (int) (BADGE_PADDING_HORIZONTAL_DP * density);

        badge.setLayoutParams(createBadgeLayoutParams(badge.getLayoutParams(), density));
        badge.setMinWidth(minSize);
        badge.setMinHeight(minSize);
        badge.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        badge.setIncludeFontPadding(false);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(context.getColor(R.color.white));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.home_bg_notification_badge);
    }

    public static FrameLayout.LayoutParams createBadgeLayoutParams(
            @Nullable ViewGroup.LayoutParams source,
            float density
    ) {
        FrameLayout.LayoutParams badgeLp;
        if (source instanceof FrameLayout.LayoutParams) {
            badgeLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.END
            );
        } else {
            badgeLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            badgeLp.gravity = Gravity.TOP | Gravity.END;
        }
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.topMargin = (int) (BADGE_MARGIN_DP * density);
        badgeLp.setMarginEnd((int) (BADGE_MARGIN_DP * density));
        return badgeLp;
    }

    public static void updateBadgeCount(TextView badge, @Nullable Integer count) {
        if (badge == null) {
            return;
        }
        if (count != null && count > 0) {
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    @Nullable
    public static TextView findBadgeView(View root) {
        if (root == null) {
            return null;
        }
        return root.findViewById(R.id.viewNotificationBadge);
    }

    @Nullable
    public static View findBellContainer(View root) {
        if (root == null) {
            return null;
        }
        View container = root.findViewById(R.id.home_header_notification_btn);
        if (container != null) {
            return container;
        }
        container = root.findViewById(R.id.layoutNotification);
        if (container != null) {
            return container;
        }
        View button = root.findViewById(R.id.btnNotification);
        if (button != null && button.getParent() instanceof View) {
            return (View) button.getParent();
        }
        return button;
    }

    public static void ensureBellContainer(ViewGroup container, Context context) {
        if (container == null || context == null) {
            return;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int containerSize = (int) (CONTAINER_DP * density);
        int bellSize = (int) (BELL_DP * density);

        ViewGroup.LayoutParams lp = container.getLayoutParams();
        if (lp != null) {
            if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.width > containerSize) {
                lp.width = containerSize;
            }
            if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height > containerSize) {
                lp.height = containerSize;
            }
            container.setLayoutParams(lp);
        }
        container.setClipChildren(false);
        container.setClipToPadding(false);

        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof TextView && child.getId() == R.id.viewNotificationBadge) {
                continue;
            }
            ViewGroup.LayoutParams childLp = child.getLayoutParams();
            if (childLp instanceof FrameLayout.LayoutParams) {
                childLp.width = bellSize;
                childLp.height = bellSize;
                ((FrameLayout.LayoutParams) childLp).gravity = Gravity.CENTER;
                child.setLayoutParams(childLp);
                child.setPadding(0, 0, 0, 0);
            }
        }
    }
}
