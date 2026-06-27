package com.veganbeauty.app.core.base;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwnerKt;

import com.veganbeauty.app.R;
import com.veganbeauty.app.data.repository.NotificationRepository;
import com.veganbeauty.app.features.account.notification.AccountNotificationFragment;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public abstract class RootieFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI(view);

        if (!shouldSkipNotificationSync()) {
            injectNotificationButtonIfNeeded(view);
            try {
                setupNotificationBellAndBadge(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        observeViewModel();
    }

    protected abstract void setupUI(@NonNull View view);

    protected void observeViewModel() {
        // Observe ViewModel data
    }

    private boolean shouldSkipNotificationSync() {
        String className = this.getClass().getSimpleName();
        String fullName = this.getClass().getName();

        // Skip community fragments (com_ / community)
        if (className.toLowerCase().startsWith("com") ||
                fullName.toLowerCase().contains("community")) {
            return true;
        }

        // Skip shop fragments (shop_ / shop)
        if (className.toLowerCase().startsWith("shop") ||
                fullName.toLowerCase().contains("shop")) {
            return true;
        }

        // Skip AccountNotificationFragment itself, to avoid infinite self-navigation/redundant UI
        if ("AccountNotificationFragment".equals(className)) {
            return true;
        }

        return false;
    }

    private void injectNotificationButtonIfNeeded(View view) {
        String[] headerIds = {
                "clHeader", "topBar", "toolbar", "header",
                "skin_header", "skin_history_header", "skin_scan_header",
                "branch_header", "booking_header", "skin_detail_header",
                "skin_scan_result_header", "rl_header"
        };
        ViewGroup headerView = null;
        for (String idStr : headerIds) {
            int resId = getResources().getIdentifier(idStr, "id", requireContext().getPackageName());
            if (resId != 0) {
                View found = view.findViewById(resId);
                if (found instanceof ViewGroup) {
                    headerView = (ViewGroup) found;
                    break;
                }
            }
        }

        if (headerView == null) return;

        // Check if there is already a notification button in the view tree
        String[] notificationButtonIds = {
                "home_header_notification_btn",
                "btnNotification",
                "btn_notification",
                "ivNotification",
                "iv_notification"
        };

        boolean hasNotificationBtn = false;
        for (String idStr : notificationButtonIds) {
            int resId = getResources().getIdentifier(idStr, "id", requireContext().getPackageName());
            if (resId != 0) {
                if (view.findViewById(resId) != null) {
                    hasNotificationBtn = true;
                    break;
                }
            }
        }

        if (hasNotificationBtn) return;

        // No notification button found, let's inject one!
        Context ctx = requireContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        FrameLayout notificationContainer = new FrameLayout(ctx);
        notificationContainer.setId(R.id.btnNotification);
        notificationContainer.setLayoutParams(new ViewGroup.LayoutParams(
                (int) (40 * density),
                (int) (40 * density)
        ));
        TypedValue outValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true) && outValue.resourceId != 0) {
            notificationContainer.setBackgroundResource(outValue.resourceId);
        }
        notificationContainer.setClickable(true);
        notificationContainer.setFocusable(true);
        notificationContainer.setClipChildren(false);
        notificationContainer.setClipToPadding(false);

        ImageView bellIcon = new ImageView(ctx);
        FrameLayout.LayoutParams bellLp = new FrameLayout.LayoutParams(
                (int) (26 * density),
                (int) (26 * density)
        );
        bellLp.gravity = Gravity.CENTER;
        bellIcon.setLayoutParams(bellLp);
        bellIcon.setImageResource(R.drawable.home_ic_notification);
        int tintColor = Color.parseColor("#3E4D44");
        bellIcon.setColorFilter(tintColor);
        notificationContainer.addView(bellIcon);

        int size = (int) (14 * density);
        TextView badge = new TextView(ctx);
        badge.setId(R.id.viewNotificationBadge);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size);
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.topMargin = (int) (2 * density);
        badgeLp.rightMargin = (int) (2 * density);
        badge.setLayoutParams(badgeLp);
        badge.setMinWidth(size);
        badge.setPadding((int) (4 * density), 0, (int) (4 * density), 0);
        badge.setGravity(Gravity.CENTER);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.home_bg_notification_badge);
        badge.setVisibility(View.GONE);
        notificationContainer.addView(badge);

        // Add to header view
        if (headerView instanceof ConstraintLayout) {
            try {
                ConstraintLayout cl = (ConstraintLayout) headerView;
                // Ensure all children of the ConstraintLayout have a valid ID to prevent ConstraintSet.clone crash
                for (int i = 0; i < cl.getChildCount(); i++) {
                    View child = cl.getChildAt(i);
                    if (child.getId() == View.NO_ID) {
                        child.setId(View.generateViewId());
                    }
                }

                View cartContainer = cl.findViewById(R.id.flCartContainer);

                cl.addView(notificationContainer);
                ConstraintSet set = new ConstraintSet();
                set.clone(cl);

                if (cartContainer != null) {
                    // For ShopHomeFragment: insert notification button to the left of Cart
                    ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                            (int) (40 * density),
                            (int) (40 * density)
                    );
                    lp.setMarginEnd((int) (8 * density));
                    notificationContainer.setLayoutParams(lp);

                    set.connect(notificationContainer.getId(), ConstraintSet.END, cartContainer.getId(), ConstraintSet.START);
                    set.connect(notificationContainer.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    set.connect(notificationContainer.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                } else {
                    // Standard header: align to end of parent
                    ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                            (int) (40 * density),
                            (int) (40 * density)
                    );
                    lp.setMarginEnd((int) (16 * density));
                    notificationContainer.setLayoutParams(lp);

                    set.connect(notificationContainer.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END);
                    set.connect(notificationContainer.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP);
                    set.connect(notificationContainer.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM);
                }
                set.applyTo(cl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (headerView instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) headerView;
            // Adjust layout_marginEnd of TextView to center the title
            for (int i = 0; i < ll.getChildCount(); i++) {
                View child = ll.getChildAt(i);
                if (child instanceof TextView) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) child.getLayoutParams();
                    if (lp != null && lp.weight > 0) {
                        lp.setMarginEnd(0);
                        child.setLayoutParams(lp);
                    }
                }
            }
            ll.addView(notificationContainer);
        } else if (headerView instanceof RelativeLayout) {
            RelativeLayout rl = (RelativeLayout) headerView;
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    (int) (40 * density),
                    (int) (40 * density)
            );
            lp.addRule(RelativeLayout.ALIGN_PARENT_END);
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            lp.setMarginEnd((int) (16 * density));
            notificationContainer.setLayoutParams(lp);
            rl.addView(notificationContainer);
        }
    }

    private void setupNotificationBellAndBadge(View view) {
        // Find existing notification button in the view tree
        String[] notificationButtonIds = {
                "home_header_notification_btn",
                "btnNotification",
                "btn_notification",
                "ivNotification",
                "iv_notification"
        };

        View notificationBtn = null;
        for (String idStr : notificationButtonIds) {
            int resId = getResources().getIdentifier(idStr, "id", requireContext().getPackageName());
            if (resId != 0) {
                View found = view.findViewById(resId);
                if (found != null) {
                    notificationBtn = found;
                    break;
                }
            }
        }

        if (notificationBtn == null) return;

        // Find pre-existing notification badge in the view tree
        String[] badgeIds = {
                "home_header_notification_badge",
                "viewNotificationBadge",
                "view_notification_badge",
                "notificationBadge",
                "notification_badge"
        };

        View badgeView = null;
        for (String idStr : badgeIds) {
            int resId = getResources().getIdentifier(idStr, "id", requireContext().getPackageName());
            if (resId != 0) {
                View found = view.findViewById(resId);
                if (found != null) {
                    badgeView = found;
                    break;
                }
            }
        }

        TextView badgeTextView = null;

        if (badgeView instanceof TextView) {
            badgeTextView = (TextView) badgeView;
        } else if (badgeView != null) {
            // Found a badge view but it's a plain View/dot. Let's replace it with a TextView badge.
            if (badgeView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) badgeView.getParent();
                int index = parent.indexOfChild(badgeView);
                ViewGroup.LayoutParams layoutParams = badgeView.getLayoutParams();

                TextView newBadge = new TextView(requireContext());
                newBadge.setId(badgeView.getId());
                newBadge.setLayoutParams(layoutParams);
                newBadge.setGravity(Gravity.CENTER);
                newBadge.setTextColor(Color.WHITE);
                newBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
                newBadge.setTypeface(null, Typeface.BOLD);
                newBadge.setBackgroundResource(R.drawable.home_bg_notification_badge);

                parent.removeViewAt(index);
                parent.addView(newBadge, index);
                badgeTextView = newBadge;
            }
        } else {
            // No badge view found.
            // If the button is not a ViewGroup (e.g. it is a simple ImageView), let's wrap it in a FrameLayout.
            if (!(notificationBtn instanceof ViewGroup)) {
                if (notificationBtn.getParent() instanceof ViewGroup) {
                    ViewGroup parent = (ViewGroup) notificationBtn.getParent();
                    int index = parent.indexOfChild(notificationBtn);
                    ViewGroup.LayoutParams originalParams = notificationBtn.getLayoutParams();

                    Context ctx = requireContext();
                    float density = ctx.getResources().getDisplayMetrics().density;

                    int originalWidth = originalParams.width;
                    int originalHeight = originalParams.height;

                    int containerWidth = (originalWidth <= 0 || originalWidth < (40 * density)) ? (int) (40 * density) : originalWidth;
                    int containerHeight = (originalHeight <= 0 || originalHeight < (40 * density)) ? (int) (40 * density) : originalHeight;

                    FrameLayout container = new FrameLayout(ctx);
                    container.setId(View.generateViewId());
                    originalParams.width = containerWidth;
                    originalParams.height = containerHeight;
                    container.setLayoutParams(originalParams);
                    container.setClipChildren(false);
                    container.setClipToPadding(false);

                    FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(
                            (originalWidth > 0) ? originalWidth : (int) (24 * density),
                            (originalHeight > 0) ? originalHeight : (int) (24 * density)
                    );
                    btnLp.gravity = Gravity.CENTER;
                    notificationBtn.setLayoutParams(btnLp);

                    parent.removeViewAt(index);
                    container.addView(notificationBtn);
                    parent.addView(container, index);
                    notificationBtn = container;
                }
            }

            // Now that notificationBtn is a ViewGroup (either original or wrapped FrameLayout), let's add the badge
            if (notificationBtn instanceof ViewGroup) {
                ViewGroup btnGroup = (ViewGroup) notificationBtn;
                Context ctx = requireContext();
                float density = ctx.getResources().getDisplayMetrics().density;

                // If it is a FrameLayout, let's make sure it is at least 40dp x 40dp and center its child to prevent badge clipping.
                if (btnGroup instanceof FrameLayout) {
                    ViewGroup.LayoutParams lp = btnGroup.getLayoutParams();
                    if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT || lp.width < (int) (40 * density)) {
                        lp.width = (int) (40 * density);
                    }
                    if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT || lp.height < (int) (40 * density)) {
                        lp.height = (int) (40 * density);
                    }
                    btnGroup.setLayoutParams(lp);

                    if (btnGroup.getChildCount() > 0) {
                        View child = btnGroup.getChildAt(0);
                        ViewGroup.LayoutParams childLp = child.getLayoutParams();
                        if (childLp instanceof FrameLayout.LayoutParams) {
                            ((FrameLayout.LayoutParams) childLp).gravity = Gravity.CENTER;
                            child.setLayoutParams(childLp);
                        }
                    }
                }

                int size = (int) (14 * density);
                TextView newBadge = new TextView(ctx);
                newBadge.setId(R.id.viewNotificationBadge);
                FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, size);
                badgeLp.gravity = Gravity.TOP | Gravity.END;
                badgeLp.topMargin = (int) (2 * density);
                badgeLp.rightMargin = (int) (2 * density);
                newBadge.setLayoutParams(badgeLp);
                newBadge.setMinWidth(size);
                newBadge.setPadding((int) (4 * density), 0, (int) (4 * density), 0);
                newBadge.setGravity(Gravity.CENTER);
                newBadge.setTextColor(Color.WHITE);
                newBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f);
                newBadge.setTypeface(null, Typeface.BOLD);
                newBadge.setBackgroundResource(R.drawable.home_bg_notification_badge);
                btnGroup.addView(newBadge);
                badgeTextView = newBadge;
            }
        }

        // Prevent clipping by setting clipChildren and clipToPadding to false on all parents and the button container itself
        if (notificationBtn instanceof ViewGroup) {
            ((ViewGroup) notificationBtn).setClipChildren(false);
            ((ViewGroup) notificationBtn).setClipToPadding(false);
        }
        android.view.ViewParent p = notificationBtn.getParent();
        while (p instanceof ViewGroup) {
            ((ViewGroup) p).setClipChildren(false);
            ((ViewGroup) p).setClipToPadding(false);
            p = p.getParent();
        }

        // Bind click listener to navigate to AccountNotificationFragment
        notificationBtn.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AccountNotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        if (badgeTextView != null) {
            TextView finalBadge = badgeTextView;
            BuildersKt.launch(LifecycleOwnerKt.getLifecycleScope(getViewLifecycleOwner()), Dispatchers.getMain(), kotlinx.coroutines.CoroutineStart.DEFAULT, (coroutineScope, continuation) -> {
                NotificationRepository.getInstance(requireContext())
                        .getUnreadCount()
                        .collect(new kotlinx.coroutines.flow.FlowCollector<Integer>() {
                            @Nullable
                            @Override
                            public Object emit(Integer count, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                                if (count > 0) {
                                    finalBadge.setText(String.valueOf(count));
                                    finalBadge.setVisibility(View.VISIBLE);
                                } else {
                                    finalBadge.setVisibility(View.GONE);
                                }
                                return kotlin.Unit.INSTANCE;
                            }
                        }, continuation);
                return kotlin.Unit.INSTANCE;
            });
        }
    }
}
