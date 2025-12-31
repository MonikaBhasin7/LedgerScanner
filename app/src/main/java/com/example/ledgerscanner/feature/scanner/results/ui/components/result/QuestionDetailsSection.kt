package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.feature.scanner.results.model.*

@Composable
fun QuestionDetailsSection(
    evaluation: EvaluationResult?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    evaluation?.let {
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
                    val itemsToShow = if (expanded) {
                        evaluation.answerMap.entries.toList()
                    } else {
                        evaluation.answerMap.entries.take(3)
                    }

                    itemsToShow.forEach { (questionNumber, answerModel) ->
                        QuestionDetailItem(questionNumber, answerModel)
                    }

                    if (!expanded && evaluation.answerMap.size > 3) {
                        Text(
                            text = "Tap to view ${evaluation.answerMap.size - 3} more...",
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
}

@Composable
private fun QuestionDetailItem(
    questionNumber: Int,
    answerModel: AnswerModel
) {
    val status = answerModel.getStatus()
    
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
            Text(text = "Q${questionNumber + 1}", style = AppTypography.body3Medium, color = Grey900)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (answerModel.isAttempted()) {
                    answerModel.userSelected.joinToString(", ") { getOptionLetter(it) }
                } else {
                    "Not answered"
                },
                style = AppTypography.body3Regular,
                color = if (answerModel.isAttempted()) textColor else Grey600
            )

            if (answerModel.correctAnswer != -1 && !answerModel.isCorrect()) {
                Text(
                    text = "→ ${getOptionLetter(answerModel.correctAnswer)}",
                    style = AppTypography.body3Regular,
                    color = Green500
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

private fun getOptionLetter(index: Int): String = ('A' + index).toString()