package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.extensions.toCleanString
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Orange500
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.database.entity.getMultipleMarksDetectedCount

@Composable
fun ScoreSummaryCard(scanResultEntity: ScanResultEntity, examEntity: ExamEntity) {
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
                    text = "${scanResultEntity.score}/${examEntity.getMaxMarks().toCleanString()}",
                    style = AppTypography.title2ExtraBold,
                    color = Black,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "(${scanResultEntity.scorePercent.toCleanString()}%)",
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
                    ScoreStat(
                        Icons.Outlined.Check,
                        scanResultEntity.correctCount,
                        "Correct",
                        Green500
                    )
                    ScoreStat(Icons.Outlined.Cancel, scanResultEntity.wrongCount, "Wrong", Red500)
                    ScoreStat(
                        Icons.Outlined.RadioButtonUnchecked,
                        scanResultEntity.blankCount,
                        "Blank",
                        Grey600
                    )
                    ScoreStat(
                        Icons.Outlined.Warning,
                        scanResultEntity.getMultipleMarksDetectedCount(),
                        "Multiple\nAnswers",
                        Orange500
                    )
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