package com.example.ledgerscanner.models

data class ExamItem(
    val id: String,                // unique exam ID
    val title: String,             // e.g. "Physics Midterm"
    val status: ExamStatus,        // e.g. Processing, Completed, Draft
    val totalQuestions: Int,       // e.g. 50
    val createdDate: String,       // e.g. "Apr 12"
    val sheetsCount: Int,          // e.g. 124
    val avgScorePercent: Int,      // e.g. 71
    val topScorePercent: Int,      // e.g. 96
    val medianScorePercent: Int    // e.g. 68
)

enum class ExamStatus {
    Processing,
    Completed,
    Draft
}