package com.veganbeauty.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
importവിടandroid.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.veganbeauty.app.R;

public class BottomNavBackground extends View {

    private int bgColor;
    private float cornerRadius;
    private float shadowRadius;
    private float shadowDy;
    private int shadowColor;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public BottomNavBackground(Context context) {
        super(context);
        init(context);
    }

    public BottomNavBackground(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BottomNavBackground(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        bgColor = context.getColor(R.color.neutral);
        cornerRadius = dp(24f);
        shadowRadius = dp(12f);
        shadowDy = dp(-2f);
        shadowColor = Color.parseColor("#10000000");

        bgPaint.setColor(bgColor);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float bodyHeight = dp(64f);
        float p = h - bodyHeight;
        float r = cornerRadius;
        float cx = w / 2f;

        float bumpWidth = dp(76f);
        float bumpHeight = dp(16f);

        path.reset();
        path.moveTo(0f, p + r);
        path.arcTo(new RectF(0f, p, r * 2, p + r * 2), 180f, 90f);

        path.lineTo(cx - bumpWidth / 2f, p);

        path.cubicTo(
            cx - bumpWidth / 3f, p,
            cx - bumpWidth / 4f, p - bumpHeight,
            cx, p - bumpHeight
        );
        path.cubicTo(
            cx + bumpWidth / 4f, p - bumpHeight,
            cx + bumpWidth / 3f, p,
            cx + bumpWidth / 2f, p
        );

        path.lineTo(w - r, p);
        path.arcTo(new RectF(w - r * 2, p, w, p + r * 2), 270f, 90f);

        path.lineTo(w, h);
        path.lineTo(0f, h);
        path.lineTo(0f, p + r);

        path.close();
        canvas.drawPath(path, bgPaint);
    }

    private float dp(float value) {
        return value * getContext().getResources().getDisplayMetrics().density;
    }
}
