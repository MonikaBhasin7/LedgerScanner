package com.example.ledgerscanner.feature.scanner.scan.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.feature.scanner.scan.model.EvaluationResult

@Composable
fun ScoreSummaryCard(
    evaluation: EvaluationResult,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        evaluation.percentage >= 70f -> Color(0xFF16A34A) // Green
        evaluation.percentage >= 40f -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFDC2626) // Red
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.10f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Score Summary",
                style = AppTypography.label1Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Score Rows
            ScoreRow(label = "Total Questions", value = "${evaluation.totalQuestions}")
            Spacer(modifier = Modifier.height(8.dp))

            ScoreRow(
                label = "Correct Answers",
                value = "${evaluation.correctCount}",
                valueColor = Color(0xFF16A34A) // Green
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScoreRow(
                label = "Incorrect Answers",
                value = "${evaluation.incorrectCount}",
                valueColor = Color(0xFFDC2626) // Red
            )
            Spacer(modifier = Modifier.height(8.dp))

            ScoreRow(
                label = "Unanswered",
                value = "${evaluation.unansweredCount}",
                valueColor = Color.Gray
            )

            // Show multiple marks if any
            if (evaluation.multipleMarksQuestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                ScoreRow(
                    label = "Multiple Marks ⚠️",
                    value = "${evaluation.multipleMarksQuestions.size}",
                    valueColor = Color(0xFFF59E0B) // Orange
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Marks Obtained
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Marks Obtained",
                    style = AppTypography.body1Regular.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black
                )
                Text(
                    text = evaluation.getMarksFormatted(),
                    style = AppTypography.body1Regular.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Percentage and Grade
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎯 Percentage",
                    style = AppTypography.body1Regular.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black
                )
                Text(
                    text = "${evaluation.getPercentageFormatted()} • Grade: ${evaluation.getGrade()}",
                    style = AppTypography.body1Regular.copy(fontWeight = FontWeight.Bold),
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(
    label: String,
    value: String,
    valueColor: Color = Color.Black
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = AppTypography.body2Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = AppTypography.body2Medium.copy(fontWeight = FontWeight.Medium),
            color = valueColor
        )
    }
}