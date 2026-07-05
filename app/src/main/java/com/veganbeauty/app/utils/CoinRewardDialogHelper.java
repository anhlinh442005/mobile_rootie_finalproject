package com.veganbeauty.app.utils;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Locale;

public final class CoinRewardDialogHelper {

    public static final String RESULT_DISMISSED = "coin_reward_dismissed";

    private static final String TAG = "coin_reward_dialog";

    private CoinRewardDialogHelper() {
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

    public static void show(Fragment fragment, int coins) {
        show(fragment, coins, null);
    }

    public static void show(Fragment fragment, int coins, @Nullable String sourceMessage) {
        if (fragment == null || !fragment.isAdded()) {
            return;
        }
        show(fragment.getParentFragmentManager(), coins, sourceMessage);
    }

    public static void show(FragmentActivity activity, int coins, @Nullable String sourceMessage) {
        if (activity == null || activity.isFinishing()) {
            return;
        }
        show(activity.getSupportFragmentManager(), coins, sourceMessage);
    }

    public static void show(FragmentManager fragmentManager, int coins, @Nullable String sourceMessage) {
        if (fragmentManager == null || coins <= 0) {
            return;
        }
        if (fragmentManager.findFragmentByTag(TAG) != null) {
            return;
        }
        CoinRewardDialogFragment.newInstance(coins, sourceMessage).show(fragmentManager, TAG);
    }
}
