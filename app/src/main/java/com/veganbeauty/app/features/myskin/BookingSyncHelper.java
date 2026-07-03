package com.veganbeauty.app.features.myskin;

import android.content.Context;

public final class BookingSyncHelper {

    private BookingSyncHelper() {
    }

    public interface Callback {
        void onComplete();
    }

    public static void syncUserBookings(Context context, String userEmail, Callback callback) {
        AccountSyncHelper.sync(context, callback != null ? callback::onComplete : null);
    }
}
