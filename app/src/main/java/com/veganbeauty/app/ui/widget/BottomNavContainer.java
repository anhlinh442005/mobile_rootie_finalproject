package com.veganbeauty.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.veganbeauty.app.R;

/**
 * Root wrapper for the custom bottom nav. The 104dp bar (notch, FAB, tabs) stays
 * unchanged; a white fill below extends to the physical screen bottom.
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
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            int bottomInset = navBars.bottom;
            if (bottomInset == 0) {
                bottomInset = insets.getInsetsIgnoringVisibility(
                        WindowInsetsCompat.Type.navigationBars()).bottom;
            }
            View insetFill = findViewById(R.id.nav_bottom_inset_fill);
            if (insetFill != null) {
                ViewGroup.LayoutParams lp = insetFill.getLayoutParams();
                lp.height = bottomInset;
                insetFill.setLayoutParams(lp);
            }
            return insets;
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewCompat.requestApplyInsets(this);
    }
}
