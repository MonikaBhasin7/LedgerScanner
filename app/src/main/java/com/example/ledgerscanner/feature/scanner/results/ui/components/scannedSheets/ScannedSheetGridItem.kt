package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue700
import com.example.ledgerscanner.base.ui.theme.Green50
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Red50
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.DateAndTimeUtils
import com.example.ledgerscanner.base.extensions.toCleanString
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScannedSheetGridItem(
    sheet: ScanResultEntity,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onCardClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onViewDetails: () -> Unit = {}
) {
    val percent = sheet.scorePercent
    val attempted = (sheet.totalQuestions - sheet.blankCount).coerceAtLeast(0)
    val cardInteractionSource = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = cardInteractionSource,
                indication = ripple(color = Blue100),
                onClick = {
                    if (selectionMode) {
                        onCardClick()
                    } else {
                        onViewDetails()
                    }
                },
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = Blue500,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50 else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 3.dp else 2.dp,
            pressedElevation = if (isSelected) 5.dp else 4.dp
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Grey200)
                        .border(1.dp, Grey200, RoundedCornerShape(8.dp))
                ) {
                    val file = File(sheet.scannedImagePath)
                    if (file.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(file)
                                .crossfade(false)
                                .build(),
                            contentDescription = "Sheet preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        SheetPlaceholder()
                    }

                    if (ScanResultUtils.isRecentSheet(sheet.scannedAt) && !selectionMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            NewBadge()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sheet #${sheet.id}",
                        style = AppTypography.text14Bold,
                        color = Grey900
                    )
                    GridPercentagePill(percent)
                }

                Text(
                    text = sheet.enrollmentNumber ?: (sheet.barCode ?: "Unknown"),
                    style = AppTypography.text12SemiBold,
                    color = Grey700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GridMetaPill("Q ${sheet.totalQuestions}")
                    GridMetaPill("A $attempted")
                }

                GridScorePanel(
                    score = sheet.score,
                    totalQuestions = sheet.totalQuestions,
                    percent = percent
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GridStatChip("✓", sheet.correctCount, Green50, Green600)
                    GridStatChip("✕", sheet.wrongCount, Red50, Red600)
                    GridStatChip("—", sheet.blankCount, Grey100, Grey600)
                    if (!sheet.multipleMarksDetected.isNullOrEmpty()) {
                        GridStatChip("!", sheet.multipleMarksDetected.size, Orange50, Orange600)
                    }
                }

                if (!selectionMode) {
                    Text(
                        text = DateAndTimeUtils.formatTimeAgo(sheet.scannedAt),
                        style = AppTypography.text11Regular,
                        color = Grey600
                    )
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
                            checkedColor = Blue500,
                            uncheckedColor = Grey400,
                            checkmarkColor = White
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GridPercentagePill(percent: Float) {
    Box(
        modifier = Modifier
            .background(Blue50, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "${percent.toCleanString()}%",
            style = AppTypography.text11Bold,
            color = Blue700
        )
    }
}

@Composable
private fun GridMetaPill(text: String) {
    Box(
        modifier = Modifier
            .background(Grey100, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.text10Medium,
            color = Grey600
        )
    }
}

@Composable
private fun GridScorePanel(
    score: Float,
    totalQuestions: Int,
    percent: Float
) {
    val progressValue = if (percent <= 0f) 0f else percent / 100f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Blue50, RoundedCornerShape(10.dp))
            .border(1.dp, Blue100, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${score.toCleanString()}/$totalQuestions",
                    style = AppTypography.text12SemiBold,
                    color = Blue700
                )
                Text(
                    text = "${percent.toCleanString()}%",
                    style = AppTypography.text13Bold,
                    color = Blue700
                )
            }
            LinearProgressIndicator(
                progress = { progressValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Blue500,
                trackColor = Blue100
            )
        }
    }
}

@Composable
private fun GridStatChip(
    icon: String,
    count: Int,
    bgColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(999.dp))
            .border(1.dp, contentColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = AppTypography.text11Bold, color = contentColor)
            Text(text = count.toString(), style = AppTypography.text11Bold, color = contentColor)
        }
    }
}

@Composable
fun ScannedSheetGridRow(
    rowSheets: List<ScanResultEntity>,
    selectedSheets: Set<Int>,
    selectionMode: Boolean,
    onCardClick: (Int) -> Unit,
    onLongClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rowSheets.forEach { sheet ->
            key(sheet.id) {
                ScannedSheetGridItem(
                    sheet = sheet,
                    isSelected = selectedSheets.contains(sheet.id),
                    selectionMode = selectionMode,
                    onCardClick = { onCardClick(sheet.id) },
                    onLongClick = { onLongClick(sheet.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (rowSheets.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
