package com.veganbeauty.app.utils;

import android.app.Activity;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.veganbeauty.app.R;

public final class CartFlyAnimationHelper {

    private CartFlyAnimationHelper() {
    }

    public static void flyToCart(
            @NonNull Activity activity,
            @NonNull View startView,
            @NonNull View endView,
            @Nullable ImageView productImageSource
    ) {
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }

        int[] startLoc = new int[2];
        int[] endLoc = new int[2];
        int[] rootLoc = new int[2];
        startView.getLocationOnScreen(startLoc);
        endView.getLocationOnScreen(endLoc);
        root.getLocationOnScreen(rootLoc);

        int flySize = Math.max(startView.getWidth(), dp(activity, 52));
        float startX = startLoc[0] - rootLoc[0] + (startView.getWidth() - flySize) / 2f;
        float startY = startLoc[1] - rootLoc[1] + (startView.getHeight() - flySize) / 2f;
        float endX = endLoc[0] - rootLoc[0] + (endView.getWidth() - flySize) / 2f;
        float endY = endLoc[1] - rootLoc[1] + (endView.getHeight() - flySize) / 2f;
        float controlX = (startX + endX) / 2f;
        float controlY = Math.min(startY, endY) - dp(activity, 72);

        ImageView flying = new ImageView(activity);
        flying.setScaleType(ImageView.ScaleType.CENTER_CROP);
        flying.setElevation(dp(activity, 12));

        int strokeWidth = dp(activity, 2);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(ContextCompat.getColor(activity, R.color.white));
        circleBg.setStroke(strokeWidth, ContextCompat.getColor(activity, R.color.secondary));
        flying.setBackground(circleBg);
        flying.setPadding(strokeWidth, strokeWidth, strokeWidth, strokeWidth);
        flying.setClipToOutline(true);
        flying.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        });

        Drawable drawable = productImageSource != null ? productImageSource.getDrawable() : null;
        if (drawable != null) {
            flying.setImageDrawable(drawable.mutate());
        } else {
            flying.setImageResource(R.drawable.ic_cart);
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(flySize, flySize);
        flying.setLayoutParams(params);
        flying.setX(startX);
        flying.setY(startY);
        root.addView(flying);

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(650);
        animator.addUpdateListener(valueAnimator -> {
            float t = (float) valueAnimator.getAnimatedValue();
            float oneMinusT = 1f - t;
            float x = oneMinusT * oneMinusT * startX + 2f * oneMinusT * t * controlX + t * t * endX;
            float y = oneMinusT * oneMinusT * startY + 2f * oneMinusT * t * controlY + t * t * endY;
            flying.setX(x);
            flying.setY(y);
            float scale = 1f - (0.82f * t);
            flying.setScaleX(scale);
            flying.setScaleY(scale);
            flying.setAlpha(1f - (0.25f * t));
        });
        animator.setInterpolator(new AccelerateInterpolator(0.9f));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                root.removeView(flying);
                endView.animate()
                        .scaleX(1.15f)
                        .scaleY(1.15f)
                        .setDuration(120)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(() -> endView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(120)
                                .start())
                        .start();
            }
        });
        animator.start();
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
