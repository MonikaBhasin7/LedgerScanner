package com.example.ledgerscanner.feature.scanner.scan.repo

import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanResultRepository @Inject constructor(private val scanResultDao: ScanResultDao) {

    // Insert
    suspend fun insert(scanResult: ScanResultEntity): Long {
        return scanResultDao.insert(scanResult)
    }

    // Update
    suspend fun update(scanResult: ScanResultEntity) {
        scanResultDao.update(scanResult)
    }

    // Delete
    suspend fun delete(scanResult: ScanResultEntity) {
        scanResultDao.delete(scanResult)
    }

    // Get all by exam
    fun getAllByExamId(examId: Int): Flow<List<ScanResultEntity>> {
        return scanResultDao.getAllByExamId(examId)
    }

    // Get count
    fun getCountByExamId(examId: Int): Flow<Int> {
        return scanResultDao.getCountByExamId(examId)
    }

    // Get statistics
    suspend fun getStatistics(examId: Int): ExamStatistics {
        return ExamStatistics(
            avgScore = scanResultDao.getAverageScore(examId) ?: 0f,
            topScore = scanResultDao.getTopScore(examId) ?: 0f,
            lowestScore = scanResultDao.getLowestScore(examId) ?: 0f,
            medianScore = scanResultDao.getMedianScore(examId) ?: 0f,
            passRate = scanResultDao.getPassRate(examId) ?: 0f
        )
    }
}