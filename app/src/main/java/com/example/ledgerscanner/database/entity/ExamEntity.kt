package com.example.ledgerscanner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatus
import java.util.Date

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val title: String? = null,
    val status: ExamStatus? = null,
    val totalQuestions: Int? = null,
    val createdDate: Date? = null,
    val sheetsCount: Int? = null,
    val avgScorePercent: Int? = null,
    val topScorePercent: Int? = null,
    val medianScorePercent: Int? = null
)