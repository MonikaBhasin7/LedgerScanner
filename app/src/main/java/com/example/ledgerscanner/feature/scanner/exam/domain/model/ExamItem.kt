package com.example.ledgerscanner.feature.scanner.exam.domain.model

import android.os.Parcelable
import com.example.ledgerscanner.database.entity.ExamEntity
import kotlinx.parcelize.Parcelize

@Parcelize
enum class ExamStatus : Parcelable {
    DRAFT,      // template created, not scanned yet
    ACTIVE,     // currently scanning / running
    COMPLETED,  // all sheets scanned, stats available
    ARCHIVED    // no longer used
}

data class ExamDraft(
    val examId: Long?,             // null before insert
    val examName: String,
    val description: String?,
    val numberOfQuestions: Int,
    val templateSummary: TemplateSummary? = null // light-weight, avoid images
)

data class TemplateSummary(
    val name: String?,
    val sheetWidth: Double,
    val sheetHeight: Double,
    val optionsPerQuestion: Int
)