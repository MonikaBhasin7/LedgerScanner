package com.example.ledgerscanner.feature.scanner.scan.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.feature.scanner.scan.model.DigitColumn
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reads the enrollment/roll number from the OMR sheet's enrollment grid.
 *
 * The enrollment grid is a set of columns (typically 10), each with 10 bubbles (digits 0-9).
 * Students fill in one bubble per column to encode their enrollment number.
 *
 * Detection approach: For each column, find the darkest bubble. Since exactly one bubble
 * per column should be filled, we use a simple per-column comparison rather than the
 * complex multi-gate detection used for answer bubbles.
 */
class EnrollmentReader @Inject constructor() {

    companion object {
        private const val TAG = "EnrollmentReader"

        // Inner circle ratio for sampling bubble intensity (same as BubbleAnalyzer)
        private const val INNER_CIRCLE_RATIO = 0.60

        // A filled bubble must have intensity ratio < this vs background
        private const val FILL_RATIO_THRESHOLD = 0.70

        // Minimum intensity difference between darkest and second-darkest in a column
        // to confidently say a digit is filled
        private const val MIN_CONFIDENCE_GAP = 15.0
    }

    /**
     * Reads the enrollment number from the warped OMR image.
     *
     * @param template Template containing enrollment grid positions
     * @param warped Warped grayscale Mat (perspective-corrected)
     * @return The enrollment number as a string, or null if no enrollment grid / detection failed
     */
    @WorkerThread
    fun readEnrollmentNumber(
        template: Template,
        warped: Mat
    ): String? {
        val grid = template.enrollment_grid ?: return null

        if (grid.digits.isEmpty()) {
            Log.w(TAG, "Enrollment grid has no digit columns")
            return null
        }

        Log.d(TAG, "Reading enrollment number: ${grid.columns} columns")

        val normalized = normalizeForEnrollment(warped)

        try {
            val digits = mutableListOf<Char>()

            for (column in grid.digits) {
                val digit = readDigitFromColumn(
                    column = column,
                    template = template,
                    normalized = normalized
                )

                if (digit != null) {
                    digits.add(('0' + digit))
                    Log.d(TAG, "Column ${column.column_index}: digit = $digit")
                } else {
                    digits.add('?')
                    Log.w(TAG, "Column ${column.column_index}: no digit detected")
                }
            }

            val result = digits.joinToString("")
            Log.d(TAG, "Enrollment number: $result")
            return result

        } finally {
            normalized.release()
        }
    }

    /**
     * Normalize the warped image for enrollment reading.
     * Light CLAHE + blur — same as BubbleAnalyzer's pipeline.
     */
    private fun normalizeForEnrollment(warped: Mat): Mat {
        val gray = if (warped.channels() == 1) {
            warped.clone()
        } else {
            Mat().also { Imgproc.cvtColor(warped, it, Imgproc.COLOR_BGR2GRAY) }
        }

        val clahe = Imgproc.createCLAHE(2.5, org.opencv.core.Size(8.0, 8.0))
        val enhanced = Mat()
        clahe.apply(gray, enhanced)

        val blurred = Mat()
        Imgproc.GaussianBlur(enhanced, blurred, org.opencv.core.Size(3.0, 3.0), 0.0)

        gray.release()
        enhanced.release()

        return blurred
    }

    /**
     * Reads which digit (0-9) is filled in a single column.
     *
     * Strategy: Sample the average intensity of each bubble in the column.
     * The filled bubble will be significantly darker than the others.
     * Pick the darkest one if it passes the confidence check.
     *
     * @return The digit (0-9) or null if no clear fill detected
     */
    private fun readDigitFromColumn(
        column: DigitColumn,
        template: Template,
        normalized: Mat
    ): Int? {
        val imgCols = normalized.cols()
        val imgRows = normalized.rows()

        // Sample intensity for each bubble in the column
        data class BubbleSample(
            val digitIndex: Int,
            val intensity: Double,
            val backgroundIntensity: Double
        )

        val samples = mutableListOf<BubbleSample>()

        for ((digitIndex, bubble) in column.bubbles.withIndex()) {
            // Convert relative position to absolute
            val cx = (template.anchor_top_left.x + bubble.x).roundToInt().coerceIn(0, imgCols - 1)
            val cy = (template.anchor_top_left.y + bubble.y).roundToInt().coerceIn(0, imgRows - 1)
            val radius = max(bubble.r.roundToInt(), 3)

            val (bubbleIntensity, bgIntensity) = sampleBubbleIntensity(
                normalized, cx, cy, radius, imgCols, imgRows
            )

            samples.add(BubbleSample(digitIndex, bubbleIntensity, bgIntensity))
        }

        if (samples.isEmpty()) return null

        // Find the darkest bubble (lowest intensity = most filled)
        val sorted = samples.sortedBy { it.intensity }
        val darkest = sorted.first()
        val secondDarkest = if (sorted.size > 1) sorted[1] else null

        // Confidence check 1: The darkest must be significantly darker than its background
        val ratio = if (darkest.backgroundIntensity > 0) {
            darkest.intensity / darkest.backgroundIntensity
        } else 1.0

        if (ratio >= FILL_RATIO_THRESHOLD) {
            Log.v(TAG, "Column ${column.column_index}: darkest digit ${darkest.digitIndex} ratio=${"%.2f".format(ratio)} — not filled enough")
            return null
        }

        // Confidence check 2: Clear gap between darkest and second darkest
        if (secondDarkest != null) {
            val gap = secondDarkest.intensity - darkest.intensity
            if (gap < MIN_CONFIDENCE_GAP) {
                Log.v(TAG, "Column ${column.column_index}: ambiguous — gap=${"%.1f".format(gap)} between digit ${darkest.digitIndex} and ${secondDarkest.digitIndex}")
                return null
            }
        }

        return darkest.digitIndex
    }

    /**
     * Sample the average intensity inside a bubble and its background ring.
     * Uses the same ROI-based approach as BubbleAnalyzer for memory efficiency.
     *
     * @return Pair of (bubbleIntensity, backgroundIntensity)
     */
    private fun sampleBubbleIntensity(
        srcMat: Mat,
        cx: Int,
        cy: Int,
        radius: Int,
        imgCols: Int,
        imgRows: Int
    ): Pair<Double, Double> {
        val bgOuterRatio = 1.5
        val roiPad = (radius * bgOuterRatio).roundToInt() + 2
        val roiX = max(0, cx - roiPad)
        val roiY = max(0, cy - roiPad)
        val roiW = min(imgCols - roiX, roiPad * 2)
        val roiH = min(imgRows - roiY, roiPad * 2)

        if (roiW <= 0 || roiH <= 0) return 255.0 to 255.0

        val roiRect = Rect(roiX, roiY, roiW, roiH)
        val roi = Mat(srcMat, roiRect)

        val localCx = cx - roiX
        val localCy = cy - roiY

        var innerMask: Mat? = null
        var outerMask: Mat? = null
        var innerCircleMask: Mat? = null
        var bgMask: Mat? = null

        try {
            val rows = roi.rows()
            val cols = roi.cols()
            val center = Point(localCx.toDouble(), localCy.toDouble())

            val innerRadius = (radius * INNER_CIRCLE_RATIO).roundToInt()
            val bgInnerRadius = radius
            val bgOuterRadius = (radius * bgOuterRatio).roundToInt()

            // Inner bubble mask
            innerMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(innerMask, center, innerRadius, Scalar(255.0), -1)

            // Background ring mask
            outerMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(outerMask, center, bgOuterRadius, Scalar(255.0), -1)

            innerCircleMask = Mat.zeros(rows, cols, roi.type())
            Imgproc.circle(innerCircleMask, center, bgInnerRadius, Scalar(255.0), -1)

            bgMask = Mat()
            Core.subtract(outerMask, innerCircleMask, bgMask)

            val bubbleAvg = Core.mean(roi, innerMask).`val`[0]
            val bgAvg = Core.mean(roi, bgMask).`val`[0]

            return bubbleAvg to bgAvg

        } finally {
            innerMask?.release()
            outerMask?.release()
            innerCircleMask?.release()
            bgMask?.release()
        }
    }
}
