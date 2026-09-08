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

    // Percentage from 0 to 100. Change this and it redraws automatically.
    var percentage: Int = 0
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3D6591")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C7B8FA")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val rectF = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = minOf(width, height).toFloat()
        val strokeWidth = size * 0.1f

        backgroundPaint.strokeWidth = strokeWidth
        progressPaint.strokeWidth = strokeWidth

        val padding = strokeWidth / 2f + 2f
        rectF.set(padding, padding, size - padding, size - padding)

        // Full faint ring first (the "track")
        canvas.drawArc(rectF, 0f, 360f, false, backgroundPaint)

        // Progress ring on top, starting from the top, sweeping clockwise
        val sweepAngle = 360f * (percentage / 100f)
        if (sweepAngle > 0f) {
            canvas.drawArc(rectF, -90f, sweepAngle, false, progressPaint)
        }
    }
}