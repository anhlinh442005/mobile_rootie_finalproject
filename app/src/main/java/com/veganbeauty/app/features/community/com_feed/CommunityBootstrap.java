package com.veganbeauty.app.features.community.com_feed;

import android.content.Context;
import android.util.Log;

import com.veganbeauty.app.data.local.LocalJsonReader;
import com.veganbeauty.app.data.local.RootieDatabase;
import com.veganbeauty.app.data.remote.FirestoreService;
import com.veganbeauty.app.data.repository.CommunityRepository;
import com.veganbeauty.app.features.community.CommunitySocialHelper;
import com.veganbeauty.app.features.community.UserSocialSeeder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-time community data bootstrap. Must run off the main thread.
 */
public final class CommunityBootstrap {

    private static final String TAG = "CommunityBootstrap";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile boolean localSeedReady = false;

    private CommunityBootstrap() {
    }

    public static boolean isLocalSeedReady() {
        return localSeedReady;
    }

    public static void markLocalSeedReady() {
        localSeedReady = true;
    }

    public static void ensureLoaded(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        EXECUTOR.execute(() -> loadBlocking(appContext));
    }

    private static void loadBlocking(Context appContext) {
        try {
            RootieDatabase db = RootieDatabase.getDatabase(appContext);
            CommunityRepository repository = new CommunityRepository(
                    db.communityDao(),
                    new LocalJsonReader(appContext),
                    new FirestoreService()
            );
            repository.seedFromAssetsIfNeeded();
            localSeedReady = true;
            UserSocialSeeder.seedIfNeeded(appContext);

            LocalJsonReader reader = new LocalJsonReader(appContext);
            String userId = CommunitySocialHelper.resolveUserId(appContext);
            FeedDataCache.productsList = reader.getProducts();
            FeedDataCache.newsList = reader.getCommunityNews();
            FeedDataCache.mySocialData = reader.getSocialDataForUser(userId);
            Log.d(TAG, "Community local seed ready");

            repository.refreshCommunityData();
            try {
                reader.syncSocialFromFirestore(new FirestoreService());
            } catch (Exception e) {
                Log.w(TAG, "syncSocialFromFirestore skipped", e);
            }
            FeedDataCache.mySocialData = reader.getSocialDataForUser(userId);
            Log.d(TAG, "Community bootstrap complete");
        } catch (Exception e) {
            STARTED.set(false);
            localSeedReady = false;
            Log.e(TAG, "Community bootstrap failed", e);
        }
    }
}
