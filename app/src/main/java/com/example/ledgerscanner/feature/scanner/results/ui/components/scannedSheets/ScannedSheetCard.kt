package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Blue600
import com.example.ledgerscanner.base.ui.theme.Blue700
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.utils.DateAndTimeUtils
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScannedSheetCard(
    sheet: ScanResultEntity,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onCardClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onViewDetails: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onCardClick()
                    }
                },
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = Blue600,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50 else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = if (selectionMode) 16.dp else 0.dp
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetPreviewImages(
                        imagePath = sheet.scannedImagePath,
                        thumbnailPath = sheet.thumbnailPath
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sheet #${sheet.id}",
                                style = AppTypography.text20Bold,
                                color = Grey900
                            )

                            if (ScanResultUtils.isRecentSheet(sheet.scannedAt)) {
                                NewBadge()
                            }
                        }

                        Text(
                            text = "Student: ${sheet.barCode ?: "Unknown"}",
                            style = AppTypography.text15Regular,
                            color = Grey700
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${sheet.score}/${sheet.totalQuestions} (${sheet.scorePercent.toInt()}%)",
                            style = AppTypography.text28Bold,
                            color = Blue700
                        )

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
                    Text(
                        text = "Scanned: ${DateAndTimeUtils.formatTimeAgo(sheet.scannedAt)}",
                        style = AppTypography.text13Regular,
                        color = Grey500
                    )

                    if (!selectionMode) {
                        TextButton(
                            onClick = onViewDetails,
                            modifier = Modifier.height(IntrinsicSize.Min)
                        ) {
                            Text(
                                text = "View Details",
                                color = Blue600,
                                style = AppTypography.text15SemiBold
                            )
                        }
                    }
                }
            }

            // Checkbox overlay in selection mode
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onCardClick() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Blue600,
                            uncheckedColor = Grey400
                        )
                    )
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