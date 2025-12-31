package com.example.ledgerscanner.feature.scanner.exam.model

data class ExamStatistics(
    val avgScore: Float? = null,
    val topScore: Float? = null,
    val lowestScore: Float? = null,
    val medianScore: Float? = null,
    val passRate: Float? = null,
    val sheetsCount: Int = 0
) {
    fun hasStats(): Boolean {
        return sheetsCount > 0
    }

    private fun isValidScore(score: Float?): Boolean {
        return score != null && score in 0f..100f && !score.isNaN() && !score.isInfinite()
    }

    fun getValidAvgScore(): String {
        return if (hasStats() && isValidScore(avgScore)) {
            "${avgScore!!.toInt()}%"
        } else "--"
    }

    fun getValidTopScore(): String {
        return if (hasStats() && isValidScore(topScore)) {
            "${topScore!!.toInt()}%"
        } else "--"
    }

    fun getValidLowestScore(): String {
        return if (hasStats() && isValidScore(lowestScore)) {
            "${lowestScore!!.toInt()}%"
        } else "--"
    }

    fun getValidMedianScore(): String {
        return if (hasStats() && isValidScore(medianScore)) {
            "${medianScore!!.toInt()}%"
        } else "--"
    }

    fun getValidPassRate(): String {
        return if (hasStats() && isValidScore(passRate)) {
            "${passRate!!.toInt()}%"
        } else "--"
    }
}