package com.example.ledgerscanner.feature.scanner.results.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import kotlinx.parcelize.Parcelize
import org.opencv.core.Mat

data class OmrDetectionResult(
    val bubbles: List<BubbleResult> // Just the detected filled bubbles
)

@Parcelize
data class BubbleResult(
    val point: AnchorPoint,
    val questionIndex: Int,      // Which question (0-based)
    val optionIndex: Int,         // Which option (0-based)
    val confidence: Double = 1.0, // Detection confidence (0.0-1.0)
) : Parcelable

@Parcelize
data class EvaluationResult(
    val correctnessMarks: List<Boolean>,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val multipleMarksQuestions: List<Int>,
    val totalQuestions: Int,
    val marksObtained: Float,
    val maxMarks: Float,
    val percentage: Float,
    val answerMap: Map<Int, AnswerModel> = emptyMap() // For detailed review
) : Parcelable {
    /**
     * Get pass/fail status
     */
    fun isPassed(passingPercentage: Float = 40f): Boolean {
        return percentage >= passingPercentage
    }

    /**
     * Get grade based on percentage
     */
    fun getGrade(): String {
        return when {
            percentage >= 90 -> "A+"
            percentage >= 80 -> "A"
            percentage >= 70 -> "B"
            percentage >= 60 -> "C"
            percentage >= 50 -> "D"
            percentage >= 40 -> "E"
            else -> "F"
        }
    }

    /**
     * Get formatted marks string (e.g., "85.5/100")
     */
    fun getMarksFormatted(): String {
        return "${if (marksObtained % 1 == 0f) marksObtained.toInt() else marksObtained}/" +
                "${if (maxMarks % 1 == 0f) maxMarks.toInt() else maxMarks}"
    }

    /**
     * Get formatted percentage string (e.g., "85.50%")
     */
    fun getPercentageFormatted(): String {
        return "${"%.2f".format(percentage)}%"
    }

    /**
     * Get attempted questions count
     */
    fun getAttemptedCount(): Int {
        return correctCount + incorrectCount
    }
}

@Parcelize
data class AnswerModel(
    var userSelected: MutableList<Int> = mutableListOf(),
    var correctAnswer: Int = -1 // -1 means no answer key available
) : Parcelable {
    /**
     * Check if answer is correct
     */
    fun isCorrect(): Boolean {
        return userSelected.size == 1 && userSelected.first() == correctAnswer
    }

    /**
     * Check if question is attempted
     */
    fun isAttempted(): Boolean {
        return userSelected.isNotEmpty()
    }

    /**
     * Check if multiple answers are marked
     */
    fun hasMultipleMarks(): Boolean {
        return userSelected.size > 1
    }

    /**
     * Get status for display
     */
    fun getStatus(): AnswerStatus {
        return when {
            !isAttempted() -> AnswerStatus.UNANSWERED
            hasMultipleMarks() -> AnswerStatus.MULTIPLE_MARKS
            isCorrect() -> AnswerStatus.CORRECT
            else -> AnswerStatus.INCORRECT
        }
    }
}

enum class AnswerStatus {
    CORRECT,
    INCORRECT,
    UNANSWERED,
    MULTIPLE_MARKS
}

data class AnchorDetectionResult(
    val success: Boolean,
    val centers: List<AnchorPoint>
)

/**
 * Context object to manage Mat resources and debug bitmaps throughout processing.
 * Ensures proper cleanup in finally block.
 */
class OmrProcessingContext {
    var grayMat: Mat? = null
    var warpedMat: Mat? = null
    val debugBitmaps = mutableMapOf<String, Bitmap>()

    var rawBitmap: Bitmap? = null

    fun release() {
        grayMat?.release()
        warpedMat?.release()
    }
}

data class StudentDetailsForScanResult(
    val name: String?,
    val rollNumber: Int?,
    val barcodeId: String?
)