package com.example.ledgerscanner.feature.scanner.scan.model

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

data class BubbleAnalysisResult(
    val isFilled: Boolean,
    val confidence: Double,
    val bubbleAvgIntensity: Double,
    val backgroundAvgIntensity: Double,
    val darkPixelPercentage: Double
)