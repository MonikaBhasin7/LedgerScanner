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

        val cornerRadius = 4f
        anchorsOnPreviewInRect.clear()

        // compute uniform scale (preserve aspect) and center offset inside previewRect
        val scaleX = previewRect.width() / templateWidth.toFloat()
        val scaleY = previewRect.height() / templateHeight.toFloat()
        val scale = minOf(scaleX, scaleY) // IMPORTANT: uniform scale

        val displayedImgW = templateWidth.toFloat() * scale
        val displayedImgH = templateHeight.toFloat() * scale

        val offsetX = previewRect.left + (previewRect.width() - displayedImgW) / 2f
        val offsetY = previewRect.top  + (previewRect.height() - displayedImgH) / 2f

        val requestedHalf = side // desired half-side in view px

        anchorsTemplate.forEachIndexed { idx, a ->
            // map template pixel -> view pixel inside the displayed image area
            val viewX = offsetX + a.x.toFloat() * scale
            val viewY = offsetY + a.y.toFloat() * scale

            // available space around the center INSIDE previewRect
            val availLeft   = viewX - previewRect.left
            val availRight  = previewRect.right - viewX
            val availTop    = viewY - previewRect.top
            val availBottom = previewRect.bottom - viewY

            // If anchor is well inside previewRect, keep requestedHalf.
            // Only shrink if it would overflow (prevents tiny bottom squares unless the anchor truly sits near edge).
            val half = minOf(
                requestedHalf,
                availLeft.coerceAtLeast(1f),
                availRight.coerceAtLeast(1f),
                availTop.coerceAtLeast(1f),
                availBottom.coerceAtLeast(1f)
            )

            val left   = (viewX - half).coerceAtLeast(previewRect.left)
            val top    = (viewY - half).coerceAtLeast(previewRect.top)
            val right  = (viewX + half).coerceAtMost(previewRect.right)
            val bottom = (viewY + half).coerceAtMost(previewRect.bottom)

            val rect = RectF(left, top, right, bottom)
            anchorsOnPreviewInRect.add(idx, rect)

            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, pointFill)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, framePaint)
        }

        // progress arc (unchanged)
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
        postInvalidateOnAnimation()
    }

    fun getPreviewRect(): RectF = RectF(previewRect)

    /** Returns the current anchor squares as RectF in SCREEN/OVERLAY pixels.   */
    fun getAnchorSquaresOnScreen(): List<RectF> {
        return anchorsOnPreviewInRect
    }
}