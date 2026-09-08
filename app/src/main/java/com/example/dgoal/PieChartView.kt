package com.example.dgoal

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Percentage from 0 to 100. Change this and call invalidate() to redraw.
    var percentage: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E5E7EB") // light gray
        style = Paint.Style.FILL
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6C4CE0") // purple, matches app theme
        style = Paint.Style.FILL
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val padding = 4f

        rectF.set(padding, padding, size - padding, size - padding)

        // Draw the full gray circle first (the "empty" background)
        canvas.drawArc(rectF, 0f, 360f, true, backgroundPaint)

        // Draw the purple slice on top, starting from the top (-90 degrees)
        val sweepAngle = 360f * (percentage / 100f)
        canvas.drawArc(rectF, -90f, sweepAngle, true, progressPaint)
    }
}