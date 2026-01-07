package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange500
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.getQuestionStatus
import com.example.ledgerscanner.database.entity.isQuestionAttempted
import com.example.ledgerscanner.feature.scanner.results.model.AnswerStatus

@Composable
fun QuestionDetailsSection(
    scanResultEntity: ScanResultEntity,
    examEntity: ExamEntity,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(1.dp),
        onClick = onToggle
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question Details",
                    style = AppTypography.label2Bold,
                    color = Grey900
                )

                Icon(
                    imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp
                    else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Grey600
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Get sorted list of questions
                val allQuestions = scanResultEntity.studentAnswers.entries
                    .sortedBy { it.key }
                    .toList()

                val itemsToShow = if (expanded) {
                    allQuestions
                } else {
                    allQuestions.take(3)
                }

                itemsToShow.forEach { (questionIndex, userAnswers) ->
                    QuestionDetailItem(
                        questionIndex = questionIndex,
                        userAnswers = userAnswers,
                        correctAnswer = examEntity.answerKey?.get(questionIndex) ?: -1,
                        scanResultEntity = scanResultEntity
                    )
                }

                if (!expanded && allQuestions.size > 3) {
                    Text(
                        text = "Tap to view ${allQuestions.size - 3} more...",
                        style = AppTypography.body3Regular,
                        color = Blue500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun QuestionDetailItem(
    questionIndex: Int,
    userAnswers: List<Int>,
    correctAnswer: Int,
    scanResultEntity: ScanResultEntity
) {
    // ✅ Get status using extension function
    val status = scanResultEntity.getQuestionStatus(questionIndex, correctAnswer)

    val (backgroundColor, textColor, icon) = when (status) {
        AnswerStatus.CORRECT -> Triple(Green500.copy(alpha = 0.1f), Green500, "✓")
        AnswerStatus.INCORRECT -> Triple(Red500.copy(alpha = 0.1f), Red500, "✗")
        AnswerStatus.UNANSWERED -> Triple(Grey200, Grey600, "—")
        AnswerStatus.MULTIPLE_MARKS -> Triple(Orange500.copy(alpha = 0.1f), Orange500, "⚠")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = icon, style = AppTypography.body2Medium, color = textColor)
            Text(
                text = "Q${questionIndex + 1}",
                style = AppTypography.body3Medium,
                color = Grey900
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ✅ Display user answers
            Text(
                text = if (scanResultEntity.isQuestionAttempted(questionIndex)) {
                    userAnswers.joinToString(", ") { getOptionLetter(it) }
                } else {
                    "Not answered"
                },
                style = AppTypography.body3Regular,
                color = if (scanResultEntity.isQuestionAttempted(questionIndex)) textColor else Grey600
            )

            // ✅ Show correct answer if user is wrong
            if (correctAnswer != -1 && status == AnswerStatus.INCORRECT) {
                Text(
                    text = "→ ${getOptionLetter(correctAnswer)}",
                    style = AppTypography.body3Regular,
                    color = Green500
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

private fun getOptionLetter(index: Int): String = ('A' + index).toString()