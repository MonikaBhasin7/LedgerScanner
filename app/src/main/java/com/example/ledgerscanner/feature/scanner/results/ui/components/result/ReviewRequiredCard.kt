package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange200
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange500
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Orange700
import com.example.ledgerscanner.base.ui.theme.Orange800
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.getAnswersForQuestionIndex
import com.example.ledgerscanner.database.entity.getQuestionConfidence

private const val LOW_CONFIDENCE_WARNING_THRESHOLD = 0.70

@Composable
fun ReviewRequiredCard(
    scanResultEntity: ScanResultEntity,
    examEntity: ExamEntity,
    originalScanResultEntity: ScanResultEntity = scanResultEntity,
    onAnswerChange: (questionIndex: Int, selectedOption: Int?) -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val questionsToReview = scanResultEntity.getLowConfidenceQuestionIndices(
        totalQuestions = examEntity.totalQuestions
    )
    if (questionsToReview.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Orange50),
        border = BorderStroke(1.dp, Orange200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Orange600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Review Low Confidence Answers",
                        style = AppTypography.text16Bold,
                        color = Orange800
                    )
                }
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Orange200
                ) {
                    Text(
                        text = "${questionsToReview.size}",
                        style = AppTypography.text12Bold,
                        color = Orange800,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = if (isReadOnly) {
                    "${questionsToReview.size} question${if (questionsToReview.size > 1) "s" else ""} flagged for manual review."
                } else {
                    "${questionsToReview.size} question${if (questionsToReview.size > 1) "s" else ""} need manual review. Tap option to fix."
                },
                style = AppTypography.text14Regular,
                color = Grey800,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            questionsToReview.forEach { questionIndex ->
                val selectedAnswers = scanResultEntity.getAnswersForQuestionIndex(questionIndex)
                val originalDetectedAnswers =
                    originalScanResultEntity.getAnswersForQuestionIndex(questionIndex)
                val confidence = scanResultEntity.getQuestionConfidence(questionIndex)
                val correctAnswer = examEntity.answerKey?.get(questionIndex)
                    ?: examEntity.answerKey?.get(questionIndex + 1)

                QuestionReviewItem(
                    questionIndex = questionIndex,
                    optionCount = examEntity.template.options_per_question,
                    selectedAnswers = selectedAnswers,
                    originalDetectedAnswers = originalDetectedAnswers,
                    correctAnswer = correctAnswer,
                    confidence = confidence,
                    isReadOnly = isReadOnly,
                    onAnswerSelected = { selected ->
                        if (!isReadOnly) {
                            onAnswerChange(questionIndex, selected)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionReviewItem(
    questionIndex: Int,
    optionCount: Int,
    selectedAnswers: List<Int>,
    originalDetectedAnswers: List<Int>,
    correctAnswer: Int?,
    confidence: Double?,
    isReadOnly: Boolean,
    onAnswerSelected: (Int?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
//        border = BorderStroke(1.dp, Grey200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q${questionIndex + 1}",
                    style = AppTypography.text16Bold,
                    color = Grey900
                )
                confidence?.let {
                    Text(
                        text = "Confidence ${(it * 100).toInt()}%",
                        style = AppTypography.text12Medium,
                        color = if (it < LOW_CONFIDENCE_WARNING_THRESHOLD) Orange700 else Grey500
                    )
                }
            }

            confidence?.let {
                LinearProgressIndicator(
                    progress = { it.coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (it < LOW_CONFIDENCE_WARNING_THRESHOLD) Orange500 else Green500,
                    trackColor = Grey200
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(optionCount) { option ->
                    val isSelected = selectedAnswers.contains(option)
                    val isCorrectSelection = isSelected && correctAnswer == option
                    OptionBubble(
                        text = ('A' + option).toString(),
                        selected = isSelected,
                        containerColor = when {
                            isCorrectSelection -> Green500
                            isSelected -> Orange500
                            else -> White
                        },
                        borderColor = when {
                            originalDetectedAnswers.contains(option) -> Orange500
                            isCorrectSelection -> Green500
                            isSelected -> Orange500
                            else -> Grey200
                        },
                        enabled = !isReadOnly,
                        onClick = { onAnswerSelected(option) }
                    )
                }
                OptionBubble(
                    text = "-",
                    selected = selectedAnswers.isEmpty(),
                    containerColor = if (selectedAnswers.isEmpty()) Orange500 else White,
                    borderColor = if (originalDetectedAnswers.isEmpty()) Orange500 else Grey200,
                    enabled = !isReadOnly,
                    onClick = { onAnswerSelected(null) }
                )
            }

            if (selectedAnswers.size > 1) {
                Text(
                    text = "Multiple marks detected: ${selectedAnswers.joinToString(", ") { ('A' + it).toString() }}",
                    style = AppTypography.text14SemiBold,
                    color = Red500
                )
            }

            if (correctAnswer != null && correctAnswer >= 0) {
                Text(
                    text = "Correct Answer: ${('A' + correctAnswer)}",
                    style = AppTypography.text13Medium,
                    color = Grey500
                )
            }
        }
    }
}

@Composable
private fun OptionBubble(
    text: String,
    selected: Boolean,
    containerColor: Color,
    borderColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp = 36.dp
) {
    Surface(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = AppTypography.text14Bold,
                color = if (selected) White else Grey900
            )
        }
    }
}

private fun ScanResultEntity.getLowConfidenceQuestionIndices(
    totalQuestions: Int
): List<Int> {
    val keys = lowConfidenceQuestions?.keys?.toList().orEmpty()
    if (keys.isEmpty()) return emptyList()
    val zeroBased = keys.any { it == 0 }

    return keys.mapNotNull { raw ->
        val candidate = if (zeroBased) raw else raw - 1
        candidate.takeIf { it in 0 until totalQuestions }
    }.distinct().sorted()
}
