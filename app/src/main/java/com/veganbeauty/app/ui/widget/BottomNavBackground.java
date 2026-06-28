package com.veganbeauty.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
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
        cornerRadius = dp(20f);
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
        float r = cornerRadius;

        path.reset();
        path.moveTo(0f, h);
        path.lineTo(0f, r);
        path.arcTo(new RectF(0f, 0f, r * 2, r * 2), 180f, 90f);
        path.lineTo(w - r, 0f);
        path.arcTo(new RectF(w - r * 2, 0f, w, r * 2), 270f, 90f);
        path.lineTo(w, h);
        path.lineTo(0f, h);
        path.close();

        canvas.drawPath(path, bgPaint);
    }

    private float dp(float value) {
        return value * getContext().getResources().getDisplayMetrics().density;
    }
}
