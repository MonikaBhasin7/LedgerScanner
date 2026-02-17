package com.example.ledgerscanner.feature.scanner.exam.data.repository

import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.SyncStatus
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExamRepository @Inject constructor(
    private val dao: ExamDao,
    private val syncManager: SyncManager
) {
    fun getAllExams(): Flow<List<ExamEntity>> = dao.getAllExamsFlow()

    suspend fun getExam(id: Int): ExamEntity? = dao.getExamById(id)

    suspend fun getExamByStatus(status: ExamStatus?): Flow<List<ExamEntity>> =
        dao.getExamByStatus(status)

    suspend fun saveExam(exam: ExamEntity): Long {
        val examToSave = exam.copy(syncStatus = SyncStatus.PENDING)
        val result = if (examToSave.id == 0) {
            dao.insertExam(examToSave)
        } else {
            dao.updateExam(examToSave)
            examToSave.id.toLong()
        }
        syncManager.scheduleImmediateSync()
        return result
    }

    suspend fun saveExams(exams: List<ExamEntity>) = dao.upsertAll(exams)

    suspend fun clear() = dao.clearAll()
    suspend fun saveBasicInfo(
        examName: String,
        description: String?,
        template: Template,
        numberOfQuestions: Int,
        existingExam: ExamEntity? = null,
    ): ExamEntity {
        if (existingExam != null) {
            val didTemplateOrQuestionCountChange =
                existingExam.template != template || existingExam.totalQuestions != numberOfQuestions
            val updated = existingExam.copy(
                examName = examName,
                description = description,
                status = if (
                    didTemplateOrQuestionCountChange &&
                    existingExam.status == ExamStatus.ACTIVE
                ) {
                    ExamStatus.DRAFT
                } else {
                    existingExam.status
                },
                template = template,
                totalQuestions = numberOfQuestions,
                answerKey = if (didTemplateOrQuestionCountChange) null else existingExam.answerKey,
                marksPerCorrect = if (didTemplateOrQuestionCountChange) null else existingExam.marksPerCorrect,
                marksPerWrong = if (didTemplateOrQuestionCountChange) null else existingExam.marksPerWrong,
                syncStatus = SyncStatus.PENDING,
            )
            dao.updateExam(updated)
            syncManager.scheduleImmediateSync()
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
        syncManager.scheduleImmediateSync()
        return exam.copy(id = rowId.toInt())
    }

    suspend fun saveAnswerKey(
        examEntity: ExamEntity,
        answerKeys: Map<Int, Int>,
    ): ExamEntity {
        val updatedEntity = examEntity.copy(
            answerKey = answerKeys,
        )

        dao.updateAnswerKeyAndMarkPending(examEntity.id, answerKeys)
        syncManager.scheduleImmediateSync()

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

        dao.updateMarkingSchemeAndMarkPending(
            examEntity.id,
            updatedEntity.marksPerCorrect,
            updatedEntity.marksPerWrong
        )
        syncManager.scheduleImmediateSync()

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

    suspend fun updateExamStatus(examId: Int, status: ExamStatus) = withContext(Dispatchers.IO) {
        dao.updateExamStatusAndMarkPending(examId, status)
        syncManager.scheduleImmediateSync()
    }
}
