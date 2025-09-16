package com.example.ledgerscanner.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: String,
    val totalQuestions: Int,
    val createdDate: String,
    val sheetsCount: Int,
    val avgScorePercent: Int,
    val topScorePercent: Int,
    val medianScorePercent: Int
)