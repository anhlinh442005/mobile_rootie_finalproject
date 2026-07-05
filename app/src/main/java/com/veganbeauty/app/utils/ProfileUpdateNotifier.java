package com.veganbeauty.app.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProfileUpdateNotifier {

    public interface Listener {
        void onProfileUpdated();
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ProfileUpdateNotifier() {
    }

    public static void addListener(Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyUpdated() {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                listener.onProfileUpdated();
            }
        });
    }
}
