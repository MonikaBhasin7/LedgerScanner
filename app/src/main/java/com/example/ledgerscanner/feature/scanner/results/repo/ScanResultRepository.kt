package com.example.ledgerscanner.feature.scanner.results.repo

import android.content.Context
import com.example.ledgerscanner.base.utils.StorageUtils
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.utils.BubbleAnalyzer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ScanResultRepository @Inject constructor(
    private val scanResultDao: ScanResultDao,
    @ApplicationContext private val context: Context
) {
    suspend fun saveSheet(
        details: StudentDetailsForScanResult,
        omrImageProcessResult: OmrImageProcessResult,
        examId: Int
    ): Long {
        return withContext(Dispatchers.IO) {
            // Validate
            require(omrImageProcessResult.success) { "Invalid scan result" }
            val evaluation = omrImageProcessResult.evaluation
                ?: throw IllegalStateException("No evaluation data available")
            val detectedBubbles = omrImageProcessResult.detectedBubbles
                ?: throw IllegalStateException("No detected bubbles available")

            // Save images
            val scannedImagePath = omrImageProcessResult.finalBitmap?.let { bitmap ->
                StorageUtils.saveImageToStorage(
                    context,
                    bitmap,
                    "scan_${System.currentTimeMillis()}.jpg"
                )
            } ?: throw IllegalStateException("No scanned image available")

            val thumbnailPath = omrImageProcessResult.finalBitmap.let { bitmap ->
                val thumbnail = StorageUtils.createThumbnail(bitmap, 300, 400)
                StorageUtils.saveImageToStorage(
                    context,
                    thumbnail,
                    "thumb_${System.currentTimeMillis()}.jpg"
                )
            }

            // Convert detected bubbles to answers
            val studentAnswers = mutableMapOf<Int, List<Int>>()

            evaluation.answerMap.forEach { (key, item) ->
                studentAnswers[key] = item.userSelected.toList()
            }

            val questionConfidences = mutableMapOf<Int, Double>()
            detectedBubbles.groupBy { it.questionIndex }.forEach { (qIndex, bubbles) ->
                // For each question, store the highest confidence (if multiple marks)
                // or the single confidence value
                val maxConfidence = bubbles.maxOfOrNull { it.confidence } ?: 0.0
                questionConfidences[qIndex] = maxConfidence
            }

            val allConfidences = detectedBubbles.map { it.confidence }
            val avgConfidence = if (allConfidences.isNotEmpty()) {
                allConfidences.average()
            } else null

            val minConfidence = allConfidences.minOrNull()

            val lowConfidenceQuestions = questionConfidences
                .filter { it.value < BubbleAnalyzer.MIN_CONFIDENCE }
                .keys
                .toList()

            // Create entity
            val scanResult = ScanResultEntity(
                examId = examId,
                barCode = details.barcodeId,
                scannedImagePath = scannedImagePath,
                thumbnailPath = thumbnailPath,
                scannedAt = System.currentTimeMillis(),
                studentAnswers = studentAnswers,
                multipleMarksDetected = evaluation.multipleMarksQuestions,
                score = evaluation.marksObtained.toInt(),
                totalQuestions = evaluation.totalQuestions,
                correctCount = evaluation.correctCount,
                wrongCount = evaluation.incorrectCount,
                blankCount = evaluation.unansweredCount,
                scorePercent = evaluation.percentage,
                questionConfidences = questionConfidences,
                avgConfidence = avgConfidence,
                minConfidence = minConfidence,
                lowConfidenceQuestions = lowConfidenceQuestions
            )

            // Insert
            scanResultDao.insert(scanResult)
        }
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

    suspend fun getStatistics(examId: Int): Flow<ExamStatistics> {
        return scanResultDao.getStatisticsByExamId(examId)
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