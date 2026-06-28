package com.veganbeauty.app.ui.widget;

import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.veganbeauty.app.R;

public final class NavAiSkinGlowAnimator {

    private static final float ALPHA_MIN = 0.42f;
    private static final float ALPHA_MAX = 0.78f;
    private static final float SCALE_MIN = 0.86f;
    private static final float SCALE_MAX = 1.10f;

    private NavAiSkinGlowAnimator() {
    }

    public static void attach(@NonNull View glowView, @NonNull View lifecycleView) {
        detach(glowView);

        ValueAnimator pulse = ValueAnimator.ofFloat(0f, 1f);
        pulse.setDuration(1600);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new FastOutSlowInInterpolator());
        pulse.addUpdateListener(animation -> {
            float t = (float) animation.getAnimatedValue();
            float eased = easeInOutSine(t);
            float scale = SCALE_MIN + (SCALE_MAX - SCALE_MIN) * eased;
            glowView.setScaleX(scale);
            glowView.setScaleY(scale);
            glowView.setAlpha(ALPHA_MIN + (ALPHA_MAX - ALPHA_MIN) * eased);
        });
        pulse.start();
        glowView.setTag(R.id.nav_ai_skin_glow_anim, pulse);

        lifecycleView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                if (glowView.getTag(R.id.nav_ai_skin_glow_anim) == null) {
                    attach(glowView, lifecycleView);
                }
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                detach(glowView);
                lifecycleView.removeOnAttachStateChangeListener(this);
            }
        });
    }

    private static float easeInOutSine(float t) {
        return (float) (-(Math.cos(Math.PI * t) - 1) / 2);
    }

    public static void detach(@NonNull View glowView) {
        Object tag = glowView.getTag(R.id.nav_ai_skin_glow_anim);
        if (tag instanceof ValueAnimator) {
            ((ValueAnimator) tag).cancel();
        }
        glowView.setTag(R.id.nav_ai_skin_glow_anim, null);
    }
}
