package com.veganbeauty.app.features.account.expiry

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class AccountProductExpiryDetailCircularProgress @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Progress from 0.0 to 1.0
    var progress: Float = 0.0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E4E2") // Gray background ring
        style = Paint.Style.STROKE
        strokeWidth = dp(6f)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3E4D44") // Primary dark green ring
        style = Paint.Style.STROKE
        strokeWidth = dp(6f)
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val size = minOf(w, h)
        val stroke = dp(6f)
        val radius = (size - stroke) / 2f

        val cx = w / 2f
        val cy = h / 2f

        // Draw background circle
        canvas.drawCircle(cx, cy, radius, backgroundPaint)

        // Draw progress arc (start from top -90 degrees)
        val rectF = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rectF, -90f, progress * 360f, false, progressPaint)
    }

    private fun dp(value: Float): Float {
        return value * context.resources.displayMetrics.density
    }
}
