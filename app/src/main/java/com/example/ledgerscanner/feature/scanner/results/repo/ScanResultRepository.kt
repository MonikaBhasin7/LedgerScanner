package com.example.ledgerscanner.feature.scanner.results.repo

import android.content.Context
import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.setStudentDetails
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.QuestionStat
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanResultRepository @Inject constructor(
    private val scanResultDao: ScanResultDao,
    private val examDao: ExamDao,
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager
) {
    suspend fun saveSheet(
        details: StudentDetailsForScanResult,
        scanResultEntity: ScanResultEntity,
    ): Long {
        return scanResultDao.insert(scanResultEntity.setStudentDetails(details.barcodeId))
    }

    // ============ Insert ============

    suspend fun insert(scanResult: ScanResultEntity): Long {
        return scanResultDao.insert(scanResult)
    }

    // ============ Update ============

    suspend fun update(scanResult: ScanResultEntity) {
        scanResultDao.update(scanResult)
    }

    // ============ Delete ============

    suspend fun delete(scanResult: ScanResultEntity) {
        scanResultDao.delete(scanResult)
    }

    suspend fun deleteById(id: Int) {
        scanResultDao.deleteById(id)
    }

    suspend fun deleteAllByExamId(examId: Int) {
        scanResultDao.deleteAllByExamId(examId)
    }

    // ============ Get ============

    fun getAllByExamId(examId: Int): Flow<List<ScanResultEntity>> {
        return scanResultDao.getAllByExamId(examId)
    }

    suspend fun getByIdOnce(id: Int): ScanResultEntity? {
        return scanResultDao.getByIdOnce(id)
    }

    // ============ Count ============

    fun getCountByExamId(examId: Int): Flow<Int> {
        return scanResultDao.getCountByExamId(examId)
    }

    suspend fun getCountByExamIdOnce(examId: Int): Int {
        return scanResultDao.getCountByExamIdOnce(examId)
    }

    // ============ Statistics ============

    fun getStatistics(examId: Int): Flow<ExamStatistics> {
        return scanResultDao.getBasicStatisticsByExamId(examId).map { basicStats ->
            val allResults = scanResultDao.getAllByExamIdOnce(examId)
            val exam = examDao.getExamById(examId)

            if (allResults.isEmpty()) {
                return@map ExamStatistics(
                    avgScore = basicStats.avgScore,
                    topScore = basicStats.topScore,
                    lowestScore = basicStats.lowestScore,
                    sheetsCount = basicStats.sheetsCount
                )
            }

            // Calculate totals
            var totalCorrect = 0
            var totalWrong = 0
            var totalUnanswered = 0

            // Question-wise statistics
            val answerKey = exam?.answerKey.orEmpty()
            val totalQuestions = exam?.totalQuestions
                ?: allResults.maxOfOrNull { it.totalQuestions }
                ?: 0

            // Score distribution
            val scoreDistribution = mutableMapOf(
                "0-25" to 0,
                "26-50" to 0,
                "51-75" to 0,
                "76-100" to 0
            )

            allResults.forEach { result ->
                totalCorrect += result.correctCount
                totalWrong += result.wrongCount
                totalUnanswered += result.blankCount

                // Score distribution
                val boundedPercent = result.scorePercent.toInt().coerceIn(0, 100)
                when (boundedPercent) {
                    in 0..25 -> scoreDistribution["0-25"] = scoreDistribution["0-25"]!! + 1
                    in 26..50 -> scoreDistribution["26-50"] = scoreDistribution["26-50"]!! + 1
                    in 51..75 -> scoreDistribution["51-75"] = scoreDistribution["51-75"]!! + 1
                    in 76..100 -> scoreDistribution["76-100"] = scoreDistribution["76-100"]!! + 1
                }

            }

            val questionStats = buildQuestionStats(
                results = allResults,
                totalQuestions = totalQuestions,
                answerKey = answerKey
            )

            // Calculate median and pass rate
            val medianScore = calculateMedian(allResults.map { it.scorePercent })
            val passRate = (allResults.count { it.scorePercent >= 40f }.toFloat() / allResults.size) * 100

            ExamStatistics(
                avgScore = basicStats.avgScore,
                topScore = basicStats.topScore,
                lowestScore = basicStats.lowestScore,
                sheetsCount = basicStats.sheetsCount,
                totalCorrect = totalCorrect,
                totalWrong = totalWrong,
                totalUnanswered = totalUnanswered,
                questionStats = questionStats,
                scoreDistribution = scoreDistribution,
                medianScore = medianScore,
                passRate = passRate
            )
        }
    }

    private fun calculateMedian(scores: List<Float>): Float {
        if (scores.isEmpty()) return 0f
        val sorted = scores.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2
        } else {
            sorted[size / 2]
        }
    }

    private fun buildQuestionStats(
        results: List<ScanResultEntity>,
        totalQuestions: Int,
        answerKey: Map<Int, Int>
    ): Map<Int, QuestionStat> {
        if (totalQuestions <= 0 || results.isEmpty()) return emptyMap()

        val attemptsByQuestion = mutableMapOf<Int, Int>()
        val correctByQuestion = mutableMapOf<Int, Int>()
        val hasAnswerKey = answerKey.isNotEmpty()

        for (questionNum in 1..totalQuestions) {
            attemptsByQuestion[questionNum] = 0
            correctByQuestion[questionNum] = 0
        }

        results.forEach { result ->
            for (questionNum in 1..totalQuestions) {
                val userAnswers = result.studentAnswers[questionNum].orEmpty()
                if (userAnswers.isEmpty()) continue

                attemptsByQuestion[questionNum] = (attemptsByQuestion[questionNum] ?: 0) + 1

                if (hasAnswerKey) {
                    val correctOption = answerKey[questionNum] ?: continue
                    val hasMultipleMarks = result.multipleMarksDetected?.contains(questionNum) == true
                    val isCorrect = !hasMultipleMarks &&
                            userAnswers.size == 1 &&
                            userAnswers.first() == correctOption
                    if (isCorrect) {
                        correctByQuestion[questionNum] = (correctByQuestion[questionNum] ?: 0) + 1
                    }
                } else {
                    // Fallback when answer key is absent: show attempt-rate so UI is not empty.
                    correctByQuestion[questionNum] = (correctByQuestion[questionNum] ?: 0) + 1
                }
            }
        }

        return (1..totalQuestions)
            .mapNotNull { questionNum ->
                val totalAttempts = attemptsByQuestion[questionNum] ?: 0
                if (totalAttempts == 0) return@mapNotNull null

                val correctCount = correctByQuestion[questionNum] ?: 0
                val correctPercentage = (correctCount.toFloat() / totalAttempts) * 100f

                questionNum to QuestionStat(
                    questionNumber = questionNum,
                    correctCount = correctCount,
                    totalAttempts = totalAttempts,
                    correctPercentage = correctPercentage
                )
            }
            .toMap()
    }

    suspend fun deleteSheet(sheetId: Int) {
        withContext(Dispatchers.IO) {
            scanResultDao.deleteById(sheetId)
        }
    }

    suspend fun deleteMultipleSheets(sheetIds: List<Int>) {
        withContext(Dispatchers.IO) {
            scanResultDao.deleteByIds(sheetIds)
        }
    }
}
