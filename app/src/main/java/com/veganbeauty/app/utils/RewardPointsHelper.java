package com.veganbeauty.app.utils;

import android.content.Context;

import androidx.annotation.Nullable;

import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.local.entities.RewardPointEntity;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public final class RewardPointsHelper {

    private RewardPointsHelper() {
    }

    @Nullable
    private static String resolveUserId(Context context) {
        if (context == null) {
            return null;
        }
        String userId = ProfileSessionHelper.getEffectiveUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return userId.trim();
    }

    public static int getTotalPoints(Context context) {
        String userId = resolveUserId(context);
        if (userId == null) {
            return 0;
        }
        try {
            return RootieDatabase.getDatabase(context.getApplicationContext())
                    .rewardPointDao()
                    .getTotalPointsSync(userId);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static boolean hasAwardForOrderIdToday(Context context, String orderId) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return hasAwardForOrderIdSince(context, orderId, cal.getTimeInMillis());
    }

    /** True nếu user đã có bản ghi thưởng với orderId kể từ sinceMs. */
    public static boolean hasAwardForOrderIdSince(Context context, String orderId, long sinceMs) {
        String userId = resolveUserId(context);
        if (userId == null || orderId == null || orderId.trim().isEmpty()) {
            return false;
        }
        try {
            List<RewardPointEntity> history = RootieDatabase.getDatabase(context.getApplicationContext())
                    .rewardPointDao()
                    .getHistorySince(userId, sinceMs);
            for (RewardPointEntity entry : history) {
                if (orderId.equals(entry.getOrderId())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    public static void removeRewardsByOrderIdToday(Context context, String orderId) {
        String userId = resolveUserId(context);
        if (userId == null || orderId == null || orderId.trim().isEmpty()) {
            return;
        }
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Context appContext = context.getApplicationContext();
            RootieDatabase.getDatabase(appContext).rewardPointDao()
                    .deleteRewardsByOrderIdSince(userId, orderId, cal.getTimeInMillis());
            SyncDataHelper.syncRewardPointsToFirestore(appContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Trừ xu khi đổi quà — ghi âm vào lịch sử điểm của đúng user.
     *
     * @return số dư mới; -1 nếu không đủ xu / chưa đăng nhập
     */
    public static int spendPoints(
            Context context,
            String orderId,
            int points,
            String reason
    ) {
        String userId = resolveUserId(context);
        if (context == null || userId == null || points <= 0) {
            return -1;
        }
        Context appContext = context.getApplicationContext();
        try {
            int current = getTotalPoints(appContext);
            if (current < points) {
                return -1;
            }
            RootieDatabase.getDatabase(appContext).rewardPointDao().insertRewardPoints(
                    new RewardPointEntity(
                            0,
                            userId,
                            orderId != null ? orderId : "",
                            -points,
                            reason != null ? reason : "",
                            System.currentTimeMillis()
                    )
            );
            SyncDataHelper.syncRewardPointsToFirestore(appContext);
            return getTotalPoints(appContext);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int awardPoints(
            Context context,
            String orderId,
            int points,
            String reason,
            @Nullable String sourceLabel,
            boolean showDialog
    ) {
        String userId = resolveUserId(context);
        if (context == null || userId == null || points <= 0) {
            return getTotalPoints(context);
        }
        Context appContext = context.getApplicationContext();
        try {
            RootieDatabase.getDatabase(appContext).rewardPointDao().insertRewardPoints(
                    new RewardPointEntity(
                            0,
                            userId,
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

    public static List<RewardPointEntity> getHistorySince(Context context, long sinceMs) {
        String userId = resolveUserId(context);
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            return RootieDatabase.getDatabase(context.getApplicationContext())
                    .rewardPointDao()
                    .getHistorySince(userId, sinceMs);
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static int getPointsEarnedSince(Context context, long sinceMs) {
        String userId = resolveUserId(context);
        if (userId == null) {
            return 0;
        }
        try {
            return RootieDatabase.getDatabase(context.getApplicationContext())
                    .rewardPointDao()
                    .getPointsEarnedSince(userId, sinceMs);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
