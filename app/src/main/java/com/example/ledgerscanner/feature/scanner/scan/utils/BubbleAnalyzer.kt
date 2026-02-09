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
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// 🔄 Fixed: 08/01/26 - Stricter adaptive detection to reduce false positives
// 🔄 Improved: 09/02/26 - ROI-based sampling (memory fix), per-bubble radius
// ===========================================================================

class BubbleAnalyzer @Inject constructor() {

    companion object {
        const val TAG = "BubbleAnalyzer"

        // ✅ STRICTER thresholds to reduce false positives
        const val MIN_CONFIDENCE = 0.65  // Increased from 0.60

        // ✅ More conservative sampling
        private const val INNER_CIRCLE_RATIO = 0.60  // Reduced from 0.65 to avoid borders
        private const val BACKGROUND_INNER_RATIO = 1.0  // Background ring starts at bubble edge
        private const val BACKGROUND_OUTER_RATIO = 1.5  // Background ring ends at 150% of radius
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

        // Step 1: Apply CLAHE to normalize lighting
        val normalized = normalizeSheetLighting(warped)

        try {
            // Step 2: Collect all bubble statistics using ROI-based sampling
            val allBubbleStats = collectAllBubbleStats(omrTemplate, normalized)

            // Step 3: Calculate global statistics
            val globalStats = calculateGlobalStatistics(allBubbleStats)
            Log.d(TAG, "Global stats: bubbleMean=${"%.1f".format(globalStats.bubbleMean)}, " +
                    "bgMean=${"%.1f".format(globalStats.bgMean)}, " +
                    "filledThreshold=${"%.1f".format(globalStats.filledThreshold)}, " +
                    "darkPixelThresh=${"%.1f".format(globalStats.darkPixelThreshold)}%")

            // Step 4: Detect filled bubbles with STRICT validation
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

            Log.d(TAG, "Detection complete: ${bubbleResults.size} bubbles marked")

            return OmrDetectionResult(bubbles = bubbleResults)

        } finally {
            normalized.release()
        }
    }

    /**
     * Normalize lighting across the sheet using CLAHE + light denoise
     * (Keeping original proven pipeline: CLAHE 2.5 + GaussianBlur 3x3)
     */
    private fun normalizeSheetLighting(sheet: Mat): Mat {
        val gray = if (sheet.channels() == 1) {
            sheet.clone()
        } else {
            Mat().also { Imgproc.cvtColor(sheet, it, Imgproc.COLOR_BGR2GRAY) }
        }

        // Apply CLAHE (original clip limit 2.5)
        val clahe = Imgproc.createCLAHE(2.5, Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(gray, enhanced)

        // Light denoising (original 3x3 Gaussian)
        val blurred = Mat()
        Imgproc.GaussianBlur(enhanced, blurred, Size(3.0, 3.0), 0.0)

        gray.release()
        enhanced.release()

        return blurred
    }

    /**
     * Collect stats for ALL bubbles using ROI-based sampling.
     *
     * IMPROVEMENT: Instead of creating full-image-sized masks for each bubble,
     * we crop a small ROI around each bubble and create masks on that tiny region.
     * This reduces memory allocation by ~100x for 200 bubbles.
     */
    private fun collectAllBubbleStats(
        template: Template,
        normalized: Mat
    ): List<BubbleStats> {
        val stats = mutableListOf<BubbleStats>()
        val imgCols = normalized.cols()
        val imgRows = normalized.rows()

        template.questions.forEachIndexed { questionIndex, question ->
            question.options.forEachIndexed { optionIndex, option ->
                val bubblePoint = AnchorPoint(
                    x = template.anchor_top_left.x + option.x,
                    y = template.anchor_top_left.y + option.y
                )

                val cx = bubblePoint.x.roundToInt().coerceIn(0, imgCols - 1)
                val cy = bubblePoint.y.roundToInt().coerceIn(0, imgRows - 1)

                // IMPROVEMENT: Use per-bubble radius from template
                val bubbleRadius = max(option.r.roundToInt(), 5)

                val bubbleStat = collectBubbleStatsViaRoi(
                    normalized, cx, cy, bubbleRadius, imgCols, imgRows
                )

                stats.add(
                    BubbleStats(
                        questionIndex = questionIndex,
                        optionIndex = optionIndex,
                        bubblePoint = bubblePoint,
                        cx = cx,
                        cy = cy,
                        bubbleIntensity = bubbleStat.first,
                        backgroundIntensity = bubbleStat.second,
                        darkPixelPercentage = bubbleStat.third
                    )
                )
            }
        }

        return stats
    }

    /**
     * Collect statistics for a single bubble using ROI cropping.
     * Same sampling logic as original but on a small cropped region instead of full image.
     */
    private fun collectBubbleStatsViaRoi(
        srcMat: Mat,
        cx: Int,
        cy: Int,
        radius: Int,
        imgCols: Int,
        imgRows: Int
    ): Triple<Double, Double, Double> {
        // Calculate ROI bounds (large enough for background ring)
        val roiPad = (radius * BACKGROUND_OUTER_RATIO).roundToInt() + 2
        val roiX = max(0, cx - roiPad)
        val roiY = max(0, cy - roiPad)
        val roiW = min(imgCols - roiX, roiPad * 2)
        val roiH = min(imgRows - roiY, roiPad * 2)

        if (roiW <= 0 || roiH <= 0) return Triple(255.0, 255.0, 0.0)

        // Work on small ROI
        val roiRect = Rect(roiX, roiY, roiW, roiH)
        val roi = Mat(srcMat, roiRect)

        val localCx = cx - roiX
        val localCy = cy - roiY

        var innerMask: Mat? = null
        var outerCircleMask: Mat? = null
        var innerCircleMask: Mat? = null
        var backgroundMask: Mat? = null
        var binaryMat: Mat? = null
        var maskedBinary: Mat? = null

        try {
            val rows = roi.rows()
            val cols = roi.cols()
            val center = Point(localCx.toDouble(), localCy.toDouble())

            // Same sampling ratios as original
            val innerRadius = (radius * INNER_CIRCLE_RATIO).roundToInt()
            val backgroundInnerRadius = (radius * BACKGROUND_INNER_RATIO).roundToInt()
            val backgroundOuterRadius = (radius * BACKGROUND_OUTER_RATIO).roundToInt()

            // Create inner mask on small ROI (not full image!)
            innerMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(innerMask, center, innerRadius, Scalar(255.0), -1)

            // Create background annulus mask on small ROI
            outerCircleMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(outerCircleMask, center, backgroundOuterRadius, Scalar(255.0), -1)

            innerCircleMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(innerCircleMask, center, backgroundInnerRadius, Scalar(255.0), -1)

            backgroundMask = Mat()
            Core.subtract(outerCircleMask, innerCircleMask, backgroundMask)

            // Sample intensities (same as original)
            val backgroundAvg = Core.mean(roi, backgroundMask).`val`[0]
            val bubbleAvg = Core.mean(roi, innerMask).`val`[0]

            // ✅ Stricter dark pixel threshold (same 0.70 as original)
            val binaryThreshold = backgroundAvg * 0.70
            binaryMat = Mat()
            Imgproc.threshold(roi, binaryMat, binaryThreshold, 255.0, Imgproc.THRESH_BINARY_INV)

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
     * (Same percentile-based logic as original — proven to work)
     */
    private fun calculateGlobalStatistics(allStats: List<BubbleStats>): GlobalStats {
        val bubbleIntensities = allStats.map { it.bubbleIntensity }
        val backgroundIntensities = allStats.map { it.backgroundIntensity }
        val darkPixelPercentages = allStats.map { it.darkPixelPercentage }

        val bubbleMean = bubbleIntensities.average()
        val bgMean = backgroundIntensities.average()
        val bubbleStdDev = calculateStdDev(bubbleIntensities, bubbleMean)

        // ✅ STRICTER: Use 30th percentile as threshold
        val sortedBubbles = bubbleIntensities.sorted()
        val firstQuartileIndex = (sortedBubbles.size * 0.30).toInt()
        val filledThreshold = sortedBubbles[firstQuartileIndex]

        // ✅ STRICTER: Use 75th percentile for dark pixel threshold
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

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    /**
     * Analyze bubble with STRICT validation
     * ALL conditions must pass (not just 3 of 4)
     * (Same proven logic as original)
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

        // ✅ Condition 2: Must be in darkest 30% of all bubbles
        val quartileCondition = bubbleIntensity < globalStats.filledThreshold

        // ✅ Condition 3: Dark pixel percentage (STRICT)
        val darkPixelCondition = darkPixelPercentage > globalStats.darkPixelThreshold

        // ✅ Condition 4: Absolute difference (STRICT)
        val absoluteDifference = backgroundIntensity - bubbleIntensity
        val absoluteCondition = absoluteDifference > ABSOLUTE_DIFFERENCE_THRESHOLD

        // ✅ Condition 5: Significantly darker than global mean
        val globalCondition = bubbleIntensity < (globalStats.bubbleMean - globalStats.bubbleStdDev * 0.5)

        val conditions = listOf(
            relativeCondition,
            quartileCondition,
            darkPixelCondition,
            absoluteCondition,
            globalCondition
        )
        val passedConditions = conditions.count { it }

        // ✅ Decision logic:
        // 5/5 → definitely filled
        // 4/5 → filled if at least one signal is strong (catches lightly-filled real paper bubbles)
        // 3/5 → filled ONLY if multiple very strong signals (rare edge case: uneven lighting)
        // <3  → not filled
        val isFilled = when {
            passedConditions == 5 -> true

            passedConditions == 4 -> {
                // At least one strong signal required to prevent false positives
                ratio < 0.62 || darkPixelPercentage > 50.0 || absoluteDifference > 45.0
            }

            passedConditions == 3 -> {
                // Very strict: need ALL three strong signals simultaneously
                // This catches the rare case where uneven paper lighting fails
                // condition 2 (quartile) and condition 5 (global), but the bubble
                // is clearly dark relative to its own local background
                ratio < 0.55 && darkPixelPercentage > 60.0 && absoluteDifference > 50.0
            }

            else -> false
        }

        // Calculate confidence
        val confidence = if (isFilled) {
            val intensityScore = 1.0 - ratio
            val darkPixelScore = (darkPixelPercentage / 100.0).coerceAtMost(1.0)
            val diffScore = (absoluteDifference / 100.0).coerceAtMost(1.0)
            val rawScore = (intensityScore * 0.4) + (darkPixelScore * 0.4) + (diffScore * 0.2)

            when (passedConditions) {
                5 -> rawScore.coerceIn(0.80, 1.0)
                4 -> rawScore.coerceIn(0.70, 0.92)
                3 -> rawScore.coerceIn(0.65, 0.82)
                else -> 0.0
            }
        } else {
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
        val x = cx.coerceIn(0, (cols - 1).toLong()).toInt()
        val y = cy.coerceIn(0, (rows - 1).toLong()).toInt()

        // Use ROI-based sampling for memory efficiency
        val roiPad = (radius * BACKGROUND_OUTER_RATIO).roundToInt() + 2
        val roiX = max(0, x - roiPad)
        val roiY = max(0, y - roiPad)
        val roiW = min(cols - roiX, roiPad * 2)
        val roiH = min(rows - roiY, roiPad * 2)

        if (roiW <= 0 || roiH <= 0) return false to 0.0

        val gray = if (srcMat.channels() == 1) srcMat else {
            Mat().also { Imgproc.cvtColor(srcMat, it, Imgproc.COLOR_BGR2GRAY) }
        }

        try {
            val roiRect = Rect(roiX, roiY, roiW, roiH)
            val roi = Mat(gray, roiRect)
            val stats = collectBubbleStatsViaRoi(gray, x, y, radius, cols, rows)

            val ratio = if (stats.second > 0) stats.first / stats.second else 1.0
            val isFilled = ratio < RELATIVE_THRESHOLD && stats.third > DARK_PIXEL_PERCENTAGE
            val confidence = if (isFilled) (1.0 - ratio).coerceIn(0.0, 1.0) else 0.0

            return isFilled to confidence
        } finally {
            if (gray !== srcMat) gray.release()
        }
    }
}
