package com.example.ledgerscanner.database.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ledgerscanner.feature.scanner.results.model.AnswerStatus
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "scan_results",
    foreignKeys = [
        ForeignKey(
            entity = ExamEntity::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    val examId: Int,

    // Student Info
    var barCode: String?,

    // Scan Info
    val clickedRawImagePath: String,
    val scannedImagePath: String,
    val thumbnailPath: String? = null,
    val debugImagesPath: Map<String, String>? = null,
    val scannedAt: Long = System.currentTimeMillis(),

    // ✅ ALL ANSWERS IN ONE MAP
    val studentAnswers: Map<Int, List<Int>>, // {1: 0, 2: 1, 3: 2, ...}

    // ✅ ISSUES IN ONE MAP (optional)
    val multipleMarksDetected: List<Int>? = null, // [7, 12, 23] - question numbers with multiple marks

    // Score Summary
    val score: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val blankCount: Int,
    val scorePercent: Float,

    // NEW: Confidence tracking
    val questionConfidences: Map<Int, Double?>? = null,
    val avgConfidence: Double? = null,
    val minConfidence: Double? = null,
    val lowConfidenceQuestions: Map<Int, Double?>? = null
) : Parcelable

fun ScanResultEntity.setStudentDetails(barCode: String?): ScanResultEntity {
    if (barCode != null) {
        this.barCode = barCode
    }
    return this
}

/**
 * Get count of questions with multiple marks detected
 */
fun ScanResultEntity.getMultipleMarksDetectedCount(): Int {
    return multipleMarksDetected?.size ?: 0
}

/**
 * Get count of questions with low confidence
 */
fun ScanResultEntity.getLowConfidenceCount(): Int {
    return lowConfidenceQuestions?.size ?: 0
}

/**
 * Check if a specific question was attempted
 */
fun ScanResultEntity.isQuestionAttempted(questionIndex: Int): Boolean {
    return studentAnswers[questionIndex]?.isNotEmpty() == true
}

/**
 * Check if a specific question has multiple marks
 */
fun ScanResultEntity.hasQuestionMultipleMarks(questionIndex: Int): Boolean {
    return multipleMarksDetected?.contains(questionIndex) == true
}

/**
 * Check if a specific question is correct
 * Requires ExamEntity to compare with correct answer
 */
fun ScanResultEntity.isQuestionCorrect(questionIndex: Int, correctAnswer: Int): Boolean {
    val userAnswers = studentAnswers[questionIndex] ?: return false
    return userAnswers.size == 1 && userAnswers.first() == correctAnswer
}

/**
 * Get answer status for a specific question
 * Requires ExamEntity to get correct answer
 */
fun ScanResultEntity.getQuestionStatus(questionIndex: Int, correctAnswer: Int): AnswerStatus {
    val userAnswers = studentAnswers[questionIndex] ?: emptyList()

    return when {
        userAnswers.isEmpty() -> AnswerStatus.UNANSWERED
        hasQuestionMultipleMarks(questionIndex) -> AnswerStatus.MULTIPLE_MARKS
        isQuestionCorrect(questionIndex, correctAnswer) -> AnswerStatus.CORRECT
        else -> AnswerStatus.INCORRECT
    }
}

/**
 * Check if the scan result needs review
 */
fun ScanResultEntity.needsReview(): Boolean {
    return getMultipleMarksDetectedCount() > 0 || getLowConfidenceCount() > 0
}

/**
 * Get confidence for a specific question
 */
fun ScanResultEntity.getQuestionConfidence(questionIndex: Int): Double? {
    return questionConfidences?.get(questionIndex)
}

/**
 * Check if a question has low confidence
 */
fun ScanResultEntity.hasLowConfidence(questionIndex: Int): Boolean {
    return lowConfidenceQuestions?.contains(questionIndex) == true
}