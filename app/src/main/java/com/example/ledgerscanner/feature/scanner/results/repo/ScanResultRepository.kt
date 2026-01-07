package com.example.ledgerscanner.feature.scanner.results.repo

import android.content.Context
import com.example.ledgerscanner.database.dao.ScanResultDao
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.setStudentDetails
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
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