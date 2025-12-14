package com.example.ledgerscanner.feature.scanner.exam.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel

@Composable
fun ReviewScreen(
    createExamViewModel: CreateExamViewModel,
    updateBottomBar: (BottomBarConfig) -> Unit,
    onEditStep: (ExamStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val examEntity by createExamViewModel.examEntity.collectAsState()

    LaunchedEffect(Unit) {
        updateBottomBar(
            BottomBarConfig(
                enabled = true,
                buttonText = "Save & Publish",
                onNext = {
                    createExamViewModel.finalizeExam()
                }
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        examEntity?.let { exam ->
            ExamSummaryCard(
                exam = exam,
                onEditBasicInfo = { onEditStep(ExamStep.BASIC_INFO) },
                onEditAnswerKey = { onEditStep(ExamStep.ANSWER_KEY) },
                onEditMarking = { onEditStep(ExamStep.MARKING) }
            )
        } ?: run {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exam data available",
                    style = AppTypography.body2Medium,
                    color = Grey500
                )
            }
        }
    }
}

@Composable
private fun ExamSummaryCard(
    exam: ExamEntity,
    onEditBasicInfo: () -> Unit,
    onEditAnswerKey: () -> Unit,
    onEditMarking: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Grey100)
            .border(1.dp, Grey200, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exam Summary",
                style = AppTypography.h4Bold
            )
            StatusBadge(status = exam.status.name)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exam Name
        SummaryRow(
            label = "Exam name",
            value = exam.examName,
            onEdit = onEditBasicInfo
        )

        SummaryDivider()

        // Total Questions
        SummaryRow(
            label = "Total questions",
            value = exam.totalQuestions.toString(),
            onEdit = onEditAnswerKey
        )

        SummaryDivider()

        // Marking Scheme
        val markingText = buildMarkingSchemeText(
            marksPerCorrect = exam.marksPerCorrect,
            marksPerWrong = exam.marksPerWrong
        )
        SummaryRow(
            label = "Marking scheme",
            value = markingText,
            onEdit = onEditMarking
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = AppTypography.body3Regular,
                color = Grey500
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = AppTypography.body1Medium
            )
        }

        EditButton(onClick = onEdit)
    }
}

@Composable
private fun EditButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(White)
            .border(1.dp, Grey200, RoundedCornerShape(20.dp))
            .genericClick { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Edit",
            style = AppTypography.label3Medium,
            color = Blue500
        )
    }
}

@Composable
private fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Grey200)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status,
            style = AppTypography.label4Medium,
            color = Grey500
        )
    }
}

@Composable
private fun SummaryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color = Grey200
    )
}

private fun buildMarkingSchemeText(
    marksPerCorrect: Float?,
    marksPerWrong: Float?
): String {
    val correct = marksPerCorrect ?: 0f
    val wrong = marksPerWrong ?: 0f

    val correctText = if (correct == correct.toInt().toFloat()) {
        "+${correct.toInt()}"
    } else {
        "+$correct"
    }

    val wrongText = if (wrong == wrong.toInt().toFloat()) {
        wrong.toInt().toString()
    } else {
        wrong.toString()
    }

    val negativeMarkingStatus = if (wrong < 0f) "on" else "off"

    return "$correctText correct, $wrongText wrong (negative marking $negativeMarkingStatus)"
}