package com.veganbeauty.app.features.home;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

/**
 * Hides the home header when scrolling down; shows it again when scrolling up.
 */
public final class HomeHeaderScrollHelper {

    private static final int DY_THRESHOLD_PX = 12;
    private static final long ANIM_DURATION_MS = 220L;

    private final View header;
    private final NestedScrollView scrollView;
    private final int bottomPaddingPx;

    private int headerHeightPx;
    private boolean headerVisible = true;

    public HomeHeaderScrollHelper(@NonNull View header,
                                  @NonNull NestedScrollView scrollView,
                                  int bottomPaddingPx) {
        this.header = header;
        this.scrollView = scrollView;
        this.bottomPaddingPx = bottomPaddingPx;
    }

    public void install() {
        header.post(() -> {
            headerHeightPx = header.getHeight();
            // ScrollView already sits below the header in layout — only reserve bottom inset.
            scrollView.setPadding(0, 0, 0, bottomPaddingPx);
            scrollView.setClipToPadding(false);
        });
    }

    public void onScroll(int scrollY, int oldScrollY) {
        if (headerHeightPx <= 0) {
            return;
        }
        if (scrollY <= 0) {
            showHeader();
            return;
        }

        int dy = scrollY - oldScrollY;
        if (dy > DY_THRESHOLD_PX && headerVisible) {
            hideHeader();
        } else if (dy < -DY_THRESHOLD_PX && !headerVisible) {
            showHeader();
        }
    }

    private void hideHeader() {
        if (!headerVisible) {
            return;
        }
        headerVisible = false;
        header.animate().cancel();
        header.animate()
                .translationY(-headerHeightPx)
                .setDuration(ANIM_DURATION_MS)
                .start();
    }

    private void showHeader() {
        if (headerVisible) {
            return;
        }
        headerVisible = true;
        header.animate().cancel();
        header.animate()
                .translationY(0f)
                .setDuration(ANIM_DURATION_MS)
                .start();
    }
}
