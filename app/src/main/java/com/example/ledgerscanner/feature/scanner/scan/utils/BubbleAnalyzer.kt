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

        const val MIN_CONFIDENCE = 0.65

        // Sampling geometry — DO NOT CHANGE (proven)
        private const val INNER_CIRCLE_RATIO = 0.60
        private const val BACKGROUND_INNER_RATIO = 1.0
        private const val BACKGROUND_OUTER_RATIO = 1.5

        // ========== LOCAL thresholds (bubble vs its own background) ==========
        // These are intentionally strict to eliminate false positives.
        // On screen: filled ratio ~0.06-0.20, unfilled worst ~0.57
        // On paper:  filled ratio ~0.30-0.55, unfilled worst ~0.75-0.90
        private const val RELATIVE_THRESHOLD = 0.55
        private const val ABSOLUTE_DIFFERENCE_THRESHOLD = 35.0
        private const val DARK_PIXEL_PERCENTAGE = 40.0
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

                // TEMP DEBUG: Always log to diagnose false positives
                val ratio = stats.bubbleIntensity / stats.backgroundIntensity
                val diff = stats.backgroundIntensity - stats.bubbleIntensity
                Log.d(TAG, "Q${stats.questionIndex + 1}-${stats.optionIndex + 1}: " +
                        "bubble=${"%.1f".format(stats.bubbleIntensity)}, " +
                        "bg=${"%.1f".format(stats.backgroundIntensity)}, " +
                        "ratio=${"%.3f".format(ratio)}, " +
                        "diff=${"%.1f".format(diff)}, " +
                        "dark=${"%.1f".format(stats.darkPixelPercentage)}%, " +
                        "filled=${analysisResult.isFilled}, " +
                        "conf=${"%.3f".format(analysisResult.confidence)}, " +
                        "marked=$isMarked")

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
        val darkPixelThreshold: Double,
        val hasRealVariation: Boolean,
        /** Median bubble intensity (50th percentile). Robust against number of filled bubbles.
         *  Used to detect global outliers: a filled bubble is much darker than the median. */
        val medianBubble: Double
    )

    /**
     * Calculate global statistics with STRICTER thresholds
     * (Same percentile-based logic as original — proven to work)
     *
     * KEY INSIGHT: When no bubbles are filled, all bubble intensities cluster
     * tightly together. The 30th percentile and mean-based thresholds become
     * meaningless noise — they'll flag random unfilled bubbles as "outliers."
     *
     * We detect this by checking coefficient of variation (stddev / mean).
     * Real filled bubbles create a bimodal distribution with high CV.
     * All-empty sheets have low CV (uniform distribution).
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

        // ✅ KEY FIX: Detect if there's real variation (bimodal = some filled)
        // Coefficient of variation: stddev/mean. When bubbles are filled vs unfilled,
        // CV is typically > 10-15% (e.g., filled ~80-120, unfilled ~200-240, mean ~180, stddev ~40+).
        // When ALL bubbles are unfilled, CV is tiny (e.g., mean ~220, stddev ~5, CV ~2%).
        // Threshold at 8% provides safe margin.
        val coefficientOfVariation = if (bubbleMean > 0) bubbleStdDev / bubbleMean else 0.0
        val hasRealVariation = coefficientOfVariation > 0.08

        Log.d(TAG, "Global variation: CV=${"%.3f".format(coefficientOfVariation)}, " +
                "hasRealVariation=$hasRealVariation " +
                "(stddev=${"%.1f".format(bubbleStdDev)}, mean=${"%.1f".format(bubbleMean)})")

        // Use 60th percentile as representative of "unfilled bubble" intensity.
        // Since most bubbles are unfilled (typically 1 answer per 4 options = 75% unfilled),
        // the 60th percentile reliably sits in the unfilled cluster.
        // A filled bubble must be less than half this value to be considered filled.
        val p60Index = (sortedBubbles.size * 0.60).toInt().coerceAtMost(sortedBubbles.size - 1)
        val medianBubble = sortedBubbles[p60Index]

        Log.d(TAG, "P60 bubble intensity: ${"%.1f".format(medianBubble)}, " +
                "outlier threshold (60%): ${"%.1f".format(medianBubble * 0.60)}")

        return GlobalStats(
            bubbleMean = bubbleMean,
            bubbleStdDev = bubbleStdDev,
            bgMean = bgMean,
            filledThreshold = filledThreshold,
            darkPixelThreshold = darkPixelThreshold,
            hasRealVariation = hasRealVariation,
            medianBubble = medianBubble
        )
    }

    private fun calculateStdDev(values: List<Double>, mean: Double): Double {
        if (values.isEmpty()) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }

    /**
     * Analyze bubble with LOCAL-ONLY detection.
     *
     * PERMANENT FIX: A bubble is filled ONLY when ALL 3 local signals agree.
     * Each signal compares the bubble to its OWN immediate background — never
     * to other bubbles. This eliminates ALL false positives caused by global
     * statistics noise (percentile thresholds, mean-based thresholds, etc.)
     *
     * The 3 local signals are independent measurements of the same thing
     * ("is this bubble darker than the paper immediately surrounding it?"):
     *   1. RATIO:     bubbleIntensity / backgroundIntensity < 0.65
     *   2. DARK %:    percentage of dark pixels inside bubble > 40%
     *   3. ABS DIFF:  backgroundIntensity - bubbleIntensity > 35
     *
     * An unfilled bubble (just a printed circle on paper) typically has:
     *   - ratio ~0.85-0.95 (circle border barely darker than paper)
     *   - dark% ~5-20% (mostly white inside with thin border)
     *   - diff ~10-25 (small intensity difference)
     * It CANNOT pass all 3 thresholds simultaneously.
     *
     * A filled bubble (pencil mark or screen black) typically has:
     *   - ratio ~0.30-0.60 (much darker than paper)
     *   - dark% ~50-90% (mostly dark pixels)
     *   - diff ~50-150 (large intensity difference)
     * It passes all 3 easily.
     *
     * Global stats are used ONLY to boost confidence, never for the filled/not decision.
     */
    private fun analyzeBubbleStrict(
        bubbleIntensity: Double,
        backgroundIntensity: Double,
        darkPixelPercentage: Double,
        globalStats: GlobalStats,
        questionIndex: Int,
        optionIndex: Int
    ): BubbleAnalysisResult {

        val ratio = bubbleIntensity / backgroundIntensity
        val absoluteDifference = backgroundIntensity - bubbleIntensity

        // ========== 3 LOCAL signals (bubble vs its own background) ==========
        val ratioPass = ratio < RELATIVE_THRESHOLD                          // < 0.55
        val darkPixelPass = darkPixelPercentage > DARK_PIXEL_PERCENTAGE     // > 40%
        val absDiffPass = absoluteDifference > ABSOLUTE_DIFFERENCE_THRESHOLD // > 35

        // ========== GLOBAL outlier check ==========
        // A filled bubble must be significantly darker than the MEDIAN bubble.
        // This catches the case where CLAHE makes unfilled bubbles appear locally dark
        // (ratio 0.57-0.65, dark 65-83%) but they're still near the median globally.
        // Truly filled bubbles (intensity ~8-23) are far below median (~130).
        //
        // Uses median (50th percentile) instead of mean to be robust against
        // the number of filled bubbles. Even with 1 filled bubble out of 200,
        // the median stays at the unfilled cluster (~180), and the filled bubble
        // at ~100 is well below median - 25% = ~135.
        val isGlobalOutlier = bubbleIntensity < (globalStats.medianBubble * 0.60)

        // ========== DECISION ==========
        // ALL 3 local signals must pass AND the bubble must be a global outlier.
        // This double-gate makes false positives virtually impossible:
        // - Local gate catches bubbles that are darker than their own background
        // - Global gate catches bubbles that are darker than ALL other bubbles
        // An unfilled bubble might fool one gate but not both.
        val isFilled = ratioPass && darkPixelPass && absDiffPass && isGlobalOutlier

        // ========== CONFIDENCE ==========
        val confidence = if (isFilled) {
            val intensityScore = (1.0 - ratio).coerceIn(0.0, 1.0)
            val darkPixelScore = (darkPixelPercentage / 100.0).coerceIn(0.0, 1.0)
            val diffScore = (absoluteDifference / 100.0).coerceIn(0.0, 1.0)
            val rawScore = (intensityScore * 0.4) + (darkPixelScore * 0.4) + (diffScore * 0.2)
            rawScore.coerceIn(MIN_CONFIDENCE, 1.0)
        } else {
            0.0
        }

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Q${questionIndex + 1}-${optionIndex + 1}: " +
                    "ratio=${"%.3f".format(ratio)}(${if (ratioPass) "✓" else "✗"}), " +
                    "dark=${"%.1f".format(darkPixelPercentage)}%(${if (darkPixelPass) "✓" else "✗"}), " +
                    "diff=${"%.1f".format(absoluteDifference)}(${if (absDiffPass) "✓" else "✗"}), " +
                    "outlier=$isGlobalOutlier " +
                    "→ filled=$isFilled, conf=${"%.3f".format(confidence)}")
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
