package com.example.ledgerscanner.feature.scanner.exam.repo

import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
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
}