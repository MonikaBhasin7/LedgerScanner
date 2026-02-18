package com.example.ledgerscanner.feature.scanner.scan.ui.custom_ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val TAG = "OverlayView"
        private const val ANCHOR_SIZE = 150f
        private const val CORNER_BRACKET_LENGTH = 72f
        private const val CORNER_BRACKET_WIDTH = 5f
    }

    // Template data
    private var templateWidth = 0.0
    private var templateHeight = 0.0
    private var templateName = ""
    private var totalQuestions = 0
    private var anchorsTemplate: List<AnchorPoint> = emptyList()
    private var anchorsOnPreviewInRect: MutableList<RectF> = mutableListOf()
    private val previewRect = RectF(0f, 0f, 0f, 0f)

    // Detection state
    private var detectedAnchors: List<AnchorPoint>? = null
    private var detectedAnchorsScreenCoords: List<RectF> = emptyList()
    private var alignmentQuality: AlignmentQuality = AlignmentQuality.NONE
    private var sheetBounds: RectF? = null
    private var lightingQuality: LightingQuality = LightingQuality.GOOD
    private var isCapturing = false
    private var captureCountdown = 0
    private var safeInsetLeft = 0
    private var safeInsetTop = 0
    private var safeInsetRight = 0
    private var safeInsetBottom = 0

    // Stability tracking
    private var stabilityFrameCount = 0
    private var stabilityRequiredFrames = 7

    // Animation
    private var pulseProgress = 0f
    private val pulseSpeed = 0.05f
    private var topScrimShader: LinearGradient? = null

    // Paints - Expected Anchors (Hollow)
    private val expectedAnchorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#B3FFFFFF")
    }

    // Paints - Detected Anchors (Filled)
    private val detectedAnchorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    // Paints - Corner Brackets
    private val cornerBracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = CORNER_BRACKET_WIDTH
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E6FFFFFF")
    }

    // Paints - Frame Border
    private val frameBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#CCFFFFFF")
    }

    // Paints - Connection Lines
    private val connectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#4CAF50")
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val solidConnectionLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#4CAF50")
    }

    private val topScrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val statusCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC101418")
    }

    private val statusCardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#40FFFFFF")
    }

    private val statusCountChipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    // Paints - Status Text
    private val statusMessagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.LEFT
    }

    private val statusCountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
    }

    private val templateInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 30f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }

    // Paints - Countdown
    private val countdownTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 120f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    private val countdownCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = Color.parseColor("#4CAF50")
        strokeCap = Paint.Cap.ROUND
    }

    // Paints - Lighting Indicator
    private val lightingTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE082")
        textSize = 24f
        textAlign = Paint.Align.LEFT
    }

    private val lightingChipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC3E2F11")
    }

    private val stabilityTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#40FFFFFF")
    }

    private val stabilityFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    private val detectedAnchorGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#4CAF50")
    }

    private val detectedAnchorCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    enum class AlignmentQuality(val color: Int, val message: String) {
        NONE(Color.parseColor("#F44336"), "Searching for sheet..."),
        POOR(Color.parseColor("#FF9800"), "Move closer & center"),
        PARTIAL(Color.parseColor("#FFC107"), "Almost there..."),
        GOOD(Color.parseColor("#8BC34A"), "Hold steady"),
        STABILIZING(Color.parseColor("#8BC34A"), "Hold steady..."),
        SHEET_CURVED(Color.parseColor("#FF9800"), "Flatten the sheet"),
        PERFECT(Color.parseColor("#4CAF50"), "✓ Aligned perfectly")
    }

    enum class LightingQuality {
        GOOD, TOO_DARK, TOO_BRIGHT, GLARE
    }

    fun setTemplateSpec(template: Template) {
        templateWidth = template.sheet_width
        templateHeight = template.sheet_height
        templateName = template.name ?: ""
        totalQuestions = template.questions.size
        anchorsTemplate = template.getAnchorListClockwise()
        invalidate()
    }

    fun setPreviewRect(rect: RectF) {
        previewRect.set(rect)
        invalidate()
    }

    fun setSafeInsets(left: Int, top: Int, right: Int, bottom: Int) {
        safeInsetLeft = left
        safeInsetTop = top
        safeInsetRight = right
        safeInsetBottom = bottom
        invalidate()
    }

    fun setDetectedAnchors(anchors: List<AnchorPoint>?) {
        detectedAnchors = anchors
        updateAlignmentQuality(anchors)

        if (anchors != null && anchors.size == 4) {
            detectedAnchorsScreenCoords = anchors.mapIndexed { index, anchor ->
                convertTemplateToScreen(anchor)
            }

            // Calculate sheet bounds
            val minX = detectedAnchorsScreenCoords.minOf { it.centerX() }
            val maxX = detectedAnchorsScreenCoords.maxOf { it.centerX() }
            val minY = detectedAnchorsScreenCoords.minOf { it.centerY() }
            val maxY = detectedAnchorsScreenCoords.maxOf { it.centerY() }
            sheetBounds = RectF(minX, minY, maxX, maxY)
        } else {
            detectedAnchorsScreenCoords = emptyList()
            sheetBounds = null
        }

        invalidate()
    }

    fun setLightingQuality(quality: LightingQuality) {
        lightingQuality = quality
        invalidate()
    }

    fun startCaptureCountdown(seconds: Int) {
        isCapturing = true
        captureCountdown = seconds
        invalidate()
    }

    fun updateCountdown(remaining: Int) {
        captureCountdown = remaining
        invalidate()
    }

    fun stopCapture() {
        isCapturing = false
        captureCountdown = 0
        invalidate()
    }

    fun setStabilityProgress(stableFrames: Int, requiredFrames: Int) {
        stabilityFrameCount = stableFrames
        stabilityRequiredFrames = requiredFrames
        // Update alignment quality based on stability
        if (detectedAnchors?.size == 4) {
            alignmentQuality = if (stableFrames >= requiredFrames) {
                AlignmentQuality.PERFECT
            } else if (stableFrames > 0) {
                AlignmentQuality.STABILIZING
            } else {
                AlignmentQuality.GOOD
            }
        }
        invalidate()
    }

    fun setGeometryRejected(rejected: Boolean) {
        if (rejected && detectedAnchors?.size == 4) {
            alignmentQuality = AlignmentQuality.SHEET_CURVED
            invalidate()
        }
    }

    private fun updateAlignmentQuality(anchors: List<AnchorPoint>?) {
        alignmentQuality = when {
            anchors == null || anchors.isEmpty() -> AlignmentQuality.NONE
            anchors.size == 1 -> AlignmentQuality.POOR
            anchors.size == 2 || anchors.size == 3 -> AlignmentQuality.PARTIAL
            anchors.size == 4 -> {
                // Check if anchors are well-distributed (not clustered)
                val quality = calculateAnchorDistributionQuality(anchors)
                if (quality > 0.8) AlignmentQuality.PERFECT else AlignmentQuality.GOOD
            }
            else -> AlignmentQuality.NONE
        }
    }

    private fun calculateAnchorDistributionQuality(anchors: List<AnchorPoint>): Float {
        if (anchors.size != 4) return 0f

        // Calculate distances between consecutive corners
        val distances = mutableListOf<Float>()
        for (i in 0 until 4) {
            val a = anchors[i]
            val b = anchors[(i + 1) % 4]
            val dist = sqrt(((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y)).toFloat())
            distances.add(dist)
        }

        // Good quality if distances are relatively uniform (rectangular shape)
        val avgDist = distances.average().toFloat()
        val maxDeviation = distances.maxOf { abs(it - avgDist) }
        return 1f - (maxDeviation / avgDist).coerceIn(0f, 1f)
    }

    private fun convertTemplateToScreen(anchor: AnchorPoint): RectF {
        val scaleX = previewRect.width() / templateWidth.toFloat()
        val scaleY = previewRect.height() / templateHeight.toFloat()
        val scale = min(scaleX, scaleY)

        val displayedImgW = templateWidth.toFloat() * scale
        val displayedImgH = templateHeight.toFloat() * scale

        val offsetX = previewRect.left + (previewRect.width() - displayedImgW) / 2f
        val offsetY = previewRect.top + (previewRect.height() - displayedImgH) / 2f

        val viewX = offsetX + anchor.x.toFloat() * scale
        val viewY = offsetY + anchor.y.toFloat() * scale

        val half = 15f
        return RectF(viewX - half, viewY - half, viewX + half, viewY + half)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (previewRect.width() <= 0f || previewRect.height() <= 0f) {
            previewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        }
        topScrimShader = LinearGradient(
            0f,
            0f,
            0f,
            h * 0.35f,
            Color.parseColor("#AA000000"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (previewRect.width() <= 0f || previewRect.height() <= 0f) return
        if (templateWidth <= 0.0 || templateHeight <= 0.0) return
        if (anchorsTemplate.isEmpty()) return

        // Update pulse animation
        pulseProgress += pulseSpeed
        if (pulseProgress > 1f) pulseProgress = 0f

        // Soft top scrim for status readability
        drawTopScrim(canvas)

        // 1. Draw template info at top
//        drawTemplateInfo(canvas)

        // 2. Calculate and draw expected anchor positions
        calculateExpectedAnchorPositions()

        // 3. Draw frame border with corner brackets
        drawFrameBorderWithCorners(canvas)

        // 4. Draw expected anchor squares (hollow)
        drawExpectedAnchors(canvas)

        // 5. Draw connection lines if detected
        if (detectedAnchors != null && detectedAnchors!!.size >= 2) {
            drawConnectionLines(canvas)
        }

        // 6. Draw detected anchors (filled circles with pulse)
        if (detectedAnchors != null && detectedAnchors!!.isNotEmpty()) {
            drawDetectedAnchors(canvas)
        }

        // 7. Draw status message
        drawStatusMessage(canvas)

        // 7.5 Draw stability progress bar
        drawStabilityProgressBar(canvas)

        // 8. Draw guidance hints
        drawGuidanceHints(canvas)

        // 9. Draw lighting indicator if needed
        if (lightingQuality != LightingQuality.GOOD) {
            drawLightingIndicator(canvas)
        }

        // 10. Draw countdown if capturing
        if (isCapturing && captureCountdown > 0) {
            drawCountdown(canvas)
        }

        // BUG FIX: Only keep animating when we have detected anchors to pulse
        // Previously this ran every frame even with no anchors, wasting CPU/battery
        val hasActiveAnimation = (detectedAnchors != null && detectedAnchors!!.isNotEmpty()) ||
                (isCapturing && captureCountdown > 0) ||
                alignmentQuality == AlignmentQuality.STABILIZING
        if (hasActiveAnimation) {
            postInvalidateOnAnimation()
        }
    }

    private fun drawTopScrim(canvas: Canvas) {
        topScrimPaint.shader = topScrimShader
        canvas.drawRect(0f, 0f, width.toFloat(), height * 0.35f, topScrimPaint)
    }

    private fun calculateExpectedAnchorPositions() {
        val scaleX = previewRect.width() / templateWidth.toFloat()
        val scaleY = previewRect.height() / templateHeight.toFloat()
        val scale = min(scaleX, scaleY)

        val displayedImgW = templateWidth.toFloat() * scale
        val displayedImgH = templateHeight.toFloat() * scale

        val offsetX = previewRect.left + (previewRect.width() - displayedImgW) / 2f
        val offsetY = previewRect.top + (previewRect.height() - displayedImgH) / 2f

        anchorsOnPreviewInRect.clear()

        anchorsTemplate.forEach { a ->
            val viewX = offsetX + a.x.toFloat() * scale
            val viewY = offsetY + a.y.toFloat() * scale

            val half = ANCHOR_SIZE / 2
            val rect = RectF(viewX - half, viewY - half, viewX + half, viewY + half)
            anchorsOnPreviewInRect.add(rect)
        }
    }

    private fun drawTemplateInfo(canvas: Canvas) {
        val text = "$templateName • $totalQuestions Questions"
        canvas.drawText(text, previewRect.centerX(), previewRect.top + 60f, templateInfoPaint)
    }

    private fun drawFrameBorderWithCorners(canvas: Canvas) {
        if (anchorsOnPreviewInRect.size != 4) return

        // Calculate bounding box of expected positions
        val minX = anchorsOnPreviewInRect.minOf { it.left }
        val maxX = anchorsOnPreviewInRect.maxOf { it.right }
        val minY = anchorsOnPreviewInRect.minOf { it.top }
        val maxY = anchorsOnPreviewInRect.maxOf { it.bottom }

        val frameBounds = RectF(minX - 20, minY - 20, maxX + 20, maxY + 20)

        // Update border color based on alignment
        frameBorderPaint.color = alignmentQuality.color
        cornerBracketPaint.color = alignmentQuality.color

        // Draw corner brackets
        val bracketLength = CORNER_BRACKET_LENGTH

        // Top-left
        canvas.drawLine(frameBounds.left, frameBounds.top, frameBounds.left + bracketLength, frameBounds.top, cornerBracketPaint)
        canvas.drawLine(frameBounds.left, frameBounds.top, frameBounds.left, frameBounds.top + bracketLength, cornerBracketPaint)

        // Top-right
        canvas.drawLine(frameBounds.right, frameBounds.top, frameBounds.right - bracketLength, frameBounds.top, cornerBracketPaint)
        canvas.drawLine(frameBounds.right, frameBounds.top, frameBounds.right, frameBounds.top + bracketLength, cornerBracketPaint)

        // Bottom-right
        canvas.drawLine(frameBounds.right, frameBounds.bottom, frameBounds.right - bracketLength, frameBounds.bottom, cornerBracketPaint)
        canvas.drawLine(frameBounds.right, frameBounds.bottom, frameBounds.right, frameBounds.bottom - bracketLength, cornerBracketPaint)

        // Bottom-left
        canvas.drawLine(frameBounds.left, frameBounds.bottom, frameBounds.left + bracketLength, frameBounds.bottom, cornerBracketPaint)
        canvas.drawLine(frameBounds.left, frameBounds.bottom, frameBounds.left, frameBounds.bottom - bracketLength, cornerBracketPaint)
    }

    private fun drawExpectedAnchors(canvas: Canvas) {
        anchorsOnPreviewInRect.forEach { rect ->
            canvas.drawRoundRect(rect, 8f, 8f, expectedAnchorStrokePaint)
        }
    }

    private fun drawDetectedAnchors(canvas: Canvas) {
        detectedAnchorsScreenCoords.forEach { rect ->
            // Pulse effect
            val pulseFactor = 1f + (pulseProgress * 0.3f)
            val radius = 20f * pulseFactor

            detectedAnchorGlowPaint.alpha = (90 * (1f - pulseProgress)).toInt().coerceIn(0, 255)
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius + 10f, detectedAnchorGlowPaint)

            // Main circle
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius, detectedAnchorFillPaint)

            // Inner white dot
            canvas.drawCircle(rect.centerX(), rect.centerY(), 6f, detectedAnchorCenterPaint)
        }
    }

    private fun drawConnectionLines(canvas: Canvas) {
        if (detectedAnchorsScreenCoords.size < 2) return

        val paint = if (detectedAnchors!!.size == 4) solidConnectionLinePaint else connectionLinePaint
        paint.color = alignmentQuality.color

        for (i in 0 until detectedAnchorsScreenCoords.size) {
            val start = detectedAnchorsScreenCoords[i]
            val end = detectedAnchorsScreenCoords[(i + 1) % detectedAnchorsScreenCoords.size]
            canvas.drawLine(start.centerX(), start.centerY(), end.centerX(), end.centerY(), paint)
        }
    }

    private fun drawStatusMessage(canvas: Canvas) {
        val cardHeight = 94f
        val horizontalMargin = 20f
        val cardTop = safeInsetTop + 16f
        val cardBottom = cardTop + cardHeight
        val cardLeft = safeInsetLeft + horizontalMargin
        val cardRight = width.toFloat() - safeInsetRight - horizontalMargin
        val cardRect = RectF(cardLeft, cardTop, cardRight, cardBottom)
        val radius = 26f

        canvas.drawRoundRect(cardRect, radius, radius, statusCardPaint)
        statusCardBorderPaint.color = ColorUtils.setAlphaComponent(alignmentQuality.color, 170)
        canvas.drawRoundRect(cardRect, radius, radius, statusCardBorderPaint)

        val countChipRadius = 24f
        val chipCx = cardRight - 44f
        val chipCy = cardRect.centerY()
        statusCountChipPaint.color = ColorUtils.setAlphaComponent(alignmentQuality.color, 230)
        canvas.drawCircle(chipCx, chipCy, countChipRadius, statusCountChipPaint)
        canvas.drawText("${detectedAnchors?.size ?: 0}/4", chipCx, chipCy + 9f, statusCountPaint)

        val message = if (alignmentQuality == AlignmentQuality.STABILIZING) {
            "Hold steady ($stabilityFrameCount/$stabilityRequiredFrames)"
        } else {
            alignmentQuality.message
        }
        statusMessagePaint.color = Color.WHITE
        statusMessagePaint.textAlign = Paint.Align.CENTER
        val messageCenterX = (cardLeft + chipCx - countChipRadius - 10f) / 2f
        canvas.drawText(message, messageCenterX, chipCy + 11f, statusMessagePaint)
        statusMessagePaint.textAlign = Paint.Align.LEFT
    }

    private fun drawGuidanceHints(canvas: Canvas) {
        val hint = when (alignmentQuality) {
            AlignmentQuality.NONE -> "Move camera until all four corner boxes are visible."
            AlignmentQuality.POOR -> "Keep sheet flat and include all corners in frame."
            AlignmentQuality.PARTIAL -> "Almost done. Tilt less and center the sheet."
            AlignmentQuality.SHEET_CURVED -> "Sheet is bent. Press it flat on a surface."
            else -> null
        }
        if (hint != null) {
            val x = safeInsetLeft + 24f
            val y = height.toFloat() - safeInsetBottom - 32f
            canvas.drawText(hint, x, y, hintTextPaint)
        }
    }

    private fun drawLightingIndicator(canvas: Canvas) {
        val message = when (lightingQuality) {
            LightingQuality.TOO_DARK -> "Low light. Move to brighter area."
            LightingQuality.TOO_BRIGHT -> "Too bright. Reduce direct light."
            LightingQuality.GLARE -> "Glare found. Change angle slightly."
            else -> return
        }

        val textWidth = lightingTextPaint.measureText(message)
        val chipPadding = 18f
        val chipTop = safeInsetTop + 128f
        val chipBottom = chipTop + 46f
        val centerX = width / 2f
        val chipLeft = centerX - (textWidth / 2f) - chipPadding
        val chipRight = centerX + (textWidth / 2f) + chipPadding
        canvas.drawRoundRect(chipLeft, chipTop, chipRight, chipBottom, 20f, 20f, lightingChipPaint)
        canvas.drawText(message, chipLeft + chipPadding, chipBottom - 14f, lightingTextPaint)
    }

    private fun drawCountdown(canvas: Canvas) {
        val radius = 150f
        val centerX = previewRect.centerX()
        val centerY = previewRect.centerY()

        // Background circle
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#80000000")
        }
        canvas.drawCircle(centerX, centerY, radius + 20f, bgPaint)

        // Progress arc
        val sweepAngle = (captureCountdown / 3f) * 360f
        canvas.drawArc(
            centerX - radius, centerY - radius, centerX + radius, centerY + radius,
            -90f, sweepAngle, false, countdownCirclePaint
        )

        // Countdown number
        canvas.drawText(captureCountdown.toString(), centerX, centerY + 40f, countdownTextPaint)
    }

    private fun drawStabilityProgressBar(canvas: Canvas) {
        if (alignmentQuality != AlignmentQuality.STABILIZING) return

        val barWidth = width * 0.45f
        val barHeight = 10f
        val barX = (width / 2f) - barWidth / 2
        val barY = safeInsetTop + 126f

        canvas.drawRoundRect(barX, barY, barX + barWidth, barY + barHeight, 6f, 6f, stabilityTrackPaint)

        // Progress fill
        val progress = stabilityFrameCount.toFloat() / stabilityRequiredFrames.toFloat()
        val fillWidth = barWidth * progress.coerceIn(0f, 1f)
        canvas.drawRoundRect(barX, barY, barX + fillWidth, barY + barHeight, 6f, 6f, stabilityFillPaint)
    }

    fun getPreviewRect(): RectF = RectF(previewRect)

    fun getAnchorSquaresOnScreen(): List<RectF> = anchorsOnPreviewInRect.map { RectF(it) }
}
