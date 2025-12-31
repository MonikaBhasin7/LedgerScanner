package com.example.ledgerscanner.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {

    // ============ Basic CRUD ============

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scanResult: ScanResultEntity): Long

    @Update
    suspend fun update(scanResult: ScanResultEntity)

    @Delete
    suspend fun delete(scanResult: ScanResultEntity)

    @Query("DELETE FROM scan_results WHERE examId = :examId")
    suspend fun deleteAllByExamId(examId: Int)

    @Query("DELETE FROM scan_results WHERE id = :sheetId")
    suspend fun deleteById(sheetId: Int)

    @Query("DELETE FROM scan_results WHERE id IN (:sheetIds)")
    suspend fun deleteByIds(sheetIds: List<Int>)

    // ============ Single Item ============

    @Query("SELECT * FROM scan_results WHERE id = :id")
    fun getById(id: Int): Flow<ScanResultEntity?>

    @Query("SELECT * FROM scan_results WHERE id = :id")
    suspend fun getByIdOnce(id: Int): ScanResultEntity?

    // ============ List by Exam ============

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scannedAt DESC")
    fun getAllByExamId(examId: Int): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scannedAt DESC")
    suspend fun getAllByExamIdOnce(examId: Int): List<ScanResultEntity>

    // ============ Count ============

    @Query("SELECT COUNT(*) FROM scan_results WHERE examId = :examId")
    fun getCountByExamId(examId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE examId = :examId")
    suspend fun getCountByExamIdOnce(examId: Int): Int

    // ============ Statistics ============

    @Query("SELECT AVG(scorePercent) FROM scan_results WHERE examId = :examId")
    suspend fun getAverageScore(examId: Int): Float?

    @Query("SELECT MAX(scorePercent) FROM scan_results WHERE examId = :examId")
    suspend fun getTopScore(examId: Int): Float?

    @Query("SELECT MIN(scorePercent) FROM scan_results WHERE examId = :examId")
    suspend fun getLowestScore(examId: Int): Float?

    @Query(
        """
        SELECT AVG(scorePercent) 
        FROM (
            SELECT scorePercent 
            FROM scan_results 
            WHERE examId = :examId 
            ORDER BY scorePercent 
            LIMIT 2 - (SELECT COUNT(*) FROM scan_results WHERE examId = :examId) % 2 
            OFFSET (SELECT (COUNT(*) - 1) / 2 FROM scan_results WHERE examId = :examId)
        )
    """
    )
    suspend fun getMedianScore(examId: Int): Float?

    @Query(
        """
        SELECT COUNT(*) * 100.0 / (SELECT COUNT(*) FROM scan_results WHERE examId = :examId)
        FROM scan_results 
        WHERE examId = :examId AND scorePercent >= :passingPercent
    """
    )
    suspend fun getPassRate(examId: Int, passingPercent: Float = 40f): Float?

    // ============ Sorting ============

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scorePercent DESC")
    fun getOrderedByScore(examId: Int): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE examId = :examId ORDER BY scannedAt DESC")
    fun getOrderedByDate(examId: Int): Flow<List<ScanResultEntity>>

    @Query(
        """
        SELECT 
            COALESCE(AVG(scorePercent), 0) as avgScore,
            COALESCE(MAX(scorePercent), 0) as topScore,
            COALESCE(MIN(scorePercent), 0) as lowestScore,
            COUNT(*) as sheetsCount
        FROM scan_results 
        WHERE examId = :examId
    """
    )
    fun getStatisticsByExamId(examId: Int): Flow<ExamStatistics>
}