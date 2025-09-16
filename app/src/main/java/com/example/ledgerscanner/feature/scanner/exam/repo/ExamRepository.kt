package com.example.ledgerscanner.feature.scanner.exam.repo

import com.example.ledgerscanner.database.dao.ExamDao
import com.example.ledgerscanner.database.entity.ExamEntity
import kotlinx.coroutines.flow.Flow

class ExamRepository(private val dao: ExamDao) {
    fun observeExams(): Flow<List<ExamEntity>> = dao.getAllExamsFlow()

    suspend fun getExam(id: String): ExamEntity? = dao.getExamById(id)

    suspend fun saveExam(exam: ExamEntity) = dao.insertExam(exam)

    suspend fun saveExams(exams: List<ExamEntity>) = dao.insertAll(exams)

    suspend fun clear() = dao.clearAll()
}