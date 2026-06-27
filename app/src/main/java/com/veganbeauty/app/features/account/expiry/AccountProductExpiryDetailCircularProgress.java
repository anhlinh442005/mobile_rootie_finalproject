package com.veganbeauty.app.features.account.expiry;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class AccountProductExpiryDetailCircularProgress extends View {

    private float progress = 0.0f;
    private int progressColor = Color.parseColor("#3E4D44");

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AccountProductExpiryDetailCircularProgress(Context context) {
        super(context);
        init();
    }

    public AccountProductExpiryDetailCircularProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AccountProductExpiryDetailCircularProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        backgroundPaint.setColor(Color.parseColor("#E0E4E2"));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(dp(6f));

        progressPaint.setColor(progressColor);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dp(6f));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float value) {
        this.progress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setProgressColor(int color) {
        this.progressColor = color;
        progressPaint.setColor(color);
        invalidate();
    }

    public int getProgressColor() {
        return progressColor;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float stroke = dp(6f);
        float radius = (size - stroke) / 2f;

        float cx = w / 2f;
        float cy = h / 2f;

        canvas.drawCircle(cx, cy, radius, backgroundPaint);

        RectF rectF = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(rectF, -90f, progress * 360f, false, progressPaint);
    }

    private float dp(float value) {
        return value * getContext().getResources().getDisplayMetrics().density;
    }
}
