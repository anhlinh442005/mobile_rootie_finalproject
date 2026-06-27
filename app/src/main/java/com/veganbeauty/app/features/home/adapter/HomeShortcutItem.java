package com.veganbeauty.app.features.home.adapter;

import androidx.annotation.NonNull;

public class HomeShortcutItem {
    @NonNull private String title;
    private int iconResId;
    @NonNull private Runnable action;

    public HomeShortcutItem(@NonNull String title, int iconResId, @NonNull Runnable action) {
        this.title = title;
        this.iconResId = iconResId;
        this.action = action;
    }

    @NonNull public String getTitle() { return title; }
    public int getIconResId() { return iconResId; }
    @NonNull public Runnable getAction() { return action; }
}
