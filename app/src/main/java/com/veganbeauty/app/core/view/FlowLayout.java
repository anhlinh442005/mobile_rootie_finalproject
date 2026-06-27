package com.veganbeauty.app.core.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public class FlowLayout extends ViewGroup {

    private int horizontalSpacing = 8;
    private int verticalSpacing = 8;

    public FlowLayout(Context context) {
        super(context);
        init(context);
    }

    public FlowLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FlowLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        horizontalSpacing = (int) (horizontalSpacing * density);
        verticalSpacing = (int) (verticalSpacing * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthLimit = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int width = 0;
        int height = 0;
        int lineLength = 0;
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;

            measureChild(child, widthMeasureSpec, heightMeasureSpec);

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (lineLength + childWidth > widthLimit) {
                width = Math.max(width, lineLength);
                height += lineHeight + verticalSpacing;
                lineLength = childWidth;
                lineHeight = childHeight;
            } else {
                lineLength += childWidth + horizontalSpacing;
                lineHeight = Math.max(lineHeight, childHeight);
            }
        }

        width = Math.max(width, lineLength);
        height += lineHeight;

        int idealWidth = width + getPaddingLeft() + getPaddingRight();
        int idealHeight = height + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(
                resolveSize(idealWidth, widthMeasureSpec),
                resolveSize(idealHeight, heightMeasureSpec)
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int widthLimit = r - l - getPaddingLeft() - getPaddingRight();
        int curLeft = getPaddingLeft();
        int curTop = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            if (curLeft + childWidth > widthLimit) {
                curLeft = getPaddingLeft();
                curTop += lineHeight + verticalSpacing;
                lineHeight = 0;
            }

            child.layout(curLeft, curTop, curLeft + childWidth, curTop + childHeight);

            curLeft += childWidth + horizontalSpacing;
            lineHeight = Math.max(lineHeight, childHeight);
        }
    }
}
