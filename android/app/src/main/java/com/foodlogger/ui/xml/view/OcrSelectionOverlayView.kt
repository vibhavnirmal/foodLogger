package com.foodlogger.ui.xml.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.foodlogger.R

class OcrSelectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
    }

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.colorPrimary)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, android.R.color.white)
        style = Paint.Style.FILL
    }

    private var startPoint: PointF? = null
    private var endPoint: PointF? = null

    fun setStartPoint(point: PointF) {
        startPoint = point
        endPoint = null
        invalidate()
    }

    fun setEndPoint(point: PointF) {
        endPoint = point
        invalidate()
    }

    fun clearSelection() {
        startPoint = null
        endPoint = null
        invalidate()
    }

    fun hasCompleteSelection(): Boolean = startPoint != null && endPoint != null

    fun getSelectionRect(): RectF? {
        val start = startPoint ?: return null
        val end = endPoint ?: return null
        return RectF(
            minOf(start.x, end.x),
            minOf(start.y, end.y),
            maxOf(start.x, end.x),
            maxOf(start.y, end.y)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val selection = getSelectionRect()
        val start = startPoint
        val end = endPoint

        if (selection != null) {
            // Shade only outside the selected bounds so the center remains visible.
            canvas.drawRect(0f, 0f, width.toFloat(), selection.top, shadePaint)
            canvas.drawRect(0f, selection.bottom, width.toFloat(), height.toFloat(), shadePaint)
            canvas.drawRect(0f, selection.top, selection.left, selection.bottom, shadePaint)
            canvas.drawRect(selection.right, selection.top, width.toFloat(), selection.bottom, shadePaint)
            canvas.drawRect(selection, boxPaint)
        } else {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shadePaint)
        }

        start?.let { canvas.drawCircle(it.x, it.y, 10f, pointPaint) }
        end?.let { canvas.drawCircle(it.x, it.y, 10f, pointPaint) }
    }
}