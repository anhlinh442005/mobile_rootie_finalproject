package com.veganbeauty.app.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View

class CircleCropOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B328362E") // 70% dark botanical green overlay
        style = Paint.Style.FILL
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.TRANSPARENT
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D8E8C6") // primary_light border
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    init {
        // Enable software rendering type to support transparent CLEAR mode
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (Math.min(width, height) / 2f) * 0.9f
        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        canvas.drawCircle(centerX, centerY, radius, borderPaint)
    }
}
