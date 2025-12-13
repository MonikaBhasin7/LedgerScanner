package com.example.ledgerscanner.feature.scanner.exam.repo

import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ExamRepository @Inject constructor(private val dao: ExamDao) {
    fun getAllExams(): Flow<List<ExamEntity>> = dao.getAllExamsFlow()

    suspend fun getExam(id: String): ExamEntity? = dao.getExamById(id)

    suspend fun getExamByStatus(status: ExamStatus?): Flow<List<ExamEntity>> =
        dao.getExamByStatus(status)

    suspend fun saveExam(exam: ExamEntity) = dao.insertExam(exam)

    suspend fun saveExams(exams: List<ExamEntity>) = dao.insertAll(exams)

    suspend fun clear() = dao.clearAll()
    suspend fun saveBasicInfo(
        examName: String,
        description: String?,
        template: Template,
        numberOfQuestions: Int,
        saveInDb: Boolean
    ): ExamEntity {
        val now = System.currentTimeMillis()
        val exam = ExamEntity(
            id = 0,
            examName = examName,
            status = ExamStatus.DRAFT,
            totalQuestions = numberOfQuestions,
            template = template,
            createdAt = now,
        )

        return if (saveInDb) {
            val rowId: Long = dao.insertExam(exam)
            val newId = if (rowId >= 0L) rowId.toInt() else 0
            exam.copy(id = newId)
        } else {
            exam
        }
    }

    suspend fun saveAnswerKey(
        examEntity: ExamEntity,
        answerKeys: Map<Int, Int>,
        saveInDb: Boolean
    ): ExamEntity {
        val updatedEntity = examEntity.copy(
            answerKey = answerKeys,
            status = if (saveInDb) ExamStatus.DRAFT else examEntity.status
        )

        if (saveInDb) {
            dao.updateAnswerKey(examEntity.id, answerKeys)
        }

        return updatedEntity
    }
}