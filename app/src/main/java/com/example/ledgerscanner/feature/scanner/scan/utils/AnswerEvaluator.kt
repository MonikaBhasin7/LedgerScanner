package com.example.ledgerscanner.feature.scanner.scan.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.AnswerModel
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import javax.inject.Inject

class AnswerEvaluator @Inject constructor() {

    companion object {
        private const val TAG = "AnswerEvaluator"
    }

    /**
     * Evaluates detected bubbles against exam answer key with marking scheme
     *
     * @param detectedBubbles List of filled bubbles from detection
     * @param examEntity Exam containing answer key and marking scheme
     * @return Evaluation result with correctness marks, score, and statistics
     */
    @WorkerThread
    fun evaluateAnswers(
        detectedBubbles: List<BubbleResult>,
        examEntity: ExamEntity
    ): EvaluationResult {
        val totalQuestions = examEntity.totalQuestions
        val answerKey = examEntity.answerKey ?: emptyMap()
        val marksPerCorrect = examEntity.marksPerCorrect ?: 1f
        val marksPerWrong = (examEntity.marksPerWrong ?: 0f) * -1

        // Initialize answer map for all questions
        val answerMap = mutableMapOf<Int, AnswerModel>()
        for (i in 0 until totalQuestions) { // FIXED: was 0 until totalQuestions - 1
            answerMap[i] = AnswerModel(
                correctAnswer = answerKey[i] ?: answerKey[i + 1] ?: -1
            )
        }

        // Populate user selections from detected bubbles
        detectedBubbles.forEach { bubble ->
            answerMap[bubble.questionIndex]?.userSelected?.add(bubble.optionIndex)
        }

        // Evaluation counters
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        var marksObtained = 0f
        val multipleMarksQuestions = mutableMapOf<Int, List<Int>>()
        val correctnessMarks = MutableList(totalQuestions) { false }

        // Evaluate each question
        answerMap.forEach { (questionIndex, answerModel) ->
            when {
                // Case 1: Unanswered (no bubbles filled)
                answerModel.userSelected.isEmpty() -> {
                    unansweredCount++
                    correctnessMarks[questionIndex] = false
                    // FIXED: Unanswered questions don't get negative marking
                    // marksObtained stays the same (0 marks)
                }

                // Case 2: Multiple answers (more than one bubble filled)
                answerModel.userSelected.size > 1 -> {
                    multipleMarksQuestions[questionIndex] = answerModel.userSelected.toList()
                    incorrectCount++
                    marksObtained += marksPerWrong // Apply negative marking
                    correctnessMarks[questionIndex] = false
                }

                // Case 3: Single answer
                else -> {
                    val userAnswer = answerModel.userSelected.first()
                    val isCorrect = answerModel.correctAnswer == userAnswer

                    if (isCorrect) {
                        correctCount++
                        marksObtained += marksPerCorrect
                        correctnessMarks[questionIndex] = true
                    } else {
                        incorrectCount++
                        marksObtained += marksPerWrong
                        correctnessMarks[questionIndex] = false
                    }
                }
            }
        }

        // Calculate max marks and percentage
        // BUG FIX: Allow negative percentage when negative marking is enabled
        // Previously clamped to 0-100, hiding true score when marks go negative
        val maxMarks = totalQuestions * marksPerCorrect
        val percentage = if (maxMarks > 0) {
            (marksObtained / maxMarks) * 100
        } else {
            0f
        }

        Log.d(TAG, "Evaluation Complete:")
        Log.d(TAG, "  Correct: $correctCount/$totalQuestions")
        Log.d(TAG, "  Incorrect: $incorrectCount")
        Log.d(TAG, "  Unanswered: $unansweredCount")
        Log.d(TAG, "  Multiple Marks: ${multipleMarksQuestions.size}")
        Log.d(TAG, "  Marks: $marksObtained/$maxMarks (${"%.2f".format(percentage)}%)")

        return EvaluationResult(
            correctnessMarks = correctnessMarks,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            multipleMarksQuestions = multipleMarksQuestions.keys.toList(),
            totalQuestions = totalQuestions,
            marksObtained = marksObtained,
            maxMarks = maxMarks,
            percentage = percentage,
            answerMap = answerMap // Store for detailed review
        )
    }
}
