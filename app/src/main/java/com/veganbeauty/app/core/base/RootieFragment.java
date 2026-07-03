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
import com.veganbeauty.app.features.home.NotificationBadgeHelper;

import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

public abstract class RootieFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupUI(view);

        try {
            if (!shouldSkipNotificationSync()) {
                injectNotificationButtonIfNeeded(view);
            }
            if (shouldSetupNotificationBell()) {
                setupNotificationBellAndBadge(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        observeViewModel();
    }

    private FrameLayout.LayoutParams createBadgeLayoutParams(
            @Nullable ViewGroup.LayoutParams source,
            float density
    ) {
        return NotificationBadgeHelper.createBadgeLayoutParams(source, density);
    }

    private void styleBadge(TextView badge, Context context) {
        NotificationBadgeHelper.styleBadge(badge, context);
    }

    protected abstract void setupUI(@NonNull View view);

    protected void observeViewModel() {
        // Observe ViewModel data
    }

    private boolean shouldSkipNotificationSync() {
        // Auto-inject/manipulation of the view tree is fragile and crashes on inner navigation.
        // Screens that need notification wire btn_notification in their own setupUI.
        return true;
    }

    protected boolean shouldSetupNotificationBell() {
        return true;
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
                (int) (22 * density),
                (int) (22 * density)
        );
        bellLp.gravity = Gravity.CENTER;
        bellIcon.setLayoutParams(bellLp);
        bellIcon.setImageResource(R.drawable.ic_notification);
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

                View cartContainer = cl.findViewById(R.id.home_header_cart_btn);

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
        View notificationBtn = NotificationBadgeHelper.findBellContainer(view);

        if (notificationBtn == null) {
            String[] notificationButtonIds = {
                    "btn_notification",
                    "ivNotification",
                    "iv_notification"
            };

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
        }

        if (notificationBtn == null) return;

        Context ctx = requireContext();
        float density = ctx.getResources().getDisplayMetrics().density;

        if (notificationBtn instanceof FrameLayout) {
            NotificationBadgeHelper.ensureBellContainer((FrameLayout) notificationBtn, ctx);
        } else if (notificationBtn.getParent() instanceof FrameLayout) {
            NotificationBadgeHelper.ensureBellContainer((FrameLayout) notificationBtn.getParent(), ctx);
        }

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
            styleBadge(badgeTextView, ctx);
        } else if (badgeView != null) {
            // Found a badge view but it's a plain View/dot. Replace with numbered badge on the bell corner.
            if (badgeView.getParent() instanceof ViewGroup) {
                ViewGroup parent = (ViewGroup) badgeView.getParent();
                int index = parent.indexOfChild(badgeView);

                TextView newBadge = new TextView(requireContext());
                newBadge.setId(badgeView.getId());
                styleBadge(newBadge, ctx);

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

                    int containerSize = (int) (NotificationBadgeHelper.CONTAINER_DP * density);
                    int bellSize = (int) (NotificationBadgeHelper.BELL_DP * density);

                    FrameLayout container = new FrameLayout(ctx);
                    container.setId(View.generateViewId());
                    originalParams.width = containerSize;
                    originalParams.height = containerSize;
                    container.setLayoutParams(originalParams);
                    container.setClipChildren(false);
                    container.setClipToPadding(false);

                    FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(bellSize, bellSize);
                    btnLp.gravity = Gravity.CENTER;
                    notificationBtn.setLayoutParams(btnLp);
                    notificationBtn.setPadding(0, 0, 0, 0);
                    if (notificationBtn instanceof ImageView) {
                        ((ImageView) notificationBtn).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }

                    parent.removeViewAt(index);
                    container.addView(notificationBtn);
                    parent.addView(container, index);
                    notificationBtn = container;
                }
            }

            // Now that notificationBtn is a ViewGroup (either original or wrapped FrameLayout), add the badge
            if (notificationBtn instanceof ViewGroup) {
                ViewGroup btnGroup = (ViewGroup) notificationBtn;

                if (btnGroup instanceof FrameLayout) {
                    NotificationBadgeHelper.ensureBellContainer(btnGroup, ctx);

                    if (btnGroup.getChildCount() > 0) {
                        View child = btnGroup.getChildAt(0);
                        if (!(child instanceof TextView) || child.getId() != R.id.viewNotificationBadge) {
                            int bellSize = (int) (NotificationBadgeHelper.BELL_DP * density);
                            ViewGroup.LayoutParams childLp = child.getLayoutParams();
                            if (childLp instanceof FrameLayout.LayoutParams) {
                                childLp.width = bellSize;
                                childLp.height = bellSize;
                                ((FrameLayout.LayoutParams) childLp).gravity = Gravity.CENTER;
                                child.setLayoutParams(childLp);
                                child.setPadding(0, 0, 0, 0);
                                if (child instanceof ImageView) {
                                    ((ImageView) child).setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                                }
                            }
                        }
                    }
                }

                TextView newBadge = new TextView(ctx);
                newBadge.setId(R.id.viewNotificationBadge);
                styleBadge(newBadge, ctx);
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

        // Only wire default navigation when the screen did not set its own handler in setupUI().
        if (!notificationBtn.hasOnClickListeners()) {
            notificationBtn.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new AccountNotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (badgeTextView != null) {
            TextView finalBadge = badgeTextView;
            new Thread(() -> {
                try {
                    // Chờ một chút để Fragment ổn định
                    Thread.sleep(500);
                    Context badgeContext = getContext();
                    if (badgeContext == null) return;

                    NotificationRepository.getInstance(badgeContext)
                            .getUnreadCount()
                            .collect(new kotlinx.coroutines.flow.FlowCollector<Integer>() {
                                @Override
                                public Object emit(Integer count, @NonNull kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
                                    androidx.fragment.app.FragmentActivity activity = getActivity();
                                    if (activity != null) {
                                        activity.runOnUiThread(() ->
                                                NotificationBadgeHelper.updateBadgeCount(finalBadge, count)
                                        );
                                    }
                                    return kotlin.Unit.INSTANCE;
                                }
                            }, new kotlin.coroutines.Continuation<kotlin.Unit>() {
                                @NonNull
                                @Override
                                public kotlin.coroutines.CoroutineContext getContext() {
                                    return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                                }
                                @Override
                                public void resumeWith(@NonNull Object o) {}
                            });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
