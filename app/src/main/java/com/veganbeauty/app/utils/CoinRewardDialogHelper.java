package com.veganbeauty.app.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.Locale;

public final class CoinRewardDialogHelper {

    public static final String RESULT_DISMISSED = "coin_reward_dismissed";

    private static final String TAG = "coin_reward_dialog";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static WeakReference<FragmentActivity> hostActivity;

    private static final class PendingReward {
        int coins;
        int totalBalance;
        @Nullable String source;
    }

    private static PendingReward pendingReward;

    private CoinRewardDialogHelper() {
    }

    public static void registerHost(FragmentActivity activity) {
        if (activity == null) {
            return;
        }
        hostActivity = new WeakReference<>(activity);
        flushPendingReward();
    }

    public static void unregisterHost(FragmentActivity activity) {
        if (hostActivity != null && hostActivity.get() == activity) {
            hostActivity = null;
        }
    }

    public static String formatCoinValue(int coins) {
        return String.format(Locale.US, "%,d", Math.max(coins, 0)).replace(',', '.');
    }

    public static String formatAmountLabel(int coins) {
        return formatCoinValue(coins) + " xu";
    }

    public static String formatBadgeLabel(int coins) {
        return "+" + formatCoinValue(coins) + " xu";
    }

    public static String formatBalanceSubtitle(int totalBalance) {
        return "Bạn hiện có: " + formatCoinValue(totalBalance) + " xu";
    }

    public static void showPending(Context context, int coins, int totalBalance, @Nullable String sourceMessage) {
        if (coins <= 0) {
            return;
        }
        MAIN_HANDLER.post(() -> {
            FragmentActivity activity = resolveHost(context);
            if (activity == null || activity.isFinishing()) {
                PendingReward pending = new PendingReward();
                pending.coins = coins;
                pending.totalBalance = totalBalance;
                pending.source = sourceMessage;
                pendingReward = pending;
                return;
            }
            show(activity.getSupportFragmentManager(), coins, totalBalance, sourceMessage);
        });
    }

    private static void flushPendingReward() {
        if (pendingReward == null) {
            return;
        }
        FragmentActivity activity = hostActivity != null ? hostActivity.get() : null;
        if (activity == null || activity.isFinishing()) {
            return;
        }
        PendingReward pending = pendingReward;
        pendingReward = null;
        show(activity.getSupportFragmentManager(), pending.coins, pending.totalBalance, pending.source);
    }

    @Nullable
    private static FragmentActivity resolveHost(Context context) {
        FragmentActivity activity = hostActivity != null ? hostActivity.get() : null;
        if (activity == null && context instanceof FragmentActivity) {
            activity = (FragmentActivity) context;
        }
        return activity;
    }

    public static void show(Fragment fragment, int coins) {
        show(fragment, coins, null);
    }

    public static void show(Fragment fragment, int coins, @Nullable String sourceMessage) {
        if (fragment == null || !fragment.isAdded()) {
            return;
        }
        int total = RewardPointsHelper.getTotalPoints(fragment.requireContext());
        show(fragment.getParentFragmentManager(), coins, total, sourceMessage);
    }

    public static void show(FragmentActivity activity, int coins, @Nullable String sourceMessage) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        int total = RewardPointsHelper.getTotalPoints(activity);
        show(activity.getSupportFragmentManager(), coins, total, sourceMessage);
    }

    public static void show(FragmentManager fragmentManager, int coins, @Nullable String sourceMessage) {
        if (fragmentManager == null || coins <= 0) {
            return;
        }
        Context context = null;
        if (fragmentManager.getFragments() != null && !fragmentManager.getFragments().isEmpty()) {
            Fragment fragment = fragmentManager.getFragments().get(0);
            if (fragment != null) {
                context = fragment.getContext();
            }
        }
        int total = context != null ? RewardPointsHelper.getTotalPoints(context) : 0;
        show(fragmentManager, coins, total, sourceMessage);
    }

    public static void show(FragmentManager fragmentManager, int coins, int totalBalance, @Nullable String sourceMessage) {
        if (fragmentManager == null || coins <= 0) {
            return;
        }
        Fragment existing = fragmentManager.findFragmentByTag(TAG);
        if (existing instanceof androidx.fragment.app.DialogFragment) {
            ((androidx.fragment.app.DialogFragment) existing).dismissAllowingStateLoss();
        }
        CoinRewardDialogFragment dialog = CoinRewardDialogFragment.newInstance(coins, totalBalance, sourceMessage);
        try {
            if (!fragmentManager.isStateSaved()) {
                dialog.showNow(fragmentManager, TAG);
            } else {
                fragmentManager.beginTransaction()
                        .add(dialog, TAG)
                        .commitAllowingStateLoss();
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (!fragmentManager.isStateSaved()) {
                    dialog.show(fragmentManager, TAG);
                } else {
                    fragmentManager.beginTransaction()
                            .add(dialog, TAG)
                            .commitAllowingStateLoss();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Shows the coin reward popup and runs {@code onDismiss} after the user closes it.
     */
    public static void showWithDismissCallback(
            @NonNull Fragment fragment,
            int coins,
            int totalBalance,
            @Nullable String sourceMessage,
            @Nullable Runnable onDismiss
    ) {
        if (!fragment.isAdded() || coins <= 0) {
            if (onDismiss != null) {
                onDismiss.run();
            }
            return;
        }
        FragmentManager fm = fragment.getParentFragmentManager();
        LifecycleOwner lifecycleOwner = fragment.getViewLifecycleOwner();
        fm.setFragmentResultListener(
                RESULT_DISMISSED,
                lifecycleOwner,
                (requestKey, result) -> {
                    fm.clearFragmentResultListener(RESULT_DISMISSED);
                    if (onDismiss != null && fragment.isAdded()) {
                        onDismiss.run();
                    }
                });
        show(fm, coins, totalBalance, sourceMessage);
    }

    /**
     * Shows the coin reward popup from an activity and runs {@code onDismiss} after close.
     */
    public static void showWithDismissCallback(
            @NonNull FragmentActivity activity,
            int coins,
            int totalBalance,
            @Nullable String sourceMessage,
            @Nullable Runnable onDismiss
    ) {
        if (activity.isFinishing() || coins <= 0) {
            if (onDismiss != null) {
                onDismiss.run();
            }
            return;
        }
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            if (onDismiss != null) {
                fm.setFragmentResultListener(
                        RESULT_DISMISSED,
                        activity,
                        (requestKey, result) -> {
                            fm.clearFragmentResultListener(RESULT_DISMISSED);
                            if (!activity.isFinishing()) {
                                onDismiss.run();
                            }
                        });
            }
            show(fm, coins, totalBalance, sourceMessage);
        } catch (Exception e) {
            e.printStackTrace();
            if (onDismiss != null) {
                onDismiss.run();
            }
        }
    }
}
