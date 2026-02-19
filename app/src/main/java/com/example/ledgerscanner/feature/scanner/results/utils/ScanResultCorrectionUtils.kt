package com.example.ledgerscanner.feature.scanner.results.utils

import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity

fun ScanResultEntity.updateAnswerAndRecalculate(
    examEntity: ExamEntity,
    questionIndex: Int,
    selectedOption: Int?
): ScanResultEntity {
    if (questionIndex !in 0 until examEntity.totalQuestions) return this

    val updatedAnswers = studentAnswers.toMutableMap()
    val answerKeyToWrite = when {
        updatedAnswers.containsKey(questionIndex) -> questionIndex
        updatedAnswers.containsKey(questionIndex + 1) -> questionIndex + 1
        else -> questionIndex
    }
    updatedAnswers[answerKeyToWrite] = selectedOption?.let(::listOf) ?: emptyList()

    val updatedMultipleMarks = multipleMarksDetected
        ?.filterNot { it == questionIndex || it == questionIndex + 1 }
    val updatedLowConfidence = lowConfidenceQuestions
        ?.filterKeys { it != questionIndex && it != questionIndex + 1 }
    val updatedConfidences = questionConfidences?.toMutableMap()?.apply {
        if (containsKey(questionIndex)) {
            this[questionIndex] = 1.0
        } else if (containsKey(questionIndex + 1)) {
            this[questionIndex + 1] = 1.0
        } else {
            this[questionIndex] = 1.0
        }
    }

    val answerKey = examEntity.answerKey.orEmpty()
    if (answerKey.isEmpty()) {
        return copy(
            studentAnswers = updatedAnswers,
            multipleMarksDetected = updatedMultipleMarks,
            lowConfidenceQuestions = updatedLowConfidence,
            questionConfidences = updatedConfidences
        )
    }

    val marksPerCorrect = examEntity.marksPerCorrect ?: 1f
    val marksPerWrong = (examEntity.marksPerWrong ?: 0f) * -1f

    var correctCount = 0
    var wrongCount = 0
    var blankCount = 0
    var score = 0f

    for (index in 0 until examEntity.totalQuestions) {
        val answers = updatedAnswers[index] ?: updatedAnswers[index + 1] ?: emptyList()
        if (answers.isEmpty()) {
            blankCount++
            continue
        }

        val hasMultiple = updatedMultipleMarks?.contains(index) == true ||
            updatedMultipleMarks?.contains(index + 1) == true ||
            answers.size > 1

        if (hasMultiple) {
            wrongCount++
            score += marksPerWrong
            continue
        }

        val correctAnswer = answerKey[index] ?: answerKey[index + 1] ?: -1
        if (correctAnswer != -1 && answers.first() == correctAnswer) {
            correctCount++
            score += marksPerCorrect
        } else {
            wrongCount++
            score += marksPerWrong
        }
    }

    val maxMarks = examEntity.totalQuestions * marksPerCorrect
    val scorePercent = if (maxMarks > 0f) (score / maxMarks) * 100f else 0f

    return copy(
        studentAnswers = updatedAnswers,
        multipleMarksDetected = updatedMultipleMarks,
        questionConfidences = updatedConfidences,
        lowConfidenceQuestions = updatedLowConfidence,
        score = score,
        correctCount = correctCount,
        wrongCount = wrongCount,
        blankCount = blankCount,
        scorePercent = scorePercent
    )
}
