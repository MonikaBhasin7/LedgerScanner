package com.example.ledgerscanner.feature.scanner.exam.repo

import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExamRepository @Inject constructor(private val dao: ExamDao) {
    fun getAllExams(): Flow<List<ExamEntity>> = dao.getAllExamsFlow()

    suspend fun getExam(id: Int): ExamEntity? = dao.getExamById(id)

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
        existingExam: ExamEntity? = null,
    ): ExamEntity {
        if (existingExam != null) {
            val updated = existingExam.copy(
                examName = examName,
                description = description,
                template = template,
                totalQuestions = numberOfQuestions,
            )
            dao.insertExam(updated)
            return updated
        }

        val now = System.currentTimeMillis()
        val exam = ExamEntity(
            id = 0,
            examName = examName,
            description = description,
            status = ExamStatus.DRAFT,
            totalQuestions = numberOfQuestions,
            template = template,
            createdAt = now,
        )

        val rowId: Long = dao.insertExam(exam)
        return exam.copy(id = rowId.toInt())
    }

    suspend fun saveAnswerKey(
        examEntity: ExamEntity,
        answerKeys: Map<Int, Int>,
    ): ExamEntity {
        val updatedEntity = examEntity.copy(
            answerKey = answerKeys,
        )

        dao.updateAnswerKey(examEntity.id, answerKeys)

        return updatedEntity
    }

    suspend fun saveMarkingScheme(
        examEntity: ExamEntity,
        marksPerCorrect: Float,
        marksPerWrong: Float,
        negativeMarking: Boolean
    ): ExamEntity {
        val updatedEntity = examEntity.copy(
            marksPerCorrect = marksPerCorrect,
            marksPerWrong = if (negativeMarking) marksPerWrong else 0f
        )

        dao.updateMarkingScheme(
            examEntity.id,
            updatedEntity.marksPerCorrect,
            updatedEntity.marksPerWrong
        )

        return updatedEntity
    }

    suspend fun deleteExam(examId: Int) = withContext(Dispatchers.IO) {
        dao.deleteExam(examId)
    }

    suspend fun duplicateExam(examEntity: ExamEntity) = withContext(Dispatchers.IO) {
        val newExamEntity = examEntity.copy(
            id = 0,
            examName = "${examEntity.examName} (Copy)",
            createdAt = System.currentTimeMillis()
        )
        dao.insertExam(newExamEntity)
    }
}