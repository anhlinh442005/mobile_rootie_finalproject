package com.veganbeauty.app.utils;

import android.os.Handler;
import android.os.Looper;

import com.veganbeauty.app.data.local.entities.CommunityPostEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class CommunityPostNotifier {

    public interface Listener {
        void onPostCreated(CommunityPostEntity post);
    }

    private static final List<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CommunityPostNotifier() {
    }

    public static void addListener(Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void notifyPostCreated(CommunityPostEntity post) {
        if (post == null) return;
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                listener.onPostCreated(post);
            }
        });
    }
}
