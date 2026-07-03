package com.veganbeauty.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.veganbeauty.app.R;

/**
 * Viettel / MoMo bottom bar notch — SVG-tangent cubic Bézier (G1 at wing + valley).
 * <pre>
 * H wingL  C  wingL+cp1, top | center-cp2, valley | center, valley
 *          C  center+cp2, valley | wingR-cp1, top | wingR, top
 * </pre>
 * Reference SVG (400×70): M0 0 H140 C165 0,160 45,200 45 C240 45,235 0,260 0 …
 */
public class BottomNavBackground extends View {

    private static final float HOLE_HALF_WIDTH_FACTOR = 1.28f;
    /** SVG: cp1 spread 25 / wing half 60 */
    private static final float SVG_CP1_WING_RATIO = 25f / 60f;
    /** SVG: cp2 inset 40 / valley depth 45 */
    private static final float SVG_CP2_DEPTH_RATIO = 40f / 45f;

    private int bgColor;
    private float topCornerRadius;
    private float fabSize;
    private float fabBottomMargin;
    private float notchGap;
    private float iconVisualRadius;
    private float barBodyHeight;
    private float shadowRadius;
    private float shadowDy;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path drawPath = new Path();

    private float barTop;

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
        setBackgroundColor(Color.TRANSPARENT);

        bgColor = context.getColor(R.color.white);
        topCornerRadius = getResources().getDimension(R.dimen.home_nav_top_corner_radius);
        fabSize = getResources().getDimension(R.dimen.home_nav_fab_size);
        fabBottomMargin = getResources().getDimension(R.dimen.home_nav_fab_bottom_margin);
        notchGap = getResources().getDimension(R.dimen.home_nav_notch_gap);
        iconVisualRadius = getResources().getDimension(R.dimen.home_nav_fab_icon_size) * 0.5f;
        barBodyHeight = getResources().getDimension(R.dimen.home_nav_bar_height);
        shadowRadius = getResources().getDimension(R.dimen.home_nav_bar_shadow);
        shadowDy = getResources().getDimension(R.dimen.home_nav_bar_shadow_dy);

        fillPaint.setColor(bgColor);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setShadowLayer(shadowRadius, 0f, -shadowDy, Color.parseColor("#1A000000"));

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        barTop = Math.max(0f, h - barBodyHeight);
    }

    private void rebuildPath(float width, float height) {
        float centerX = width * 0.5f;
        float top = barTop;
        float fabRadius = fabSize * 0.5f;
        float iconCenterY = height - fabBottomMargin - fabRadius;
        float clearanceR = iconVisualRadius + notchGap;

        float dy = top - iconCenterY;
        float dxSq = clearanceR * clearanceR - dy * dy;
        if (dxSq <= 0f) {
            drawRectFallback(width, height, top);
            return;
        }

        float intersectionHalf = (float) Math.sqrt(dxSq);
        float wingHalf = Math.max(intersectionHalf, clearanceR) * HOLE_HALF_WIDTH_FACTOR;
        float valleyY = iconCenterY + clearanceR;

        float leftWingX = centerX - wingHalf;
        float rightWingX = centerX + wingHalf;
        float cp1Spread = wingHalf * SVG_CP1_WING_RATIO;
        float cp2Inset = clearanceR * SVG_CP2_DEPTH_RATIO;

        drawPath.reset();
        drawPath.moveTo(0f, height);
        drawPath.lineTo(0f, top + topCornerRadius);
        drawPath.quadTo(0f, top, topCornerRadius, top);

        drawPath.lineTo(leftWingX, top);
        // A → B: CP1 on top (horizontal exit), CP2 on valley row (horizontal entry into B)
        drawPath.cubicTo(
                leftWingX + cp1Spread, top,
                centerX - cp2Inset, valleyY,
                centerX, valleyY
        );
        // B → C: mirror
        drawPath.cubicTo(
                centerX + cp2Inset, valleyY,
                rightWingX - cp1Spread, top,
                rightWingX, top
        );

        drawPath.lineTo(width - topCornerRadius, top);
        drawPath.quadTo(width, top, width, top + topCornerRadius);
        drawPath.lineTo(width, height);
        drawPath.close();
    }

    private void drawRectFallback(float width, float height, float top) {
        drawPath.reset();
        drawPath.moveTo(0f, height);
        drawPath.lineTo(0f, top + topCornerRadius);
        drawPath.quadTo(0f, top, topCornerRadius, top);
        drawPath.lineTo(width - topCornerRadius, top);
        drawPath.quadTo(width, top, width, top + topCornerRadius);
        drawPath.lineTo(width, height);
        drawPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (width <= 0f || height <= 0f) {
            return;
        }

        rebuildPath(width, height);
        canvas.drawPath(drawPath, fillPaint);
    }
}
