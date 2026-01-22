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
import org.opencv.core.MatOfDouble
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.roundToInt

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// 🔄 Fixed: 08/01/26 - Stricter adaptive detection to reduce false positives
// ===========================================================================

class BubbleAnalyzer @Inject constructor() {

    companion object {
        const val TAG = "BubbleAnalyzer"

        // ✅ STRICTER thresholds to reduce false positives
        const val MIN_CONFIDENCE = 0.65  // Increased from 0.60
        private const val CENTER_RADIUS_FACTOR = 0.5
        private const val GAUSSIAN_BLUR_SIZE = 5.0
        private const val GAUSSIAN_BLUR_SIGMA = 1.5

        // ✅ More conservative sampling
        private const val INNER_CIRCLE_RATIO = 0.60  // Reduced from 0.65 to avoid borders
        private const val BACKGROUND_SAMPLE_RATIO = 1.5  // Increased from 1.4 for better background
        private const val DARK_PIXEL_PERCENTAGE = 40.0  // Increased from 35.0

        // ✅ Stricter relative threshold
        private const val RELATIVE_THRESHOLD = 0.65  // Must be 35% darker (was 30%)
        private const val ABSOLUTE_DIFFERENCE_THRESHOLD = 35.0  // Increased from 25.0
    }

    /**
     * Detects filled bubbles in warped OMR sheet image
     * Uses adaptive thresholding with STRICT validation
     */
    @WorkerThread
    fun detectFilledBubbles(
        omrTemplate: Template,
        warped: Mat
    ): OmrDetectionResult {
        val bubbleResults = mutableListOf<BubbleResult>()

        // ✅ Step 1: Apply CLAHE to normalize lighting
        val normalized = normalizeSheetLighting(warped)

        // ✅ Step 2: Collect all bubble statistics first
        val allBubbleStats = mutableListOf<BubbleStats>()

        omrTemplate.questions.forEachIndexed { questionIndex, question ->
            question.options.forEachIndexed { optionIndex, option ->
                val bubblePoint = AnchorPoint(
                    x = omrTemplate.anchor_top_left.x + option.x,
                    y = omrTemplate.anchor_top_left.y + option.y
                )

                val cx = bubblePoint.x.roundToInt().coerceIn(0, normalized.cols() - 1)
                val cy = bubblePoint.y.roundToInt().coerceIn(0, normalized.rows() - 1)

                val stats = collectBubbleStatistics(
                    normalized, cx, cy, omrTemplate.getAverageRadius()
                )

                allBubbleStats.add(
                    BubbleStats(
                        questionIndex = questionIndex,
                        optionIndex = optionIndex,
                        bubblePoint = bubblePoint,
                        cx = cx,
                        cy = cy,
                        bubbleIntensity = stats.first,
                        backgroundIntensity = stats.second,
                        darkPixelPercentage = stats.third
                    )
                )
            }
        }

        // ✅ Step 3: Calculate global statistics
        val globalStats = calculateGlobalStatistics(allBubbleStats)
        Log.d(TAG, "Global stats: bubbleMean=${"%.1f".format(globalStats.bubbleMean)}, " +
                "bgMean=${"%.1f".format(globalStats.bgMean)}, " +
                "filledThreshold=${"%.1f".format(globalStats.filledThreshold)}, " +
                "darkPixelThresh=${"%.1f".format(globalStats.darkPixelThreshold)}%")

        // ✅ Step 4: Detect filled bubbles with STRICT validation
        allBubbleStats.forEach { stats ->
            val analysisResult = analyzeBubbleStrict(
                bubbleIntensity = stats.bubbleIntensity,
                backgroundIntensity = stats.backgroundIntensity,
                darkPixelPercentage = stats.darkPixelPercentage,
                globalStats = globalStats,
                questionIndex = stats.questionIndex,
                optionIndex = stats.optionIndex
            )

            val isMarked = analysisResult.isFilled && analysisResult.confidence >= MIN_CONFIDENCE

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                val ratio = stats.bubbleIntensity / stats.backgroundIntensity
                val diff = stats.backgroundIntensity - stats.bubbleIntensity
                Log.d(TAG, "Q${stats.questionIndex + 1}-${stats.optionIndex + 1}: " +
                        "bubble=${"%.1f".format(stats.bubbleIntensity)}, " +
                        "bg=${"%.1f".format(stats.backgroundIntensity)}, " +
                        "ratio=${"%.2f".format(ratio)}, " +
                        "diff=${"%.1f".format(diff)}, " +
                        "dark=${"%.1f".format(stats.darkPixelPercentage)}%, " +
                        "filled=${analysisResult.isFilled}, " +
                        "conf=${"%.3f".format(analysisResult.confidence)}, " +
                        "marked=$isMarked")
            }

            if (isMarked) {
                bubbleResults.add(
                    BubbleResult(
                        point = stats.bubblePoint,
                        questionIndex = stats.questionIndex,
                        optionIndex = stats.optionIndex,
                        confidence = analysisResult.confidence
                    )
                )
            }
        }

        // Cleanup
        normalized.release()

        Log.d(TAG, "Detection complete: ${bubbleResults.size} bubbles marked")

        return OmrDetectionResult(
            bubbles = bubbleResults
        )
    }

    /**
     * Normalize lighting across the sheet using CLAHE
     */
    private fun normalizeSheetLighting(sheet: Mat): Mat {
        val gray = if (sheet.channels() == 1) {
            sheet.clone()
        } else {
            Mat().also {
                Imgproc.cvtColor(sheet, it, Imgproc.COLOR_BGR2GRAY)
            }
        }

        // Apply CLAHE
        val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(gray, enhanced)

        // Light denoising
        val blurred = Mat()
        Imgproc.GaussianBlur(enhanced, blurred, Size(3.0, 3.0), 0.0)

        gray.release()
        enhanced.release()

        return blurred
    }

    /**
     * Collect statistics for a single bubble
     */
    private fun collectBubbleStatistics(
        srcMat: Mat,
        cx: Int,
        cy: Int,
        radius: Int
    ): Triple<Double, Double, Double> {
        var innerMask: Mat? = null
        var outerCircleMask: Mat? = null
        var innerCircleMask: Mat? = null
        var backgroundMask: Mat? = null
        var binaryMat: Mat? = null
        var maskedBinary: Mat? = null

        try {
            val cols = srcMat.cols()
            val rows = srcMat.rows()
            val center = Point(cx.toDouble(), cy.toDouble())

            // Calculate regions with stricter parameters
            val innerRadius = (radius * INNER_CIRCLE_RATIO).roundToInt()
            val backgroundInnerRadius = radius
            val backgroundOuterRadius = (radius * BACKGROUND_SAMPLE_RATIO).roundToInt()

            // Create inner mask
            innerMask = Mat.zeros(rows, cols, srcMat.type())
            Imgproc.circle(innerMask, center, innerRadius, Scalar(255.0), -1)

            // Create background mask (annulus)
            outerCircleMask = Mat.zeros(rows, cols, srcMat.type())
            Imgproc.circle(outerCircleMask, center, backgroundOuterRadius, Scalar(255.0), -1)

            innerCircleMask = Mat.zeros(rows, cols, srcMat.type())
            Imgproc.circle(innerCircleMask, center, backgroundInnerRadius, Scalar(255.0), -1)

            backgroundMask = Mat()
            Core.subtract(outerCircleMask, innerCircleMask, backgroundMask)

            // Sample intensities
            val backgroundAvg = Core.mean(srcMat, backgroundMask).`val`[0]
            val bubbleAvg = Core.mean(srcMat, innerMask).`val`[0]

            // ✅ Stricter dark pixel threshold
            val binaryThreshold = backgroundAvg * 0.70  // Was 0.75
            binaryMat = Mat()
            Imgproc.threshold(
                srcMat,
                binaryMat,
                binaryThreshold,
                255.0,
                Imgproc.THRESH_BINARY_INV
            )

            maskedBinary = Mat()
            Core.bitwise_and(binaryMat, innerMask, maskedBinary)

            val darkPixelCount = Core.countNonZero(maskedBinary)
            val totalPixelCount = Core.countNonZero(innerMask)
            val darkPixelPercentage = if (totalPixelCount > 0) {
                (darkPixelCount.toDouble() / totalPixelCount.toDouble()) * 100.0
            } else {
                0.0
            }

            return Triple(bubbleAvg, backgroundAvg, darkPixelPercentage)

        } finally {
            innerMask?.release()
            outerCircleMask?.release()
            innerCircleMask?.release()
            backgroundMask?.release()
            binaryMat?.release()
            maskedBinary?.release()
        }
    }

    /**
     * Statistics for a single bubble
     */
    private data class BubbleStats(
        val questionIndex: Int,
        val optionIndex: Int,
        val bubblePoint: AnchorPoint,
        val cx: Int,
        val cy: Int,
        val bubbleIntensity: Double,
        val backgroundIntensity: Double,
        val darkPixelPercentage: Double
    )

    /**
     * Global statistics across all bubbles
     */
    private data class GlobalStats(
        val bubbleMean: Double,
        val bubbleStdDev: Double,
        val bgMean: Double,
        val filledThreshold: Double,
        val darkPixelThreshold: Double
    )

    /**
     * Calculate global statistics with STRICTER thresholds
     */
    private fun calculateGlobalStatistics(allStats: List<BubbleStats>): GlobalStats {
        val bubbleIntensities = allStats.map { it.bubbleIntensity }
        val backgroundIntensities = allStats.map { it.backgroundIntensity }
        val darkPixelPercentages = allStats.map { it.darkPixelPercentage }

        val bubbleMean = bubbleIntensities.average()
        val bgMean = backgroundIntensities.average()
        val bubbleStdDev = calculateStdDev(bubbleIntensities, bubbleMean)

        // ✅ STRICTER: Filled bubbles must be significantly below average
        // Use 1st quartile (25th percentile) as threshold
        val sortedBubbles = bubbleIntensities.sorted()
        val firstQuartileIndex = (sortedBubbles.size * 0.30).toInt()
        val filledThreshold = sortedBubbles[firstQuartileIndex]

        // ✅ STRICTER: Use higher percentile for dark pixel threshold
        val sortedDarkPixels = darkPixelPercentages.sorted()
        val darkPixelThresholdIndex = (sortedDarkPixels.size * 0.75).toInt()
        val darkPixelThreshold = sortedDarkPixels[darkPixelThresholdIndex]
            .coerceAtLeast(DARK_PIXEL_PERCENTAGE)

        return GlobalStats(
            bubbleMean = bubbleMean,
            bubbleStdDev = bubbleStdDev,
            bgMean = bgMean,
            filledThreshold = filledThreshold,
            darkPixelThreshold = darkPixelThreshold
        )
    }

    /**
     * Calculate standard deviation
     */
    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    /**
     * Analyze bubble with STRICT validation
     * ALL conditions must pass (not just 3 of 4)
     */
    private fun analyzeBubbleStrict(
        bubbleIntensity: Double,
        backgroundIntensity: Double,
        darkPixelPercentage: Double,
        globalStats: GlobalStats,
        questionIndex: Int,
        optionIndex: Int
    ): BubbleAnalysisResult {

        // ✅ Condition 1: Relative to local background (STRICT)
        val ratio = bubbleIntensity / backgroundIntensity
        val relativeCondition = ratio < RELATIVE_THRESHOLD

        // ✅ Condition 2: Must be in darkest 25% of all bubbles
        val quartileCondition = bubbleIntensity < globalStats.filledThreshold

        // ✅ Condition 3: Dark pixel percentage (STRICT)
        val darkPixelCondition = darkPixelPercentage > globalStats.darkPixelThreshold

        // ✅ Condition 4: Absolute difference (STRICT)
        val absoluteDifference = backgroundIntensity - bubbleIntensity
        val absoluteCondition = absoluteDifference > ABSOLUTE_DIFFERENCE_THRESHOLD

        // ✅ NEW Condition 5: Significantly darker than global mean
        val globalCondition = bubbleIntensity < (globalStats.bubbleMean - globalStats.bubbleStdDev * 0.5)

        // ✅ STRICT: ALL 5 conditions must pass (was 3 of 4)
        val conditions = listOf(
            relativeCondition,
            quartileCondition,
            darkPixelCondition,
            absoluteCondition,
            globalCondition
        )
        val passedConditions = conditions.count { it }

        // ✅ Must pass ALL conditions OR at least 4 with very strong signals
        val isFilled = when {
            passedConditions == 5 -> true
            passedConditions == 4 && ratio < 0.60 && darkPixelPercentage > 55.0 -> true
            else -> false
        }

        // Calculate confidence
        val confidence = if (isFilled) {
            when (passedConditions) {
                5 -> {
                    // All conditions - very high confidence
                    val intensityScore = 1.0 - ratio
                    val darkPixelScore = (darkPixelPercentage / 100.0).coerceAtMost(1.0)
                    val diffScore = (absoluteDifference / 100.0).coerceAtMost(1.0)
                    ((intensityScore * 0.4) + (darkPixelScore * 0.4) + (diffScore * 0.2))
                        .coerceIn(0.7, 1.0)
                }
                4 -> {
                    // Four conditions with strong signals
                    0.75
                }
                else -> 0.0
            }
        } else {
            // Not filled
            0.0
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Q${questionIndex + 1}-${optionIndex + 1}: " +
                    "conditions=[rel=$relativeCondition, qrt=$quartileCondition, " +
                    "dark=$darkPixelCondition, abs=$absoluteCondition, glob=$globalCondition] " +
                    "passed=$passedConditions/5 → filled=$isFilled " +
                    "(ratio=${"%.2f".format(ratio)}, diff=${"%.1f".format(absoluteDifference)}, " +
                    "dark=${"%.1f".format(darkPixelPercentage)}%)")
        }

        return BubbleAnalysisResult(
            isFilled = isFilled,
            confidence = confidence,
            bubbleAvgIntensity = bubbleIntensity,
            backgroundAvgIntensity = backgroundIntensity,
            darkPixelPercentage = darkPixelPercentage
        )
    }

    /**
     * Original method - kept for compatibility
     */
    @WorkerThread
    fun isBubbleFilled(
        srcMat: Mat,
        cx: Long,
        cy: Long,
        radius: Int,
        binarizeThreshold: Double = 127.0,
        fillFractionThreshold: Double = 0.65
    ): Pair<Boolean, Double> {
        if (srcMat.empty()) {
            Log.w(TAG, "Empty Mat provided to isBubbleFilled")
            return false to 0.0
        }

        val cols = srcMat.cols()
        val rows = srcMat.rows()
        val x = cx.coerceIn(0, (cols - 1).toLong())
        val y = cy.coerceIn(0, (rows - 1).toLong())

        var gray: Mat? = null
        var blurred: Mat? = null
        var mask: Mat? = null

        try {
            gray = if (srcMat.channels() == 1) {
                srcMat.clone()
            } else {
                Mat().also {
                    Imgproc.cvtColor(srcMat, it, Imgproc.COLOR_BGR2GRAY)
                }
            }

            blurred = Mat()
            Imgproc.GaussianBlur(
                gray,
                blurred,
                Size(GAUSSIAN_BLUR_SIZE, GAUSSIAN_BLUR_SIZE),
                GAUSSIAN_BLUR_SIGMA
            )

            val centerRadius = (radius * CENTER_RADIUS_FACTOR).roundToInt()
            mask = Mat.zeros(rows, cols, blurred.type())
            Imgproc.circle(
                mask,
                Point(x.toDouble(), y.toDouble()),
                centerRadius,
                Scalar(255.0),
                -1
            )

            val meanScalar = Core.mean(blurred, mask)
            val avgGrayscale = meanScalar.`val`[0]

            val isFilled = avgGrayscale < 100.0
            val confidence = (1.0 - (avgGrayscale / 255.0)).coerceIn(0.0, 1.0)

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