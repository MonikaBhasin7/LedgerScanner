package com.example.ledgerscanner.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY createdAt DESC")
    fun getAllExamsFlow(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: Int): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exams: List<ExamEntity>)

    @Query("DELETE FROM exams")
    suspend fun clearAll()


    @Query("SELECT * FROM exams WHERE status = :status")
    fun getExamByStatus(status: ExamStatus?): Flow<List<ExamEntity>>

    @Query("UPDATE exams SET answerKey = :answerKey WHERE id = :examId")
    suspend fun updateAnswerKey(examId: Int, answerKey: Map<Int, Int>)

    @Query("UPDATE exams SET marksPerCorrect=:marksPerCorrect, marksPerWrong=:marksPerWrong WHERE id = :examId")
    suspend fun updateMarkingScheme(examId: Int, marksPerCorrect: Float?, marksPerWrong: Float?)

    @Query("UPDATE exams SET status = :status WHERE id = :examId")
    suspend fun updateExamStatus(examId: Int, status: ExamStatus)


    @Transaction
    suspend fun updateAnswerKeyAndMarkPending(examId: Int, answerKey: Map<Int, Int>) {
        updateAnswerKey(examId, answerKey)
        markExamPending(examId)
    }

    @Transaction
    suspend fun updateMarkingSchemeAndMarkPending(
        examId: Int,
        marksPerCorrect: Float?,
        marksPerWrong: Float?
    ) {
        updateMarkingScheme(examId, marksPerCorrect, marksPerWrong)
        markExamPending(examId)
    }

    @Transaction
    suspend fun updateExamStatusAndMarkPending(examId: Int, status: ExamStatus) {
        updateExamStatus(examId, status)
        markExamPending(examId)
    }

    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExam(examId: Int)

    @Query("SELECT * FROM exams WHERE syncStatus = 'PENDING'")
    suspend fun getUnsyncedExams(): List<ExamEntity>

    @Query("UPDATE exams SET syncStatus = 'SYNCED' WHERE id = :examId")
    suspend fun markExamSynced(examId: Int)

    @Query("UPDATE exams SET syncStatus = 'PENDING' WHERE id = :examId")
    suspend fun markExamPending(examId: Int)
}
