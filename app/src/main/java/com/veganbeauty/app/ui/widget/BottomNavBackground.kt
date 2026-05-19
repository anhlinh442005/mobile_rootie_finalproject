package com.veganbeauty.app.ui.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BottomNavBackground @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgColor       = context.getColor(com.veganbeauty.app.R.color.neutral) // Fetching from colors.xml
    private val cornerRadius  = dp(24f)   // Top corners radius
    private val bumpRadius    = dp(28f)   // Radius of the circle bump
    private val shadowRadius  = dp(12f)
    private val shadowDy      = dp(-2f) 
    private val shadowColor   = Color.parseColor("#1A000000") // Soft shadow

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
        setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
    }

    private val path = Path()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        // The bottom nav layout height is 72dp, we assume view height is 100dp.
        // So the flat top of the rectangle starts at height - 72dp.
        val bodyHeight = dp(72f)
        val p = h - bodyHeight 
        val r = cornerRadius
        val br = bumpRadius
        val cx = w / 2f

        path.reset()
        // Top-left corner
        path.moveTo(0f, p + r)
        path.arcTo(RectF(0f, p, r * 2, p + r * 2), 180f, 90f)

        // Line to the start of the bump
        path.lineTo(cx - br, p)

        // The bump (perfect semi-circle upwards)
        val bumpOval = RectF(cx - br, p - br, cx + br, p + br)
        path.arcTo(bumpOval, 180f, 180f)

        // Line to top-right corner
        path.lineTo(w - r, p)

        // Top-right corner
        path.arcTo(RectF(w - r * 2, p, w, p + r * 2), 270f, 90f)

        // Right line down to bottom
        path.lineTo(w, h)
        // Bottom line to left
        path.lineTo(0f, h)
        // Close path back to top-left
        path.lineTo(0f, p + r)

        path.close()
        canvas.drawPath(path, bgPaint)
    }

    private fun dp(value: Float) = value * context.resources.displayMetrics.density
}
