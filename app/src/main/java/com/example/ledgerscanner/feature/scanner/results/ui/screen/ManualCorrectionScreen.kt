package com.example.ledgerscanner.feature.scanner.results.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.getAnswersForQuestionIndex
import com.example.ledgerscanner.database.entity.getQuestionConfidence

@Composable
fun ManualCorrectionScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    questionIndicesToReview: List<Int>,
    scanResult: ScanResultEntity
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val currentQuestion = questionIndicesToReview[currentQuestionIndex]
    
    Scaffold(
        topBar = {
            GenericToolbar(
                title = "Review Question ${currentQuestionIndex + 1}/${questionIndicesToReview.size}",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentQuestionIndex + 1f) / questionIndicesToReview.size },
                modifier = Modifier.fillMaxWidth()
            )

            // Question preview with detected answer
            QuestionReviewCard(
                questionIndex = currentQuestion,
                detectedAnswer = scanResult.getAnswersForQuestionIndex(currentQuestion),
                confidence = scanResult.getQuestionConfidence(currentQuestion),
                correctAnswer = examEntity.answerKey?.get(currentQuestion)
                    ?: examEntity.answerKey?.get(currentQuestion + 1),
                onAnswerChange = { newAnswer ->
                    // Update the answer
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentQuestionIndex > 0) {
                    GenericButton(
                        text = "Previous",
                        onClick = { currentQuestionIndex-- },
                        type = ButtonType.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                }

                GenericButton(
                    text = if (currentQuestionIndex < questionIndicesToReview.size - 1) "Next" else "Done",
                    onClick = {
                        if (currentQuestionIndex < questionIndicesToReview.size - 1) {
                            currentQuestionIndex++
                        } else {
                            // Save changes and go back
                            navController.popBackStack()
                        }
                    },
                    type = ButtonType.PRIMARY,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun QuestionReviewCard(
    questionIndex: Int,
    detectedAnswer: List<Int>?,
    confidence: Double?,
    correctAnswer: Int?,
    onAnswerChange: (Int?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Question ${questionIndex + 1}",
                style = AppTypography.text20Bold,
                color = Grey900
            )

            // Show detected answer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Detected:",
                    style = AppTypography.text14Regular,
                    color = Grey700
                )
                Text(
                    text = detectedAnswer?.joinToString(", ") { "${('A' + it)}" } ?: "No answer",
                    style = AppTypography.text14Bold,
                    color = if (detectedAnswer.isNullOrEmpty()) Grey500 else Grey900
                )
                confidence?.let {
                    Text(
                        text = "(${(it * 100).toInt()}%)",
                        style = AppTypography.text13Regular,
                        color = when {
                            it > 0.7 -> Green600
                            it > 0.5 -> Orange600
                            else -> Red600
                        }
                    )
                }
            }

            // Show bubble preview/image here
            // TODO: Show cropped image of the question bubbles

            Divider()

            // Option buttons to change answer
            Text(
                text = "Select correct answer:",
                style = AppTypography.text14SemiBold,
                color = Grey800
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(0, 1, 2, 3).forEach { optionIndex ->
                    OutlinedButton(
                        onClick = { onAnswerChange(optionIndex) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (detectedAnswer?.contains(optionIndex) == true) 
                                Blue50 else Color.Transparent
                        )
                    ) {
                        Text(
                            text = "${('A' + optionIndex)}",
                            style = AppTypography.text16Bold
                        )
                    }
                }
            }

            // Option to mark as blank
            TextButton(
                onClick = { onAnswerChange(null) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Mark as Blank/Unanswered",
                    color = Grey600
                )
            }
        }
    }
}
