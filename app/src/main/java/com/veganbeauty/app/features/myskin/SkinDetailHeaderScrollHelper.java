package com.veganbeauty.app.features.myskin;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Hides the app header when scrolling down; shows it again when scrolling up.
 */
public final class SkinDetailHeaderScrollHelper {

    private static final int DY_THRESHOLD_PX = 12;
    private static final long ANIM_DURATION_MS = 220L;

    private final View header;
    private final View scrollTarget;
    private final View[] companionViews;
    private final int contentPaddingTopPx;
    private int bottomPaddingPx;

    private int headerHeightPx;
    private int visibleTopInsetPx;
    private int hiddenTopInsetPx;
    private boolean headerVisible = true;
    private ValueAnimator animator;

    public SkinDetailHeaderScrollHelper(@NonNull View header,
                                        @NonNull NestedScrollView scrollView,
                                        int bottomPaddingPx) {
        this(header, scrollView, 0, bottomPaddingPx);
    }

    public SkinDetailHeaderScrollHelper(@NonNull View header,
                                        @NonNull View scrollTarget,
                                        int contentPaddingTopPx,
                                        int bottomPaddingPx,
                                        View... companionViews) {
        this.header = header;
        this.scrollTarget = scrollTarget;
        this.contentPaddingTopPx = contentPaddingTopPx;
        this.bottomPaddingPx = bottomPaddingPx;
        this.companionViews = companionViews != null ? companionViews : new View[0];
    }

    public void install() {
        header.post(() -> {
            headerHeightPx = header.getHeight();
            if (headerHeightPx <= 0) {
                return;
            }

            int companionHeightPx = 0;
            for (View companion : companionViews) {
                companionHeightPx += companion.getHeight();
            }

            visibleTopInsetPx = headerHeightPx + companionHeightPx + contentPaddingTopPx;
            hiddenTopInsetPx = companionHeightPx + contentPaddingTopPx;
            applyPaddingTop(visibleTopInsetPx);
            if (scrollTarget instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) scrollTarget).setClipToPadding(false);
            }
        });
    }

    public void setBottomPaddingPx(int bottomPaddingPx) {
        this.bottomPaddingPx = bottomPaddingPx;
        applyPaddingTop(scrollTarget.getPaddingTop());
    }

    public void attachToNestedScrollView(@NonNull NestedScrollView scrollView) {
        install();
        scrollView.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                        onScroll(scrollY, oldScrollY));
    }

    public void attachToRecyclerView(@NonNull RecyclerView recyclerView) {
        install();
        final int[] lastScrollY = {0};
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                int scrollY = rv.computeVerticalScrollOffset();
                onScroll(scrollY, lastScrollY[0]);
                lastScrollY[0] = scrollY;
            }
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
        animateHeader(false);
    }

    private void showHeader() {
        animateHeader(true);
    }

    private void animateHeader(boolean show) {
        if (show == headerVisible || headerHeightPx <= 0) {
            return;
        }
        headerVisible = show;

        if (animator != null) {
            animator.cancel();
        }

        float startTranslation = header.getTranslationY();
        float endTranslation = show ? 0f : -headerHeightPx;
        int startPadding = scrollTarget.getPaddingTop();
        int endPadding = show ? visibleTopInsetPx : hiddenTopInsetPx;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(ANIM_DURATION_MS);
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            float translation = startTranslation + (endTranslation - startTranslation) * fraction;
            header.setTranslationY(translation);
            for (View companion : companionViews) {
                companion.setTranslationY(translation);
            }
            int paddingTop = (int) (startPadding + (endPadding - startPadding) * fraction);
            applyPaddingTop(paddingTop);
        });
        animator.start();
    }

    private void applyPaddingTop(int paddingTop) {
        scrollTarget.setPadding(
                scrollTarget.getPaddingLeft(),
                paddingTop,
                scrollTarget.getPaddingRight(),
                bottomPaddingPx
        );
    }
}
