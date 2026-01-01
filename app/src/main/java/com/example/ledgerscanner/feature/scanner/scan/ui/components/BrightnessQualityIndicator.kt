package com.example.ledgerscanner.feature.scanner.scan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessQualityReport
import com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 01/01/26
// ===========================================================================

@Composable
fun BrightnessQualityIndicator(
    report: BrightnessQualityReport,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true
) {
    val check = report.brightnessCheck
    val color = check.level.toColor()
    val icon = when (check.level) {
        QualityLevel.EXCELLENT, QualityLevel.GOOD -> Icons.Default.CheckCircle
        QualityLevel.ACCEPTABLE -> Icons.Default.Warning
        QualityLevel.POOR, QualityLevel.FAILED -> Icons.Default.Error
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (check.level) {
                QualityLevel.EXCELLENT, QualityLevel.GOOD -> Green50
                QualityLevel.ACCEPTABLE -> Orange50
                QualityLevel.POOR, QualityLevel.FAILED -> Red50
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brightness:",
                        style = AppTypography.text14Regular,
                        color = Grey700
                    )
                    Text(
                        text = check.level.name,
                        style = AppTypography.text14Bold,
                        color = color
                    )
                }

                if (showDetails) {
                    Text(
                        text = "Level: ${check.value.toInt()}/255",
                        style = AppTypography.text12Regular,
                        color = Grey600
                    )

                    check.suggestion?.let { suggestion ->
                        Text(
                            text = suggestion,
                            style = AppTypography.text12Regular,
                            color = Grey700
                        )
                    }

                    // Show histogram info if available
                    report.histogram?.let { hist ->
                        if (hist.hasClipping) {
                            Text(
                                text = "⚠ Clipping detected: ${hist.tooBlackPercentage.toInt()}% shadows, ${hist.tooWhitePercentage.toInt()}% highlights",
                                style = AppTypography.text11Regular,
                                color = Orange600
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact version for preview overlay
 */
@Composable
fun BrightnessQualityBadge(
    report: BrightnessQualityReport,
    modifier: Modifier = Modifier
) {
    val check = report.brightnessCheck
    val color = check.level.toColor()
    val icon = check.level.toIcon()

    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = AppTypography.text14Bold,
            color = color
        )
        Text(
            text = "Brightness: ${check.level.name}",
            style = AppTypography.text13SemiBold,
            color = Grey900
        )
    }
}