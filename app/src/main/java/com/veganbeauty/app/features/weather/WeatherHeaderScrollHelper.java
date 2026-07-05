package com.veganbeauty.app.features.weather;

import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;

/**
 * Ẩn header khi cuộn xuống, hiện lại khi cuộn lên — đồng thời thu padding để tăng không gian nội dung.
 */
public final class WeatherHeaderScrollHelper {

    private static final int DY_THRESHOLD_PX = 8;
    private static final long ANIM_DURATION_MS = 220L;

    private final View header;
    private final NestedScrollView scrollView;
    private final int bottomPaddingPx;

    private int headerHeightPx;
    private boolean headerVisible = true;
    private ValueAnimator paddingAnimator;

    public WeatherHeaderScrollHelper(@NonNull View header,
                                     @NonNull NestedScrollView scrollView,
                                     int bottomPaddingPx) {
        this.header = header;
        this.scrollView = scrollView;
        this.bottomPaddingPx = bottomPaddingPx;
    }

    public void attach() {
        header.post(() -> {
            headerHeightPx = header.getHeight();
            if (headerHeightPx <= 0) {
                return;
            }
            applyPaddingTop(headerHeightPx);
            scrollView.setClipToPadding(false);
        });

        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
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
                });
    }

    private void hideHeader() {
        if (!headerVisible || headerHeightPx <= 0) {
            return;
        }
        headerVisible = false;
        header.animate().cancel();
        header.animate()
                .translationY(-headerHeightPx)
                .setDuration(ANIM_DURATION_MS)
                .start();
        animatePaddingTop(scrollView.getPaddingTop(), 0);
    }

    private void showHeader() {
        if (headerVisible || headerHeightPx <= 0) {
            return;
        }
        headerVisible = true;
        header.animate().cancel();
        header.animate()
                .translationY(0f)
                .setDuration(ANIM_DURATION_MS)
                .start();
        animatePaddingTop(scrollView.getPaddingTop(), headerHeightPx);
    }

    private void animatePaddingTop(int from, int to) {
        if (paddingAnimator != null) {
            paddingAnimator.cancel();
        }
        paddingAnimator = ValueAnimator.ofInt(from, to);
        paddingAnimator.setDuration(ANIM_DURATION_MS);
        paddingAnimator.addUpdateListener(animation ->
                applyPaddingTop((int) animation.getAnimatedValue()));
        paddingAnimator.start();
    }

    private void applyPaddingTop(int paddingTop) {
        scrollView.setPadding(
                scrollView.getPaddingLeft(),
                paddingTop,
                scrollView.getPaddingRight(),
                bottomPaddingPx
        );
    }
}
