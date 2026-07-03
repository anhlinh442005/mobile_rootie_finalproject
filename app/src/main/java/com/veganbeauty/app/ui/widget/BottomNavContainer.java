package com.veganbeauty.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Root wrapper for the custom bottom nav. Applies system navigation-bar insets
 * so the bar sits above the gesture area on all devices.
 */
public class BottomNavContainer extends FrameLayout {

    public BottomNavContainer(@NonNull Context context) {
        super(context);
        init();
    }

    public BottomNavContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BottomNavContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(this, (view, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(0, 0, 0, navBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewCompat.requestApplyInsets(this);
    }
}
