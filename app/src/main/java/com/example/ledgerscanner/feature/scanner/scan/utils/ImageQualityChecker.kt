package com.example.ledgerscanner.feature.scanner.scan.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessHistogram
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessQualityReport
import com.example.ledgerscanner.feature.scanner.scan.model.QualityCheck
import com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

class ImageQualityChecker @Inject constructor() {

    companion object {
        private const val TAG = "ImageQualityChecker"

        // Histogram clipping thresholds
        private const val CLIPPING_THRESHOLD = 5.0  // % of pixels at extremes
        private const val NEAR_BLACK_THRESHOLD = 30 // Pixel value threshold for "too dark"
        private const val NEAR_WHITE_THRESHOLD = 225 // Pixel value threshold for "too bright"
    }

    /**
     * Checks brightness quality of the image
     *
     * @param image Grayscale Mat to analyze
     * @param analyzeHistogram Whether to perform detailed histogram analysis
     * @return BrightnessQualityReport with assessment and suggestions
     */
    @WorkerThread
    fun checkBrightness(
        image: Mat,
        analyzeHistogram: Boolean = true
    ): BrightnessQualityReport {
        if (image.empty()) {
            Log.w(TAG, "Empty Mat provided to checkBrightness")
            return BrightnessQualityReport(
                brightnessCheck = QualityCheck(
                    level = QualityLevel.FAILED,
                    value = 0.0,
                    suggestion = "Image is empty or invalid"
                )
            )
        }

        var grayMat: Mat? = null

        try {
            // Convert to grayscale if needed
            grayMat = if (image.channels() == 1) {
                image
            } else {
                Mat().also {
                    Imgproc.cvtColor(image, it, Imgproc.COLOR_BGR2GRAY)
                }
            }

            // Step 1: Calculate average brightness
            val meanScalar = Core.mean(grayMat)
            val avgBrightness = meanScalar.`val`[0]

            Log.d(TAG, "========== BRIGHTNESS CHECK ==========")
            Log.d(TAG, "Average brightness: $avgBrightness / 255")

            // Step 2: Determine quality level based on brightness
            val (level, suggestion) = determineBrightnessQuality(avgBrightness)

            Log.d(TAG, "Quality level: $level")
            Log.d(TAG, "Suggestion: $suggestion")

            // Step 3: Analyze histogram if requested
            val histogram = if (analyzeHistogram) {
                analyzeHistogram(grayMat)
            } else {
                null
            }

            // Step 4: Adjust quality if clipping detected
            val finalLevel =
                if (histogram?.hasClipping == true && level > QualityLevel.ACCEPTABLE) {
                    Log.d(TAG, "Downgrading quality due to clipping")
                    QualityLevel.ACCEPTABLE
                } else {
                    level
                }

            val finalSuggestion = when {
                histogram?.hasClipping == true && histogram.tooBlackPercentage > histogram.tooWhitePercentage -> {
                    Log.d(TAG, "Shadow clipping detected: ${histogram.tooBlackPercentage}%")
                    "Image has shadow clipping - move to brighter area"
                }

                histogram?.hasClipping == true && histogram.tooWhitePercentage > histogram.tooBlackPercentage -> {
                    Log.d(TAG, "Highlight clipping detected: ${histogram.tooWhitePercentage}%")
                    "Image has highlight clipping - reduce lighting or avoid direct light"
                }

                else -> suggestion
            }

            if (histogram != null) {
                Log.d(
                    TAG, "Histogram - Black: ${histogram.tooBlackPercentage.toInt()}%, " +
                            "White: ${histogram.tooWhitePercentage.toInt()}%, Clipping: ${histogram.hasClipping}"
                )
            }

            Log.d(TAG, "Final quality: $finalLevel")
            Log.d(TAG, "=====================================")

            return BrightnessQualityReport(
                brightnessCheck = QualityCheck(
                    level = finalLevel,
                    value = avgBrightness,
                    suggestion = finalSuggestion
                ),
                histogram = histogram
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error checking brightness", e)
            return BrightnessQualityReport(
                brightnessCheck = QualityCheck(
                    level = QualityLevel.FAILED,
                    value = 0.0,
                    suggestion = "Failed to analyze brightness: ${e.message}"
                )
            )
        } finally {
            if (grayMat !== image) {
                grayMat?.release()
            }
        }
    }

    /**
     * Determines brightness quality level and provides actionable suggestion
     *
     * Scale (0-255):
     * 0-60:     FAILED (too dark)
     * 60-80:    POOR (very dark)
     * 80-100:   ACCEPTABLE (slightly dark)
     * 100-120:  GOOD (good lighting)
     * 120-160:  EXCELLENT (ideal lighting)
     * 160-180:  GOOD (good lighting)
     * 180-200:  ACCEPTABLE (slightly bright)
     * 200-220:  POOR (very bright)
     * 220-255:  FAILED (too bright)
     */
    private fun determineBrightnessQuality(brightness: Double): Pair<QualityLevel, String?> {
        return when {
            // FAILED - Extremely dark (0-60)
            brightness < 60.0 -> {
                QualityLevel.FAILED to "Image is extremely dark - cannot process. Please ensure adequate lighting"
            }

            // POOR - Very dark (60-80)
            brightness < 80.0 -> {
                QualityLevel.POOR to "Image is too dark - move to brighter area or use flash"
            }

            // ACCEPTABLE - Slightly dark (80-100)
            brightness < 100.0 -> {
                QualityLevel.ACCEPTABLE to "Image is slightly dark - consider improving lighting"
            }

            // GOOD - Good lighting (100-120)
            brightness < 120.0 -> {
                QualityLevel.GOOD to null
            }

            // EXCELLENT - Ideal lighting (120-160)
            brightness <= 160.0 -> {
                QualityLevel.EXCELLENT to null
            }

            // GOOD - Good lighting (160-180)
            brightness <= 180.0 -> {
                QualityLevel.GOOD to null
            }

            // ACCEPTABLE - Slightly bright (180-200)
            brightness <= 200.0 -> {
                QualityLevel.ACCEPTABLE to "Image is slightly bright - consider reducing lighting"
            }

            // POOR - Very bright (200-220)
            brightness <= 220.0 -> {
                QualityLevel.POOR to "Image is too bright - reduce lighting or move away from direct light"
            }

            // FAILED - Extremely bright (220-255)
            else -> {
                QualityLevel.FAILED to "Image is extremely bright - cannot process. Please reduce lighting"
            }
        }
    }

    /**
     * Analyzes histogram to detect clipping and distribution issues
     */
    @WorkerThread
    private fun analyzeHistogram(grayMat: Mat): BrightnessHistogram {
        var histogram: Mat? = null

        try {
            // Calculate histogram
            histogram = Mat()
            val histSize = MatOfInt(256)
            val ranges = MatOfFloat(0f, 256f)

            Imgproc.calcHist(
                listOf(grayMat),
                MatOfInt(0),
                Mat(),
                histogram,
                histSize,
                ranges
            )

            val totalPixels = (grayMat.rows() * grayMat.cols()).toDouble()

            // Count pixels in extreme ranges
            var tooBlackCount = 0.0
            var tooWhiteCount = 0.0

            for (i in 0 until NEAR_BLACK_THRESHOLD) {
                tooBlackCount += histogram.get(i, 0)[0]
            }

            for (i in NEAR_WHITE_THRESHOLD until 256) {
                tooWhiteCount += histogram.get(i, 0)[0]
            }

            val tooBlackPercentage = (tooBlackCount / totalPixels) * 100.0
            val tooWhitePercentage = (tooWhiteCount / totalPixels) * 100.0

            // NEW: Check for narrow distribution (auto-exposure indicator)
            var pixelsInMidRange = 0.0
            for (i in 60 until 140) {  // Mid-range
                pixelsInMidRange += histogram.get(i, 0)[0]
            }
            val midRangePercentage = (pixelsInMidRange / totalPixels) * 100.0

            // If >70% of pixels are in narrow mid-range, likely auto-exposed dark scene
            val isNarrowDistribution = midRangePercentage > 70.0

            val hasClipping = tooBlackPercentage > CLIPPING_THRESHOLD ||
                    tooWhitePercentage > CLIPPING_THRESHOLD ||
                    isNarrowDistribution

            Log.d(TAG, "Histogram - Black: ${tooBlackPercentage.toInt()}%, " +
                    "White: ${tooWhitePercentage.toInt()}%, " +
                    "MidRange: ${midRangePercentage.toInt()}%, " +
                    "Narrow: $isNarrowDistribution")

            return BrightnessHistogram(
                tooBlackPercentage = tooBlackPercentage,
                tooWhitePercentage = tooWhitePercentage,
                hasClipping = hasClipping
            )

        } finally {
            histogram?.release()
        }
    }
}