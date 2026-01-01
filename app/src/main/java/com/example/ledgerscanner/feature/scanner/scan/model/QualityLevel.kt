package com.example.ledgerscanner.feature.scanner.scan.model

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

/**
 * Quality level for image assessment
 */
enum class QualityLevel {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
    FAILED;

    fun toIcon(): String = when (this) {
        EXCELLENT, GOOD -> "✓"
        ACCEPTABLE -> "⚠"
        POOR, FAILED -> "✗"
    }

    fun toColor(): androidx.compose.ui.graphics.Color = when (this) {
        EXCELLENT, GOOD -> com.example.ledgerscanner.base.ui.theme.Green600
        ACCEPTABLE -> com.example.ledgerscanner.base.ui.theme.Orange500
        POOR, FAILED -> com.example.ledgerscanner.base.ui.theme.Red600
    }
}

/**
 * Individual quality check result with measured value and suggestion
 */
data class QualityCheck(
    val level: QualityLevel,
    val value: Double,           // Actual measured value
    val suggestion: String? = null // What to do if poor/failed
)

/**
 * Brightness quality assessment result
 */
data class BrightnessQualityReport(
    val brightnessCheck: QualityCheck,
    val histogram: BrightnessHistogram? = null
)

/**
 * Histogram data for brightness distribution
 */
data class BrightnessHistogram(
    val tooBlackPercentage: Double,   // % of pixels that are pure black or near-black
    val tooWhitePercentage: Double,   // % of pixels that are pure white or near-white
    val hasClipping: Boolean          // True if significant clipping detected
)