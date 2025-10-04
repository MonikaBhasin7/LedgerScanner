package com.example.ledgerscanner.feature.scanner.scan.ui


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val TAG = "OverlayView"
    }

    var templateProcessor = TemplateProcessor()
    var omrProcessor = OmrProcessor()

    val side = 60f

    // template image size used when coordinates were measured
    private var templateWidth = 0.0
    private var templateHeight = 0.0
    private var anchorsTemplate: List<AnchorPoint> = emptyList()
    private var anchorsOnPreviewInRect: MutableList<RectF> = mutableListOf()

    // where the camera buffer is drawn inside this View; defaults to full view
    private val previewRect = RectF(0f, 0f, 0f, 0f)

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }

    private val progressIndicatorPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
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
    fun setTemplateSpec(template: Template) {
        templateWidth = template.sheet_width
        templateHeight = template.sheet_height
        anchorsTemplate = template.getAnchorListClockwise()
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

        val cornerRadius = 4f  // set 0f for sharp corners
        anchorsOnPreviewInRect.clear()

        val halfPreviewWidth = previewRect.width() / 2
        val halfPreviewHeight = previewRect.height() / 2
        val templateAnchorWidth = (anchorsTemplate[1].x - anchorsTemplate[0].x) / 2
        val templateAnchorHeight = (anchorsTemplate[3].y - anchorsTemplate[0].y) / 2

        anchorsTemplate.forEachIndexed { idx, a ->
            var left: Float = if (a.x < halfPreviewWidth) {
                (halfPreviewWidth - templateAnchorWidth).toFloat()
            } else {
                (halfPreviewWidth + templateAnchorWidth).toFloat()
            }
            left = left - side
            val right = left + 2 * side

            var top: Float = if (a.y < halfPreviewHeight) {
                (halfPreviewHeight - templateAnchorHeight).toFloat()
            } else {
                (halfPreviewHeight + templateAnchorHeight).toFloat()
            }
            top = top - side
            val bottom = top + 2 * side
            val rect = RectF(left, top, right, bottom)
            anchorsOnPreviewInRect.add(idx, rect)
            canvas.apply {
                drawRoundRect(rect, cornerRadius, cornerRadius, pointFill)
                drawRoundRect(rect, cornerRadius, cornerRadius, framePaint)
//                drawText("#$idx", right + 8f, top - 8f, labelPaint)
            }
        }

        val bounding = RectF()
        anchorsOnPreviewInRect.forEach { rect -> bounding.union(rect) }
        val centerX = bounding.centerX()
        val centerY = bounding.centerY()
        val sweepAngle = (System.currentTimeMillis() / 2) % 360
        canvas.drawArc(
            centerX - 40f, centerY - 40f, centerX + 40f, centerY + 40f,
            0f, sweepAngle.toFloat(),
            false, progressIndicatorPaint
        )
        // Invalidate again for animation
        postInvalidateOnAnimation()
    }

    fun getPreviewRect(): RectF = RectF(previewRect)

    /** Returns the current anchor squares as RectF in SCREEN/OVERLAY pixels.   */
    fun getAnchorSquaresOnScreen(): List<RectF> {
        return anchorsOnPreviewInRect
    }
}