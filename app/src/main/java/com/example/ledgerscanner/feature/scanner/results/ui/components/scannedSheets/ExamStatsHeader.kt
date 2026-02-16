package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics

@Composable
fun ExamStatsHeader(
    examEntity: ExamEntity,
    stats: ExamStatistics?,
    modifier: Modifier = Modifier,
    collapseFraction: Float = 0f
) {
    val safeStats = stats ?: ExamStatistics()
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val outerVerticalPadding = lerp(8.dp, 1.dp, fraction)
    val cardPadding = lerp(16.dp, 8.dp, fraction)
    val infoAlpha = (1f - (fraction * 1.35f)).coerceIn(0f, 1f)
    val infoSectionHeight = lerp(62.dp, 0.dp, fraction)
    val statTopSpacing = lerp(18.dp, 4.dp, fraction)

    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Blue75, Blue50)
                )
            )
            .padding(horizontal = 16.dp, vertical = outerVerticalPadding)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(16.dp))
                .border(1.dp, Blue100, RoundedCornerShape(16.dp))
                .padding(cardPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(infoSectionHeight)
                    .graphicsLayer {
                        alpha = infoAlpha
                        clip = true
                    }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = examEntity.examName,
                    style = AppTypography.text20Bold,
                    color = Grey900
                )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${examEntity.totalQuestions} Questions",
                        style = AppTypography.body3Regular,
                        color = Grey600
                    )
                }
            }
            Spacer(modifier = Modifier.height(statTopSpacing))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    value = "${safeStats.sheetsCount}",
                    label = "TOTAL",
                    color = Blue500,
                    modifier = Modifier.weight(1f),
                    collapseFraction = fraction
                )
                StatItem(
                    value = safeStats.getValidAvgScore(),
                    label = "AVERAGE",
                    color = Blue500,
                    modifier = Modifier.weight(1f),
                    collapseFraction = fraction
                )
                StatItem(
                    value = safeStats.getValidTopScore(),
                    label = "TOP",
                    color = Blue500,
                    modifier = Modifier.weight(1f),
                    collapseFraction = fraction
                )
                StatItem(
                    value = safeStats.getValidLowestScore(),
                    label = "LOWEST",
                    color = Blue500,
                    modifier = Modifier.weight(1f),
                    collapseFraction = fraction
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    collapseFraction: Float = 0f
) {
    val fraction = collapseFraction.coerceIn(0f, 1f)
    val valueStyle = AppTypography.text24Bold.copy(
        fontSize = lerp(24.sp, 16.sp, fraction),
        lineHeight = lerp(30.sp, 20.sp, fraction)
    )
    val labelStyle = AppTypography.text11SemiBold.copy(
        fontSize = lerp(11.sp, 9.sp, fraction),
        lineHeight = lerp(14.sp, 11.sp, fraction)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(lerp(6.dp, 2.dp, fraction))
    ) {
        Text(
            text = value,
            style = valueStyle,
            color = color
        )
        Text(
            text = label,
            style = labelStyle,
            color = Grey600,
            textAlign = TextAlign.Center
        )
    }
}
