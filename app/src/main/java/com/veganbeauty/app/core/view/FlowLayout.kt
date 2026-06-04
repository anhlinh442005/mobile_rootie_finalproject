package com.veganbeauty.app.core.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var horizontalSpacing = 8
    private var verticalSpacing = 8

    init {
        val density = context.resources.displayMetrics.density
        horizontalSpacing = (horizontalSpacing * density).toInt()
        verticalSpacing = (verticalSpacing * density).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthLimit = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var width = 0
        var height = 0
        var lineLength = 0
        var lineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            
            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (lineLength + childWidth > widthLimit) {
                width = max(width, lineLength)
                height += lineHeight + verticalSpacing
                lineLength = childWidth
                lineHeight = childHeight
            } else {
                lineLength += childWidth + horizontalSpacing
                lineHeight = max(lineHeight, childHeight)
            }
        }
        
        width = max(width, lineLength)
        height += lineHeight
        
        val idealWidth = width + paddingLeft + paddingRight
        val idealHeight = height + paddingTop + paddingBottom
        
        setMeasuredDimension(
            resolveSize(idealWidth, widthMeasureSpec),
            resolveSize(idealHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val widthLimit = r - l - paddingLeft - paddingRight
        var curLeft = paddingLeft
        var curTop = paddingTop
        var lineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            val childWidth = child.measuredWidth
            val childHeight = child.measuredHeight

            if (curLeft + childWidth > widthLimit) {
                curLeft = paddingLeft
                curTop += lineHeight + verticalSpacing
                lineHeight = 0
            }

            child.layout(curLeft, curTop, curLeft + childWidth, curTop + childHeight)
            
            curLeft += childWidth + horizontalSpacing
            lineHeight = max(lineHeight, childHeight)
        }
    }
}
