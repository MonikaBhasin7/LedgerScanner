package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult

@Composable
fun ScoreSummaryCard(evaluation: EvaluationResult?) {
    evaluation?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Blue75),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Blue75, White)
                        )
                    )
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${evaluation.marksObtained}/${evaluation.maxMarks}",
                        style = AppTypography.title2ExtraBold,
                        color = Black,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "(${evaluation.percentage}%)",
                        style = AppTypography.h4Bold,
                        color = Blue500
                    )

                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider(thickness = 0.3.dp)

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreStat(Icons.Outlined.Check, evaluation.correctCount, "Correct", Green500)
                        ScoreStat(Icons.Outlined.Cancel, evaluation.incorrectCount, "Wrong", Red500)
                        ScoreStat(Icons.Outlined.RadioButtonUnchecked, evaluation.unansweredCount, "Blank", Grey600)
                        ScoreStat(Icons.Outlined.Warning, evaluation.multipleMarksQuestions.size, "Multiple\nAnswers", Orange500)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreStat(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = count.toString(),
                style = AppTypography.h3ExtraBold,
                color = color
            )
        }

        Spacer(Modifier.height(8.dp))
        
        Text(
            text = label,
            style = AppTypography.body3Regular,
            color = Grey700
        )
    }
}