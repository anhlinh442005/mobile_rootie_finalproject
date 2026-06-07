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
    private val shadowRadius  = dp(12f)
    private val shadowDy      = dp(-2f) 
    private val shadowColor   = Color.parseColor("#10000000") // Very soft shadow

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
        val bodyHeight = dp(64f) 
        val p = h - bodyHeight 
        val r = cornerRadius
        val cx = w / 2f

        val bumpWidth = dp(76f)
        val bumpHeight = dp(16f)

        path.reset()
        // Top-left corner
        path.moveTo(0f, p + r)
        path.arcTo(RectF(0f, p, r * 2, p + r * 2), 180f, 90f)

        // Line to the start of the bump
        path.lineTo(cx - bumpWidth / 2f, p)

        // Smooth Bezier wave
        path.cubicTo(
            cx - bumpWidth / 3f, p,
            cx - bumpWidth / 4f, p - bumpHeight,
            cx, p - bumpHeight
        )
        path.cubicTo(
            cx + bumpWidth / 4f, p - bumpHeight,
            cx + bumpWidth / 3f, p,
            cx + bumpWidth / 2f, p
        )

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
