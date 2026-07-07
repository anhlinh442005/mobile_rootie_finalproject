package com.veganbeauty.app.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;

public final class RewardPointsHelper {

    private RewardPointsHelper() {
    }

    public static int getTotalPoints(Context context) {
        if (context == null) {
            return 0;
        }
        try {
            return RootieDatabase.getDatabase(context.getApplicationContext())
                    .rewardPointDao()
                    .getTotalPointsSync();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Adds coins to the user's wallet, syncs Firebase, and shows the reward dialog.
     *
     * @return new total balance after the award
     */
    public static int awardPoints(
            Context context,
            String orderId,
            int points,
            String reason,
            @Nullable String sourceLabel
    ) {
        return awardPoints(context, orderId, points, reason, sourceLabel, true);
    }

    public static int awardPoints(
            Context context,
            String orderId,
            int points,
            String reason,
            @Nullable String sourceLabel,
            boolean showDialog
    ) {
        if (context == null || points <= 0) {
            return getTotalPoints(context);
        }
        Context appContext = context.getApplicationContext();
        try {
            RootieDatabase.getDatabase(appContext).rewardPointDao().insertRewardPoints(
                    new RewardPointEntity(
                            0,
                            orderId != null ? orderId : "",
                            points,
                            reason != null ? reason : "",
                            System.currentTimeMillis()
                    )
            );
            SyncDataHelper.syncRewardPointsToFirestore(appContext);
            int total = getTotalPoints(appContext);
            if (showDialog) {
                String label = sourceLabel != null && !sourceLabel.trim().isEmpty()
                        ? sourceLabel.trim()
                        : reason;
                CoinRewardDialogHelper.showPending(appContext, points, total, label);
            }
            return total;
        } catch (Exception e) {
            e.printStackTrace();
            return getTotalPoints(appContext);
        }
    }
}
