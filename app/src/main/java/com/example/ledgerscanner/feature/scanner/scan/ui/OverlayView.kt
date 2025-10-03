package com.example.ledgerscanner.feature.scanner.scan.ui


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.ImageProxy
import com.example.ledgerscanner.base.utils.OmrUtils
import com.example.ledgerscanner.base.utils.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

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
    }

    fun getPreviewRect(): RectF = RectF(previewRect)

    /** Returns the current anchor squares as RectF in SCREEN/OVERLAY pixels.   */
    fun getAnchorSquaresOnScreen(): List<RectF> {
        return anchorsOnPreviewInRect
    }

    fun screenRectToMatRect(
        screenRect: RectF,
        previewRect: RectF,
        mat: Mat
    ): Rect {
        // 1) Normalize the overlay rect into the previewRect (0..1 in both axes)
        val u0 = ((screenRect.left - previewRect.left) / previewRect.width()).coerceIn(0f, 1f)
        val v0 = ((screenRect.top - previewRect.top) / previewRect.height()).coerceIn(0f, 1f)
        val u1 = ((screenRect.right - previewRect.left) / previewRect.width()).coerceIn(0f, 1f)
        val v1 = ((screenRect.bottom - previewRect.top) / previewRect.height()).coerceIn(0f, 1f)

        // 2) Map normalized coords to Mat pixels (Mat is UPRIGHT)
        val w = mat.cols()
        val h = mat.rows()

        val x0 = (u0 * w).toInt()
        val y0 = (v0 * h).toInt()
        val x1 = (u1 * w).toInt()
        val y1 = (v1 * h).toInt()

        // 3) Clamp & build OpenCV Rect (note: OpenCV Rect width/height are inclusive-exclusive)
        val left = minOf(x0, x1).coerceIn(0, w - 1)
        val top = minOf(y0, y1).coerceIn(0, h - 1)
        val right = maxOf(x0, x1).coerceIn(left + 1, w)
        val bottom = maxOf(y0, y1).coerceIn(top + 1, h)

        return Rect(left, top, right - left, bottom - top)
    }

    private fun detectAnchorInRoi(roiGray: Mat): Point? {
        val bin = Mat()
        // simple threshold (anchors are dark); tweak 50..90 as needed
        Imgproc.threshold(
            roiGray,
            bin, 60.0,
            255.0,
            Imgproc.THRESH_BINARY_INV
        )

        val contours = mutableListOf<org.opencv.core.MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            bin,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        for (c in contours) {
            val peri = Imgproc.arcLength(
                MatOfPoint2f(*c.toArray()),
                true
            )
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(
                MatOfPoint2f(*c.toArray()),
                approx,
                0.04 * peri,
                true
            )

//            if (approx.total() == 4L) {
//                val rect = Imgproc.boundingRect(MatOfPoint(*approx.toArray()))
//                val aspect = rect.width.toDouble() / rect.height.toDouble()
//                val contourArea = Imgproc.contourArea(c)
//                val rectArea = rect.width * rect.height.toDouble()
//                val solidity = if (rectArea > 0) contourArea / rectArea else 0.0
//
//                //&& solidity > 0.80 && rectArea > 200 //todo monika sort this out first thing in morning
//                // square-ish and filled enough and big enough inside this ROI
//                if (aspect in 0.9..1.1 && solidity > 0.80 && rectArea > 200) {
//                    val cx = rect.x + rect.width / 2.0
//                    val cy = rect.y + rect.height / 2.0
//                    bin.release(); hierarchy.release(); approx.release()
//                    return Point(cx, cy)
//                }
//            }

            val roiW = roiGray.cols()
            val roiH = roiGray.rows()
            val roiArea = (roiW * roiH).toDouble()
            val minArea = 0.002 * roiArea   // 0.2% of ROI
            val maxArea = 0.25 * roiArea   // 25% of ROI
            if (approx.total() == 4L) {
                val approxMP = MatOfPoint(*approx.toArray())
                // must look like a convex quad
                if (!Imgproc.isContourConvex(approxMP)) {
                    approxMP.release()
                    // keep your existing 'continue' here
                } else {
                    val rect = Imgproc.boundingRect(approxMP)
                    val aspect = rect.width.toDouble() / rect.height.toDouble()

                    val area = Imgproc.contourArea(approxMP)
                    val rectArea = (rect.width * rect.height).toDouble().coerceAtLeast(1.0)
                    val solidity = area / rectArea

                    // reject circles by circularity (circles ≈ 1.0; squares clearly lower)
                    val peri2 = Imgproc.arcLength(MatOfPoint2f(*approxMP.toArray()), true)
                    val circularity =
                        if (peri2 > 1e-6) 4.0 * Math.PI * area / (peri2 * peri2) else 0.0

                    // Scale-robust checks:
                    // - near-square aspect
                    // - filled enough
                    // - area within [minArea, maxArea] fraction of ROI
                    // - not a circle
                    if (aspect in 0.8..1.25 && solidity > 0.70 && area in minArea..maxArea && circularity < 0.85) {
                        val cx = rect.x + rect.width / 2.0
                        val cy = rect.y + rect.height / 2.0

                        bin.release()
                        hierarchy.release()
                        approxMP.release()
                        approx.release()
                        return Point(cx, cy)
                    }
                    approxMP.release()
                }
                // keep your existing approx.release() if you didn't above
            }
        }
        bin.release(); hierarchy.release()
        return null
    }

    private fun imageProxyToGrayMatUpright(image: ImageProxy): Mat {
        val yPlane = image.planes[0]
        val w = image.width
        val h = image.height
        val rowStride = yPlane.rowStride
        val buffer = yPlane.buffer

        val mat = Mat(h, w, CvType.CV_8UC1)
        if (rowStride == w) {
            val arr = ByteArray(buffer.remaining())
            buffer.get(arr)
            mat.put(0, 0, arr)
        } else {
            val row = ByteArray(rowStride)
            for (r in 0 until h) {
                buffer.position(r * rowStride)
                buffer.get(row, 0, rowStride)
                mat.put(r, 0, row, 0, w)
            }
        }

        // Rotate based on imageInfo.rotationDegrees
        val rotated = Mat()
        when (image.imageInfo.rotationDegrees) {
            90 -> Core.rotate(mat, rotated, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(mat, rotated, Core.ROTATE_180)
            270 -> Core.rotate(mat, rotated, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> mat.copyTo(rotated)
        }
        mat.release()
        return rotated
    }

    fun detectAnchorsInsideOverlay(
        image: ImageProxy,
        omrTemplate: Template,
        debug: Boolean = false
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> {
        val debugBitmaps = hashMapOf<String, Bitmap>()
        var finalBitmap: Bitmap? = null

        try {
            // 1) Get overlay squares in SCREEN pixels and the preview rectangle we drew against
            val overlayRects = getAnchorSquaresOnScreen() // same sidePx as your overlay
            val previewRect = getPreviewRect()
            if (overlayRects.isEmpty() || previewRect.width() <= 0f) {
                return OmrImageProcessResult(false) to emptyList()
            }

            // 2) Convert camera buffer to a gray Mat (once per frame)
            val gray = imageProxyToGrayMatUpright(image)
            if (debug) debugBitmaps["image proxy gray mat"] = gray.toBitmapSafe()

            val centersInBuffer = ArrayList<AnchorPoint>(overlayRects.size)
            var allFound = true

            // 3) For each overlay square: map to buffer → crop → detect black square
            for ((index, screenRect) in overlayRects.withIndex()) {
                // 1) Map screen overlay square → Mat rect
                val matRect = screenRectToMatRect(screenRect, previewRect, gray)

                // 2) Crop ROI directly with that rect
                val roi = Mat(gray, matRect)
                if (debug) debugBitmaps["roi - $index"] = roi.toBitmapSafe()

                // 3) Detect anchor inside ROI
//                val anchor = TemplateProcessor().detectAnchorPointsImpl(roi).getOrNull(0)
                val anchor = detectAnchorInRoi(roi)
                roi.release()

                if (anchor == null) {
                    allFound = false
                } else {
                    // 4) Convert ROI-local center to full Mat coordinates
                    centersInBuffer += AnchorPoint(
                        (matRect.x + anchor.x.toFloat()).toDouble(),
                        (matRect.y + anchor.y.toFloat()).toDouble()
                    )
                }
            }

            if (debug)
                OmrUtils.drawPoints(
                    gray,
                    points = centersInBuffer,
                ).apply {
                    debugBitmaps["full image with found anchor"] = toBitmapSafe()
                }

            if (!allFound) {
                return OmrImageProcessResult(
                    success = false,
                    debugBitmaps = debugBitmaps
                ) to centersInBuffer
            }

            val templateAnchors = listOf(
                omrTemplate.anchor_top_left,
                omrTemplate.anchor_top_right,
                omrTemplate.anchor_bottom_right,
                omrTemplate.anchor_bottom_left
            ) // anchors from your template JSON
            val warped = omrProcessor.warpWithAnchors(
                src = gray,
                detectedAnchors = centersInBuffer,
                templateAnchors = templateAnchors,
                templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
            )
            if (debug) debugBitmaps["warped"] = warped.toBitmapSafe()

            if (centersInBuffer.size == 4) {
                val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(
                    omrTemplate
                )
                if (debug) {
                    finalBitmap = OmrUtils.drawPoints(
                        warped,
                        bubblePoints,
                        fillColor = Scalar(255.0, 255.0, 255.0), // white dots
                        textColor = Scalar(0.0, 255.0, 255.0),   // cyan labels
                    ).toBitmapSafe()
                    debugBitmaps["bubble"] = finalBitmap
                }
                if (bubblePoints.size != omrTemplate.totalBubbles()) {
                    return OmrImageProcessResult(
                        success = false,
                        debugBitmaps = debugBitmaps
                    ) to centersInBuffer
                }
            }

            gray.release()

            return OmrImageProcessResult(
                success = true,
                debugBitmaps = debugBitmaps,
                finalBitmap = finalBitmap
            ) to centersInBuffer
        } catch (e: Exception) {
            return OmrImageProcessResult(
                success = false,
                reason = e.toString(),
                debugBitmaps = debugBitmaps
            ) to listOf()
        }
    }
}