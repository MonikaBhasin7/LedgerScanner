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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ScoreSummaryCard(
    totalQuestions: Int,
    correctAnswers: Int,
    incorrectAnswers: Int
) {
    val scoreColor = when {
        correctAnswers >= totalQuestions * 0.7 -> Color(0xFF16A34A) // Green
        correctAnswers >= totalQuestions * 0.4 -> Color(0xFFF59E0B) // Amber
        else -> Color(0xFFDC2626) // Red
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = scoreColor.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Score Summary",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Score Rows
            ScoreRow(label = "Total Questions", value = "$totalQuestions")
            ScoreRow(label = "Correct Answers", value = "$correctAnswers")
            ScoreRow(label = "Incorrect Answers", value = "$incorrectAnswers")

            Spacer(modifier = Modifier.height(12.dp))
            Divider(thickness = 1.dp, color = Color.LightGray.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(12.dp))

            // Final Score
            Text(
                text = "🎯 Final Score: $correctAnswers / $totalQuestions",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
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
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.Black
        )
    }
}