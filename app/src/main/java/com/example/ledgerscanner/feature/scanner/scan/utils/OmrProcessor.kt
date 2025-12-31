package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.OmrDetectionResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.roundToInt

class OmrProcessor @Inject constructor() {

    companion object {
        private const val TAG = "OmrProcessor"

        // Bubble detection thresholds
        private const val CENTER_RADIUS_FACTOR = 0.5
        private const val GAUSSIAN_BLUR_SIZE = 5.0
        private const val GAUSSIAN_BLUR_SIGMA = 1.5
        private const val FILLED_THRESHOLD_GRAYSCALE = 100.0
        private const val MIN_CONFIDENCE = 0.60

        val templateProcessor = TemplateProcessor()
    }

    /**
     * Warps source image to template dimensions using perspective transformation
     *
     * @param src Source grayscale Mat
     * @param detectedAnchors Detected anchor points in source image
     * @param templateAnchors Expected anchor points from template
     * @param templateSize Target dimensions for warped image
     * @return Warped Mat in template coordinate space
     */
    @WorkerThread
    fun warpWithAnchors(
        src: Mat,
        detectedAnchors: List<AnchorPoint>,
        templateAnchors: List<AnchorPoint>,
        templateSize: Size
    ): Mat {
        var srcPoints: MatOfPoint2f? = null
        var dstPoints: MatOfPoint2f? = null
        var homography: Mat? = null

        try {
            srcPoints = MatOfPoint2f(*detectedAnchors.map { Point(it.x, it.y) }.toTypedArray())
            dstPoints = MatOfPoint2f(*templateAnchors.map { Point(it.x, it.y) }.toTypedArray())
            homography = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

            val warped = Mat()
            Imgproc.warpPerspective(src, warped, homography, templateSize)

            return warped
        } finally {
            srcPoints?.release()
            dstPoints?.release()
            homography?.release()
        }
    }

    /**
     * Detects anchor centers within overlay rectangles on screen
     *
     * @param overlayRects Screen rectangles where anchors are expected
     * @param previewRect Camera preview rectangle bounds
     * @param gray Grayscale image from camera
     * @param debug Enable debug bitmap generation
     * @param debugBitmaps Map to store debug bitmaps
     * @param centersOut Output list to populate with detected centers
     * @return true if all anchors found, false otherwise
     */
    @WorkerThread
    fun findCentersInBuffer(
        overlayRects: List<RectF>,
        previewRect: RectF,
        gray: Mat,
        centersOut: MutableList<AnchorPoint>,
        onDebug: (String, Bitmap) -> Unit
    ): Boolean {
        var allAnchorsFound = true

        overlayRects.forEachIndexed { index, overlayRect ->
            var roi: Mat? = null

            try {
                // Map screen coordinates to Mat coordinates
                val matRect = ImageConversionUtils.screenRectToMatRect(
                    overlayRect,
                    previewRect,
                    gray
                )

                // Extract region of interest
                roi = Mat(gray, matRect)

                onDebug("roi_anchor_$index", roi.toBitmapSafe())

                // Detect anchor in ROI
                val anchorInRoi = OpenCvUtils.detectAnchorInRoi(roi)

                if (anchorInRoi == null) {
                    allAnchorsFound = false
                    Log.w(TAG, "Anchor $index not detected in ROI")
                } else {
                    // Convert ROI coordinates to full image coordinates
                    val anchorInImage = AnchorPoint(
                        x = (matRect.x + anchorInRoi.x.toFloat()).toDouble(),
                        y = (matRect.y + anchorInRoi.y.toFloat()).toDouble()
                    )
                    centersOut.add(anchorInImage)
                }
            } finally {
                roi?.release()
            }
        }

        return allAnchorsFound
    }

    /**
     * Warps image to template space and optionally generates debug bitmap
     *
     * @param gray Grayscale image to warp
     * @param omrTemplate Template containing anchor positions and dimensions
     * @param centersInBuffer Detected anchor centers
     * @param debug Enable debug bitmap generation
     * @param debugBitmaps Map to store debug bitmaps
     * @return Pair of (warped Mat, optional debug bitmap)
     */
    @WorkerThread
    fun warpWithTemplateAndGetWarped(
        gray: Mat,
        omrTemplate: Template,
        centersInBuffer: List<AnchorPoint>,
    ): Pair<Mat, Bitmap?> {
        val warped = warpWithAnchors(
            src = gray,
            detectedAnchors = centersInBuffer,
            templateAnchors = omrTemplate.getAnchorListClockwise(),
            templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
        )

        return warped to warped.toBitmapSafe()
    }

    /**
     * Detects filled bubbles in warped OMR sheet image
     *
     * @param omrTemplate Template containing bubble positions
     * @param warped Warped grayscale image in template coordinate space
     * @return Detection result with filled bubbles and correctness marks
     */
    @WorkerThread
    fun detectFilledBubbles(
        omrTemplate: Template,
        warped: Mat
    ): OmrDetectionResult {
        val bubbleResults = mutableListOf<BubbleResult>()

        omrTemplate.questions.forEachIndexed {questionIndex, question ->
            var isQuestionCorrect = false

            question.options.forEachIndexed { optionIndex, option ->
                // Calculate bubble position in warped image
                val bubblePoint = AnchorPoint(
                    x = omrTemplate.anchor_top_left.x + option.x,
                    y = omrTemplate.anchor_top_left.y + option.y
                )

                // Clamp coordinates to image bounds
                val cx = bubblePoint.x.roundToInt().coerceIn(0, warped.cols() - 1)
                val cy = bubblePoint.y.roundToInt().coerceIn(0, warped.rows() - 1)

                // Detect if bubble is filled
                val (isFilled, confidence) = isBubbleFilled(
                    srcMat = warped,
                    cx = cx.toLong(),
                    cy = cy.toLong(),
                    radius = omrTemplate.getAverageRadius()
                )

                // Apply confidence threshold
                val isMarked = isFilled && confidence >= MIN_CONFIDENCE

                // Check if marked option is correct
                if (isMarked) {
                    bubbleResults.add(
                        BubbleResult(
                            point = bubblePoint,
                            questionIndex = questionIndex,
                            optionIndex = optionIndex,
                            confidence = confidence
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Detection complete: ${bubbleResults.size} bubbles marked")

        return OmrDetectionResult(
            bubbles = bubbleResults
        )
    }

    /**
     * Determines if a bubble is filled by analyzing grayscale intensity
     *
     * Uses center-only sampling to avoid colored rings on bubble borders.
     * Filled bubbles have dark centers (low grayscale values).
     * Empty bubbles have light centers (high grayscale values from colored rings).
     *
     * @param srcMat Source image (grayscale or color)
     * @param cx Center X coordinate
     * @param cy Center Y coordinate
     * @param radius Bubble radius in pixels
     * @return Pair of (isFilled boolean, confidence 0.0-1.0)
     */
    @WorkerThread
    fun isBubbleFilled(
        srcMat: Mat,
        cx: Long,
        cy: Long,
        radius: Int,
        binarizeThreshold: Double = 127.0, // Unused but kept for API compatibility
        fillFractionThreshold: Double = 0.65 // Unused but kept for API compatibility
    ): Pair<Boolean, Double> {
        if (srcMat.empty()) {
            Log.w(TAG, "Empty Mat provided to isBubbleFilled")
            return false to 0.0
        }

        val cols = srcMat.cols()
        val rows = srcMat.rows()

        // Clamp coordinates to image bounds
        val x = cx.coerceIn(0, (cols - 1).toLong())
        val y = cy.coerceIn(0, (rows - 1).toLong())

        var gray: Mat? = null
        var blurred: Mat? = null
        var mask: Mat? = null

        try {
            // Convert to grayscale if needed
            gray = if (srcMat.channels() == 1) {
                srcMat.clone()
            } else {
                Mat().also {
                    Imgproc.cvtColor(srcMat, it, Imgproc.COLOR_BGR2GRAY)
                }
            }

            // Apply Gaussian blur to reduce noise
            blurred = Mat()
            Imgproc.GaussianBlur(
                gray,
                blurred,
                Size(GAUSSIAN_BLUR_SIZE, GAUSSIAN_BLUR_SIZE),
                GAUSSIAN_BLUR_SIGMA
            )

            // Create circular mask for center region only (ignores colored ring)
            val centerRadius = (radius * CENTER_RADIUS_FACTOR).roundToInt()
            mask = Mat.zeros(rows, cols, blurred.type())
            Imgproc.circle(
                mask,
                Point(x.toDouble(), y.toDouble()),
                centerRadius,
                Scalar(255.0),
                -1
            )

            // Calculate average grayscale intensity in center
            val meanScalar = Core.mean(blurred, mask)
            val avgGrayscale = meanScalar.`val`[0]

            // Determine if filled
            // Dark center (low grayscale) = filled
            // Light center (high grayscale) = empty with colored ring
            val isFilled = avgGrayscale < FILLED_THRESHOLD_GRAYSCALE

            // Convert to confidence score (0.0 = white/empty, 1.0 = black/filled)
            val confidence = (1.0 - (avgGrayscale / 255.0)).coerceIn(0.0, 1.0)

            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Bubble($x,$y): avgGray=${avgGrayscale.toInt()}, " +
                        "isFilled=$isFilled, confidence=${"%.3f".format(confidence)}")
            }

            return isFilled to confidence

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing bubble at ($x, $y)", e)
            return false to 0.0
        } finally {
            if (gray !== srcMat) gray?.release()
            blurred?.release()
            mask?.release()
        }
    }
}