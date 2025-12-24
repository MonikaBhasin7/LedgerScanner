package com.example.ledgerscanner.feature.scanner.scan.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.ToolbarAction
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey300
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange500
import com.example.ledgerscanner.base.ui.theme.Orange700
import com.example.ledgerscanner.base.ui.theme.Orange800
import com.example.ledgerscanner.base.ui.theme.Orange900
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ExamEntity

enum class QuestionStatus {
    CORRECT, WRONG, BLANK, LOW_CONFIDENCE
}

data class QuestionResult(
    val questionNumber: Int,
    val selectedOption: Int?, // 0=A, 1=B, 2=C, 3=D, null=blank
    val correctOption: Int,
    val status: QuestionStatus,
    val confidence: Float = 1.0f
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen1(
    navController: NavHostController,
    examEntity: ExamEntity,
    sheetNumber: Int = 6,
    studentName: String = "John Doe",
    rollNumber: String = "12345",
    score: Int = 42,
    totalQuestions: Int = 50,
    correctAnswers: Int = 42,
    wrongAnswers: Int = 6,
    blankAnswers: Int = 2,
    lowConfidenceQuestions: List<QuestionResult> = emptyList(),
    allQuestions: List<QuestionResult> = emptyList(),
    onSaveAndContinue: () -> Unit = {},
    onRetryScan: () -> Unit = {},
    onScanNext: () -> Unit = {},
    onViewAllSheets: () -> Unit = {},
    onReviewEdit: () -> Unit = {}
) {
    val handleBack = rememberBackHandler(navController)
    val scorePercent = if (totalQuestions > 0) (score * 100) / totalQuestions else 0

    var questionDetailsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GenericToolbar(
                title = "Scan Result #$sheetNumber",
                onBackClick = handleBack,
                actions = listOf(
                    ToolbarAction.Icon(
                        icon = Icons.Outlined.MoreVert,
                        contentDescription = "Menu",
                        onClick = { /* Show menu */ }
                    )
                )
            )
        },
        containerColor = Grey100
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // OMR Sheet Preview
            OmrSheetPreview1(
                questions = allQuestions.take(5),
                optionsPerQuestion = examEntity.template.options_per_question
            )

            Spacer(Modifier.height(16.dp))

            // Student Details
            StudentDetailsSection1(
                studentName = studentName,
                rollNumber = rollNumber
            )

            Spacer(Modifier.height(16.dp))

            // Score Summary
            ScoreSummaryCard1(
                score = score,
                totalQuestions = totalQuestions,
                scorePercent = scorePercent,
                correctAnswers = correctAnswers,
                wrongAnswers = wrongAnswers,
                blankAnswers = blankAnswers
            )

            Spacer(Modifier.height(16.dp))

            // Review Required Warning (if low confidence exists)
            if (lowConfidenceQuestions.isNotEmpty()) {
                ReviewRequiredCard(
                    lowConfidenceQuestions = lowConfidenceQuestions,
                    onReviewEdit = onReviewEdit
                )
                Spacer(Modifier.height(16.dp))
            }

            // Question Details
            QuestionDetailsSection(
                questions = allQuestions,
                expanded = questionDetailsExpanded,
                onToggle = { questionDetailsExpanded = !questionDetailsExpanded }
            )

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            ActionButtonsSection(
                onSaveAndContinue = onSaveAndContinue,
                onRetryScan = onRetryScan,
                onScanNext = onScanNext,
                onViewAllSheets = onViewAllSheets,
                totalSheets = 5
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OmrSheetPreview1(
    questions: List<QuestionResult>,
    optionsPerQuestion: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Grey200),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sample questions preview
            questions.forEach { question ->
                OmrQuestionRow(
                    questionNumber = question.questionNumber,
                    selectedOption = question.selectedOption,
                    correctOption = question.correctOption,
                    status = question.status,
                    totalOptions = optionsPerQuestion
                )
                if (question != questions.last()) {
                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Tap to zoom",
                style = AppTypography.body3Regular,
                color = Grey600
            )
        }
    }
}

@Composable
private fun OmrQuestionRow(
    questionNumber: Int,
    selectedOption: Int?,
    correctOption: Int,
    status: QuestionStatus,
    totalOptions: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Q$questionNumber",
            style = AppTypography.label2Bold,
            color = Grey900,
            modifier = Modifier.width(40.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(totalOptions) { index ->
                val optionLetter = ('A' + index).toString()
                OmrBubble(
                    letter = optionLetter,
                    isSelected = selectedOption == index,
                    isCorrect = correctOption == index,
                    status = if (selectedOption == index) status else QuestionStatus.BLANK
                )
            }
        }
    }
}

@Composable
private fun OmrBubble(
    letter: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    status: QuestionStatus
) {
    val (bgColor, borderColor, textColor, icon) = when {
        !isSelected && !isCorrect -> {
            // Unselected bubble
            Tuple4(White, Grey400, Grey700, null)
        }

        status == QuestionStatus.CORRECT -> {
            // Correct answer
            Tuple4(Green500, Green500, White, "✓")
        }

        status == QuestionStatus.WRONG -> {
            // Wrong answer
            Tuple4(Red500, Red500, White, "✗")
        }

        status == QuestionStatus.LOW_CONFIDENCE -> {
            // Low confidence
            Tuple4(Orange500, Orange500, White, "!")
        }

        else -> {
            Tuple4(White, Grey400, Grey700, null)
        }
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Text(
                text = icon,
                style = AppTypography.label3Bold,
                color = textColor
            )
        } else {
            Text(
                text = letter,
                style = AppTypography.body3Regular,
                color = textColor
            )
        }
    }
}

@Composable
private fun StudentDetailsSection1(
    studentName: String,
    rollNumber: String
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Student Details",
            style = AppTypography.label2Bold,
            color = Grey900
        )

        Spacer(Modifier.height(12.dp))

        // Name field
        OutlinedTextField(
            value = studentName,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.body2Regular,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = Blue500,
                unfocusedBorderColor = Grey300
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(12.dp))

        // Roll number field
        OutlinedTextField(
            value = rollNumber,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTypography.body2Regular,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = White,
                unfocusedContainerColor = White,
                focusedBorderColor = Blue500,
                unfocusedBorderColor = Grey300
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun ScoreSummaryCard1(
    score: Int,
    totalQuestions: Int,
    scorePercent: Int,
    correctAnswers: Int,
    wrongAnswers: Int,
    blankAnswers: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Blue75),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large score
            Text(
                text = "$score/$totalQuestions",
                style = AppTypography.display2Bold,
                color = Black
            )

            Text(
                text = "($scorePercent%)",
                style = AppTypography.h4Medium,
                color = Blue500
            )

            Spacer(Modifier.height(16.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreStat(
                    icon = "✓",
                    count = correctAnswers,
                    label = "Correct",
                    color = Green500
                )

                ScoreStat(
                    icon = "✗",
                    count = wrongAnswers,
                    label = "Wrong",
                    color = Red500
                )

                ScoreStat(
                    icon = "—",
                    count = blankAnswers,
                    label = "Blank",
                    color = Grey600
                )
            }
        }
    }
}

@Composable
private fun ScoreStat(
    icon: String,
    count: Int,
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            style = AppTypography.label2Bold,
            color = color
        )
        Text(
            text = count.toString(),
            style = AppTypography.label2Bold,
            color = color
        )
        Text(
            text = label,
            style = AppTypography.body3Regular,
            color = Grey700
        )
    }
}

@Composable
private fun ReviewRequiredCard(
    lowConfidenceQuestions: List<QuestionResult>,
    onReviewEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, Orange500, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Orange50),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠️",
                    style = AppTypography.h4Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Review Required",
                    style = AppTypography.label2Bold,
                    color = Orange900
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "${lowConfidenceQuestions.size} questions have low confidence detection",
                style = AppTypography.body3Regular,
                color = Orange900
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = lowConfidenceQuestions.joinToString(", ") {
                    "Q${it.questionNumber} (${(it.confidence * 100).toInt()}%)"
                },
                style = AppTypography.body3Regular,
                color = Orange800
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Review & Edit →",
                style = AppTypography.label3Bold,
                color = Orange700,
                modifier = Modifier.genericClick { onReviewEdit() }
            )
        }
    }
}

@Composable
private fun QuestionDetailsSection(
    questions: List<QuestionResult>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        onClick = onToggle
    ) {
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
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = Grey600
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                questions.forEach { question ->
                    QuestionDetailItem(question)
                    if (question != questions.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Grey200
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun QuestionDetailItem(question: QuestionResult) {
    val optionLetter = { index: Int -> ('A' + index).toString() }
    val statusText = when (question.status) {
        QuestionStatus.CORRECT -> "Correct"
        QuestionStatus.WRONG -> "Wrong"
        QuestionStatus.BLANK -> "Blank"
        QuestionStatus.LOW_CONFIDENCE -> "Low Confidence"
    }
    val statusColor = when (question.status) {
        QuestionStatus.CORRECT -> Green500
        QuestionStatus.WRONG -> Red500
        QuestionStatus.BLANK -> Grey600
        QuestionStatus.LOW_CONFIDENCE -> Orange500
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Q${question.questionNumber}",
            style = AppTypography.body2Medium,
            color = Grey900,
            modifier = Modifier.width(40.dp)
        )

        Text(
            text = when (question.status) {
                QuestionStatus.CORRECT -> "✓"
                QuestionStatus.WRONG -> "✗"
                else -> ""
            },
            style = AppTypography.label2Bold,
            color = statusColor
        )

        Text(
            text = "${question.selectedOption?.let { optionLetter(it) } ?: "—"} → ${
                optionLetter(
                    question.correctOption
                )
            }",
            style = AppTypography.body3Regular,
            color = Grey700,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text = statusText,
            style = AppTypography.body3Medium,
            color = statusColor
        )
    }
}

@Composable
private fun ActionButtonsSection(
    onSaveAndContinue: () -> Unit,
    onRetryScan: () -> Unit,
    onScanNext: () -> Unit,
    onViewAllSheets: () -> Unit,
    totalSheets: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Primary button
        GenericButton(
            text = "Save & Continue",
            onClick = onSaveAndContinue,
            size = ButtonSize.LARGE,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // Secondary buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GenericButton(
                text = "🔄 Retry Scan",
                onClick = onRetryScan,
                type = ButtonType.SECONDARY,
                size = ButtonSize.MEDIUM,
                modifier = Modifier.weight(1f)
            )

            GenericButton(
                text = "📷 Scan Next",
                onClick = onScanNext,
                type = ButtonType.SECONDARY,
                size = ButtonSize.MEDIUM,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Link
        Text(
            text = "View All Scanned Sheets ($totalSheets)",
            style = AppTypography.label3Bold,
            color = Blue500,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewAllSheets() },
            textAlign = TextAlign.Center
        )
    }
}

// Helper data class
private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)