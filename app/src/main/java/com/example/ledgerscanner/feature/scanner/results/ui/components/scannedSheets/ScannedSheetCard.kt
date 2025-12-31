package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.base.ui.theme.*
import com.example.ledgerscanner.base.utils.DateAndTimeUtils
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils

@Composable
fun ScannedSheetCard(
    sheet: ScanResultEntity,
    modifier: Modifier = Modifier,
    onViewDetails: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SheetPreviewImages(sheet.scannedImagePath, sheet.thumbnailPath)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Sheet #${sheet.id}", style = AppTypography.text20Bold, color = Grey900)
                        if (ScanResultUtils.isRecentSheet(sheet.scannedAt)) {
                            NewBadge()
                        }
                    }

                    Text(text = "Student: ${sheet.barCode ?: "Unknown"}", style = AppTypography.text15Regular, color = Grey700)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${sheet.score}/${sheet.totalQuestions} (${sheet.scorePercent.toInt()}%)", style = AppTypography.text28Bold, color = Blue700)
                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreIndicator("✓", sheet.correctCount, Green600)
                        ScoreIndicator("✕", sheet.wrongCount, Red600)
                        ScoreIndicator("—", sheet.blankCount, Grey600)
                        if (!sheet.multipleMarksDetected.isNullOrEmpty()) {
                            ScoreIndicator("⚠", sheet.multipleMarksDetected.size, Orange600)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Scanned: ${DateAndTimeUtils.formatTimeAgo(sheet.scannedAt)}", style = AppTypography.text13Regular, color = Grey500)
                TextButton(
                    onClick = onViewDetails,
                    modifier = Modifier.height(IntrinsicSize.Min)
                ) {
                    Text(text = "View Details", color = Blue500, style = AppTypography.text15SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScoreIndicator(icon: String, count: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, color = color, style = AppTypography.label1Bold, fontSize = 18.sp)
        Text(text = count.toString(), color = color, style = AppTypography.body2SemiBold)
    }
}