package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Blue600
import com.example.ledgerscanner.base.ui.theme.Blue700
import com.example.ledgerscanner.base.ui.theme.Green50
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey300
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Red50
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.DateAndTimeUtils
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils
import java.io.File

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
    val percent = sheet.scorePercent.toInt().coerceIn(0, 100)
    val attempted = (sheet.totalQuestions - sheet.blankCount).coerceAtLeast(0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onCardClick() else onViewDetails()
                },
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Blue600, RoundedCornerShape(18.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50 else White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 10.dp else 7.dp,
            pressedElevation = if (isSelected) 12.dp else 9.dp
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        top = 14.dp,
                        end = 14.dp,
                        bottom = if (selectionMode) 14.dp else 8.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    CardPreviewImage(imagePath = sheet.scannedImagePath)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sheet #${sheet.id}",
                                style = AppTypography.text16Bold,
                                color = Grey900
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ScanResultUtils.isRecentSheet(sheet.scannedAt)) {
                                    NewBadge()
                                }
                                PercentagePill(percent = percent)
                            }
                        }

                        Text(
                            text = sheet.enrollmentNumber ?: (sheet.barCode ?: "Unknown Candidate"),
                            style = AppTypography.text15SemiBold,
                            color = Grey700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetaPill("Questions ${sheet.totalQuestions}")
                            MetaPill("Attempted $attempted")
                        }

                        ScorePanel(
                            score = sheet.score,
                            totalQuestions = sheet.totalQuestions,
                            percent = percent
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatChip("✓", "${sheet.correctCount}", Green50, Green600)
                    StatChip("✕", "${sheet.wrongCount}", Red50, Red600)
                    StatChip("—", "${sheet.blankCount}", Grey100, Grey600)
                    if (!sheet.multipleMarksDetected.isNullOrEmpty()) {
                        StatChip("⚠", "${sheet.multipleMarksDetected.size}", Orange50, Orange600)
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = Grey200)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Scanned ${DateAndTimeUtils.formatTimeAgo(sheet.scannedAt)}",
                        style = AppTypography.text13Regular,
                        color = Grey500
                    )

                    if (!selectionMode) {
                        TextButton(
                            onClick = onViewDetails,
                            modifier = Modifier.height(IntrinsicSize.Min)
                        ) {
                            Text(
                                text = "View Result",
                                color = Blue600,
                                style = AppTypography.text14SemiBold
                            )
                        }
                    }
                }
            }

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
private fun CardPreviewImage(imagePath: String?) {
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(122.dp)
            .background(Grey200, RoundedCornerShape(12.dp))
            .border(1.dp, Grey300, RoundedCornerShape(12.dp))
    ) {
        val file = imagePath?.let { File(it) }
        if (file != null && file.exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file)
                    .crossfade(true)
                    .build(),
                contentDescription = "Sheet preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Sheet placeholder",
                    tint = Grey400,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
    }
}

@Composable
private fun PercentagePill(percent: Int) {
    Box(
        modifier = Modifier
            .background(Blue50, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "$percent%",
            style = AppTypography.text12Bold,
            color = Blue700
        )
    }
}

@Composable
private fun MetaPill(text: String) {
    Box(
        modifier = Modifier
            .background(Grey100, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.text11Medium,
            color = Grey600
        )
    }
}

@Composable
private fun ScorePanel(score: Int, totalQuestions: Int, percent: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blue50, RoundedCornerShape(12.dp))
            .border(1.dp, Blue100, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score $score/$totalQuestions",
                    style = AppTypography.text14SemiBold,
                    color = Blue700
                )
                Text(
                    text = "$percent%",
                    style = AppTypography.text16Bold,
                    color = Blue700
                )
            }
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Blue600,
                trackColor = Blue100
            )
        }
    }
}

@Composable
private fun StatChip(
    icon: String,
    value: String,
    bgColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = AppTypography.text12Bold, color = contentColor)
            Text(text = value, style = AppTypography.text12SemiBold, color = contentColor)
        }
    }
}
