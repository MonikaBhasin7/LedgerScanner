package com.example.ledgerscanner.feature.scanner.scan.ui


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withSave

data class AnchorTemplateSpace(val x: Double, val y: Double)

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    // template image size used when coordinates were measured
    private var templateWidth  = 0.0
    private var templateHeight = 0.0
    private var anchorsTemplate: List<AnchorTemplateSpace> = emptyList()

    // where the camera buffer is drawn inside this View; defaults to full view
    private val previewRect = RectF(0f, 0f, 0f, 0f)

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.GREEN
    }
    private val pointFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    /** call once you know your template’s pixel size and its four anchors (in that space) */
    fun setTemplateSpec(sheetWidthPx: Int, sheetHeightPx: Int, anchors: List<AnchorTemplateSpace>) {
        templateWidth = sheetWidthPx.toDouble()
        templateHeight = sheetHeightPx.toDouble()
        anchorsTemplate = anchors
        invalidate()
    }

    /** optional: if you calculate the exact letterboxed preview area, pass it here */
    fun setPreviewRect(rect: RectF) {
        previewRect.set(rect)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // sensible default: draw over the whole view if caller didn’t provide a rect
        if (previewRect.width() <= 0f || previewRect.height() <= 0f) {
            previewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (previewRect.width() <= 0f || previewRect.height() <= 0f) return
        if (templateWidth <= 0.0 || templateHeight <= 0.0) return
        if (anchorsTemplate.isEmpty()) return

        // debug frame
        canvas.drawRect(previewRect, framePaint)

        // square size (screen px). tweak as you like or expose as a setter.
        val side = 60f
        val half = side / 1f
        val cornerRadius = 4f  // set 0f for sharp corners

        anchorsTemplate.forEachIndexed { idx, a ->
            val sx = previewRect.left + (a.x / templateWidth)  * previewRect.width()
            val sy = previewRect.top  + (a.y / templateHeight) * previewRect.height()

            val left   = sx.toFloat() - half
            val top    = sy.toFloat() - half
            val right  = sx.toFloat() + half
            val bottom = sy.toFloat() + half
            val rect = RectF(left, top, right, bottom)

            // filled square
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, pointFill)
            // outline
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, framePaint)
            // label
            canvas.drawText("#$idx", right + 8f, top - 8f, labelPaint)
        }
    }
}