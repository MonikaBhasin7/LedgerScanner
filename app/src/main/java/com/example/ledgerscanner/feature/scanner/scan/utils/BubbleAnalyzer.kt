package com.example.ledgerscanner.feature.scanner.scan.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.OmrDetectionResult
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.BubbleAnalysisResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.compareTo
import kotlin.math.roundToInt

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

class BubbleAnalyzer @Inject constructor() {

    companion object {
        const val TAG = "BubbleAnalyzer"

        // Bubble detection thresholds
        private const val MIN_CONFIDENCE = 0.60 // Minimum confidence to mark as filled
        private const val CENTER_RADIUS_FACTOR = 0.5
        private const val GAUSSIAN_BLUR_SIZE = 5.0
        private const val GAUSSIAN_BLUR_SIGMA = 1.5
        private const val FILLED_THRESHOLD_GRAYSCALE = 100.0

        // Improved bubble detection parameters
        private const val INNER_CIRCLE_RATIO = 0.75           // Analyze 75% of radius
        private const val BACKGROUND_SAMPLE_RATIO = 1.3       // Sample background at 130% radius
        private const val RELATIVE_THRESHOLD = 0.65           // Bubble must be 35% darker
        private const val DARK_PIXEL_PERCENTAGE = 35.0        // At least 35% dark pixels
        private const val BINARY_THRESHOLD_RATIO = 0.80       // For counting dark pixels
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

        omrTemplate.questions.forEachIndexed { questionIndex, question ->
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

                // Detect if bubble is filled using improved algorithm
                val analysisResult = analyzeBubbleFill(
                    srcMat = warped,
                    cx = cx,
                    cy = cy,
                    radius = omrTemplate.getAverageRadius()
                )

                // Apply confidence threshold
                val isMarked = analysisResult.isFilled && analysisResult.confidence >= MIN_CONFIDENCE

//                // Detect if bubble is filled
//                val (isFilled, confidence) = isBubbleFilled(
//                    srcMat = warped,
//                    cx = cx.toLong(),
//                    cy = cy.toLong(),
//                    radius = omrTemplate.getAverageRadius()
//                )

                // Apply confidence threshold
//                val isMarked = isFilled && confidence >= MIN_CONFIDENCE

                // Check if marked option is correct
                if (isMarked) {
                    bubbleResults.add(
                        BubbleResult(
                            point = bubblePoint,
                            questionIndex = questionIndex,
                            optionIndex = optionIndex,
                            confidence = analysisResult.confidence
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
     * Improved bubble fill detection using:
     * 1. Shrunken analysis region (75% of radius) to avoid border contamination
     * 2. Relative threshold based on surrounding background
     * 3. Dark pixel percentage validation
     *
     * @param srcMat Source grayscale image
     * @param cx Center X coordinate
     * @param cy Center Y coordinate
     * @param radius Bubble radius in pixels
     * @return BubbleAnalysisResult with detection details
     */
    @WorkerThread
    private fun analyzeBubbleFill(
        srcMat: Mat,
        cx: Int,
        cy: Int,
        radius: Int
    ): BubbleAnalysisResult {
        if (srcMat.empty()) {
            Log.w(TAG, "Empty Mat provided to analyzeBubbleFill")
            return BubbleAnalysisResult(
                isFilled = false,
                confidence = 0.0,
                bubbleAvgIntensity = 255.0,
                backgroundAvgIntensity = 255.0,
                darkPixelPercentage = 0.0
            )
        }

        var gray: Mat? = null
        var blurred: Mat? = null
        var innerMask: Mat? = null
        var outerCircleMask: Mat? = null
        var innerCircleMask: Mat? = null
        var backgroundMask: Mat? = null

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

            val cols = blurred.cols()
            val rows = blurred.rows()
            val center = Point(cx.toDouble(), cy.toDouble())

            // Step 1: Calculate analysis regions
            val innerRadius = (radius * INNER_CIRCLE_RATIO).roundToInt()
            val backgroundInnerRadius = radius
            val backgroundOuterRadius = (radius * BACKGROUND_SAMPLE_RATIO).roundToInt()

            // Step 2: Create mask for inner circle (75% of radius) - for bubble analysis
            innerMask = Mat.zeros(rows, cols, blurred.type())
            Imgproc.circle(
                innerMask,
                center,
                innerRadius,
                Scalar(255.0),
                -1  // Filled circle
            )

            // Step 3: Create annulus mask for background sampling (ring between 100% and 130%)
            // Create outer circle
            outerCircleMask = Mat.zeros(rows, cols, blurred.type())
            Imgproc.circle(
                outerCircleMask,
                center,
                backgroundOuterRadius,
                Scalar(255.0),
                -1
            )

            // Create inner circle (to subtract)
            innerCircleMask = Mat.zeros(rows, cols, blurred.type())
            Imgproc.circle(
                innerCircleMask,
                center,
                backgroundInnerRadius,
                Scalar(255.0),
                -1
            )

            // Background mask = outer circle - inner circle (creates ring/annulus)
            backgroundMask = Mat()
            Core.subtract(outerCircleMask, innerCircleMask, backgroundMask)

            // Step 4: Sample background average intensity using the annulus mask
            val backgroundMeanScalar = Core.mean(blurred, backgroundMask)
            val backgroundAvg = backgroundMeanScalar.`val`[0]

            // Step 5: Sample bubble interior average intensity using inner mask
            val bubbleMeanScalar = Core.mean(blurred, innerMask)
            val bubbleAvg = bubbleMeanScalar.`val`[0]

            // Step 6: Calculate dark pixel percentage in inner region
            val binaryThreshold = backgroundAvg * BINARY_THRESHOLD_RATIO
            val darkPixelPercentage = calculateDarkPixelPercentage(
                blurred,
                innerMask,
                binaryThreshold
            )

            // Step 7: Decision logic
            val intensityCondition = bubbleAvg < (backgroundAvg * RELATIVE_THRESHOLD)
            val percentageCondition = darkPixelPercentage > DARK_PIXEL_PERCENTAGE

            val isFilled = intensityCondition && percentageCondition

            // Step 8: Calculate confidence score (0.0 to 1.0)
            val confidence = if (backgroundAvg > 0) {
                val intensityRatio = 1.0 - (bubbleAvg / backgroundAvg)
                val percentageRatio = darkPixelPercentage / 100.0
                ((intensityRatio * 0.6) + (percentageRatio * 0.4)).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

            return BubbleAnalysisResult(
                isFilled = isFilled,
                confidence = confidence,
                bubbleAvgIntensity = bubbleAvg,
                backgroundAvgIntensity = backgroundAvg,
                darkPixelPercentage = darkPixelPercentage
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing bubble at ($cx, $cy)", e)
            return BubbleAnalysisResult(
                isFilled = false,
                confidence = 0.0,
                bubbleAvgIntensity = 255.0,
                backgroundAvgIntensity = 255.0,
                darkPixelPercentage = 0.0
            )
        } finally {
            if (gray !== srcMat) gray?.release()
            blurred?.release()
            innerMask?.release()
            outerCircleMask?.release()
            innerCircleMask?.release()
            backgroundMask?.release()
        }
    }

    /**
     * Calculates percentage of dark pixels in the masked region
     *
     * @param image Grayscale image
     * @param mask Binary mask defining region of interest
     * @param threshold Intensity threshold for dark pixels
     * @return Percentage of dark pixels (0-100)
     */
    @WorkerThread
    private fun calculateDarkPixelPercentage(
        image: Mat,
        mask: Mat,
        threshold: Double
    ): Double {
        var binaryMat: Mat? = null
        var maskedBinary: Mat? = null

        try {
            // Create binary image based on threshold
            binaryMat = Mat()
            Imgproc.threshold(
                image,
                binaryMat,
                threshold,
                255.0,
                Imgproc.THRESH_BINARY_INV // Invert: dark pixels become white
            )

            // Apply mask to binary image
            maskedBinary = Mat()
            Core.bitwise_and(binaryMat, mask, maskedBinary)

            // Count non-zero pixels (dark pixels in original)
            val darkPixelCount = Core.countNonZero(maskedBinary)

            // Count total pixels in mask
            val totalPixelCount = Core.countNonZero(mask)

            return if (totalPixelCount > 0) {
                (darkPixelCount.toDouble() / totalPixelCount.toDouble()) * 100.0
            } else {
                0.0
            }

        } finally {
            binaryMat?.release()
            maskedBinary?.release()
        }
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
                Log.v(
                    TAG, "Bubble($x,$y): avgGray=${avgGrayscale.toInt()}, " +
                            "isFilled=$isFilled, confidence=${"%.3f".format(confidence)}"
                )
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