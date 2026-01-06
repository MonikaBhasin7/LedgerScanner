package com.example.ledgerscanner.feature.scanner.results.ui.components.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Orange200
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Orange700
import com.example.ledgerscanner.base.ui.theme.Orange800
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

@Composable
fun ReviewRequiredCard(
    omrImageProcessResult: OmrImageProcessResult,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val lowConfidenceQuestions = remember(omrImageProcessResult.detectedBubbles) {
        omrImageProcessResult.detectedBubbles
            ?.groupBy { it.questionIndex }
            ?.mapNotNull { (qIndex, bubbles) ->
                val maxConf = bubbles.maxOfOrNull { it.confidence } ?: 0.0
                if (maxConf < 0.60) {
                    qIndex to maxConf
                } else null
            }
            ?.sortedBy { it.second } // Lowest confidence first
            ?: emptyList()
    }

    if (lowConfidenceQuestions.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Orange50
            ),
            border = BorderStroke(1.dp, Orange200)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Orange600,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Review Required",
                        style = AppTypography.text16Bold,
                        color = Orange800
                    )
                }

                Text(
                    text = "${lowConfidenceQuestions.size} question${if (lowConfidenceQuestions.size > 1) "s" else ""} have low confidence detection",
                    style = AppTypography.text14Regular,
                    color = Grey800
                )

                Text(
                    text = lowConfidenceQuestions.joinToString(", ") { (qNum, conf) ->
                        "Q${qNum + 1} (${(conf * 100).toInt()}%)"
                    },
                    style = AppTypography.text14SemiBold,
                    color = Orange700
                )

                TextButton(
                    onClick = onReviewClick,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = "Review & Edit",
                        style = AppTypography.text14Bold,
                        color = Orange700
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Orange700,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

        }
    }
}