package com.example.ledgerscanner.feature.scanner.scan.model

data class OmrDetectionResult(
    val bubbles: List<BubbleResult>,
    val marks: List<Boolean>
)

data class BubbleResult(
    val point: AnchorPoint,
    val isCorrect: Boolean
)