package com.example.ledgerscanner.feature.scanner.exam.domain.model

import com.example.ledgerscanner.base.extensions.toCleanString

data class BasicExamStatistics(
    val avgScore: Float? = null,
    val topScore: Float? = null,
    val lowestScore: Float? = null,
    val sheetsCount: Int = 0
)

data class ExamStatistics(
    val avgScore: Float? = null,
    val topScore: Float? = null,
    val lowestScore: Float? = null,
    val medianScore: Float? = null,
    val passRate: Float? = null,
    val sheetsCount: Int = 0,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val totalUnanswered: Int = 0,
    val firstScannedAt: Long? = null,
    val lastScannedAt: Long? = null,
    val questionStats: Map<Int, QuestionStat> = emptyMap(),
    val scoreDistribution: Map<String, Int> = emptyMap()
) {
    fun hasStats(): Boolean {
        return sheetsCount > 0
    }

    private fun isValidScore(score: Float?): Boolean {
        return score != null && !score.isNaN() && !score.isInfinite()
    }

    fun getValidAvgScore(): String {
        return if (hasStats() && isValidScore(avgScore)) {
            "${avgScore!!.toCleanString()}%"
        } else "--"
    }

    fun getValidTopScore(): String {
        return if (hasStats() && isValidScore(topScore)) {
            "${topScore!!.toCleanString()}%"
        } else "--"
    }

    fun getValidLowestScore(): String {
        return if (hasStats() && isValidScore(lowestScore)) {
            "${lowestScore!!.toCleanString()}%"
        } else "--"
    }

    fun getValidMedianScore(): String {
        return if (hasStats() && isValidScore(medianScore)) {
            "${medianScore!!.toCleanString()}%"
        } else "--"
    }

    fun getValidPassRate(): String {
        return if (hasStats() && isValidScore(passRate)) {
            "${passRate!!.toCleanString()}%"
        } else "--"
    }
}

data class QuestionStat(
    val questionNumber: Int,
    val correctCount: Int,
    val totalAttempts: Int,
    val correctPercentage: Float
)
