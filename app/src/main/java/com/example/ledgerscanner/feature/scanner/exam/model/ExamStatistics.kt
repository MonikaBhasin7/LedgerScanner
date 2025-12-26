package com.example.ledgerscanner.feature.scanner.exam.model

data class ExamStatistics(
    val avgScore: Float? = null,
    val topScore: Float? = null,
    val lowestScore: Float? = null,
    val sheetsCount: Int? = null,
) {
    fun hasStats(): Boolean {
        return avgScore != null ||
                topScore != null ||
                lowestScore != null
    }
}