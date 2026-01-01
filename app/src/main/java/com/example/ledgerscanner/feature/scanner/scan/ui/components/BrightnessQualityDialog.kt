package com.example.ledgerscanner.feature.scanner.scan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessQualityReport
import com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel

@Composable
fun BrightnessQualityDialog(
    report: BrightnessQualityReport,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = when (report.brightnessCheck.level) {
                    QualityLevel.EXCELLENT, QualityLevel.GOOD -> Icons.Default.CheckCircle
                    QualityLevel.ACCEPTABLE -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = report.brightnessCheck.level.toColor(),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = when (report.brightnessCheck.level) {
                    QualityLevel.EXCELLENT, QualityLevel.GOOD -> "Good Image Quality"
                    QualityLevel.ACCEPTABLE -> "Acceptable Quality"
                    QualityLevel.POOR -> "Poor Image Quality"
                    QualityLevel.FAILED -> "Quality Check Failed"
                },
                style = AppTypography.h4Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Brightness: ${report.brightnessCheck.value.toInt()}/255",
                    style = AppTypography.body2Regular
                )

                report.brightnessCheck.suggestion?.let {
                    Text(
                        text = it,
                        style = AppTypography.body2Regular,
                        color = Orange600
                    )
                }

                if (report.brightnessCheck.level <= QualityLevel.ACCEPTABLE) {
                    Text(
                        text = "Processing may have reduced accuracy. Consider retaking for best results.",
                        style = AppTypography.text12Regular,
                        color = Grey600
                    )
                }
            }
        },
        confirmButton = {
            if (report.brightnessCheck.level >= QualityLevel.ACCEPTABLE) {
                GenericButton(
                    text = "Continue",
                    onClick = onContinue,
                    type = ButtonType.PRIMARY
                )
            }
        },
        dismissButton = {
            GenericButton(
                text = if (report.brightnessCheck.level >= QualityLevel.ACCEPTABLE) "Retake" else "Try Again",
                onClick = onRetry,
                type = ButtonType.SECONDARY
            )
        }
    )
}