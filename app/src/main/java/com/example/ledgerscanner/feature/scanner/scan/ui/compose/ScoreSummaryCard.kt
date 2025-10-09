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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.ledgerscanner.base.ui.theme.AppTypography

@Composable
fun ScoreSummaryCard(
    marks: List<Boolean>
) {
    val totalQuestions = marks.size
    val correctAnswers = marks.count { it }
    val incorrectAnswers = totalQuestions - correctAnswers
    val ratio = if (totalQuestions > 0) correctAnswers.toDouble() / totalQuestions else 0.0
    val percent = (ratio * 100).roundToInt()

    val scoreColor = when {
        ratio >= 0.7 -> Color(0xFF16A34A) // Green
        ratio >= 0.4 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFDC2626) // Red
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.10f)
        ),
        modifier = Modifier
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

            Spacer(modifier = Modifier.height(8.dp))

            // Score Rows
            ScoreRow(label = "Total Questions", value = "$totalQuestions")
            ScoreRow(label = "Correct Answers", value = "$correctAnswers")
            ScoreRow(label = "Incorrect Answers", value = "$incorrectAnswers")

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Final Score + percent
            Text(
                text = "🎯 Final Score: $correctAnswers / $totalQuestions  •  $percent%",
                style = AppTypography.body1Regular.copy(fontWeight = FontWeight.SemiBold),
                color = scoreColor
            )
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
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
            color = Color.Black
        )
    }
}