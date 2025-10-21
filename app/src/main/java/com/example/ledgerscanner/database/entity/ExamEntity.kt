package com.example.ledgerscanner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val examName: String,
    val status: ExamStatus,
    val totalQuestions: Int,
    val template: Template,
    val answerKey: Map<Int, String>? = null,
    val marksPerCorrect: Float? = null,
    val marksPerWrong: Float? = null,
    val createdAt: Long,
    val sheetsCount: Int? = null,
    val avgScorePercent: Int? = null,
    val topScorePercent: Int? = null,
    val medianScorePercent: Int? = null
)