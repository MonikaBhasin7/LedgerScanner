package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStatistics

@Composable
fun ExamStatsHeader(
    examEntity: ExamEntity,
    stats: ExamStatistics?
) {
    val safeStats = stats ?: ExamStatistics()
    Box(
        modifier = Modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Blue75, White)
                )
            )
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = examEntity.examName,
                style = AppTypography.text22Bold,
                color = Grey900
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${examEntity.totalQuestions} Questions",
                style = AppTypography.body3Regular,
                color = Grey600
            )
            Spacer(modifier = Modifier.height(18.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(value = "${safeStats.sheetsCount}", label = "Total\nsheets", color = Blue500)
                StatItem(value = safeStats.getValidAvgScore(), label = "Average\nscore", color = Blue500)
                StatItem(value = safeStats.getValidTopScore(), label = "Top\nscore", color = Blue500)
                StatItem(value = safeStats.getValidLowestScore(), label = "Lowest\nscore", color = Blue500)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = value, style = AppTypography.text24Bold, color = color)
        Text(text = label, style = AppTypography.text13Regular, color = Grey600, textAlign = TextAlign.Center)
    }
}
