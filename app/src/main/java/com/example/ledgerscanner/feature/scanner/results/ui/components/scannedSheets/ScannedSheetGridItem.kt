package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Blue600
import com.example.ledgerscanner.base.ui.theme.Blue700
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.ui.theme.White
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
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
                        color = Blue600,
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sheet Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Grey200)
                ) {
                    val file = File(sheet.scannedImagePath)
                    if (file.exists()) {
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
                        SheetPlaceholder()
                    }

                    // New Badge overlay (only show if not in selection mode)
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

                // Sheet ID
                Text(
                    text = "Sheet #${sheet.id}",
                    style = AppTypography.text16Bold,
                    color = Grey900
                )

                // Student
                Text(
                    text = sheet.barCode ?: "Unknown",
                    style = AppTypography.text13Regular,
                    color = Grey600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Score
                Text(
                    text = "${sheet.scorePercent.toInt()}%",
                    style = AppTypography.text22Bold,
                    color = Blue700
                )

                // Score indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactScoreIndicator(
                        icon = "✓",
                        count = sheet.correctCount,
                        color = Green600
                    )

                    CompactScoreIndicator(
                        icon = "✕",
                        count = sheet.wrongCount,
                        color = Red600
                    )

                    CompactScoreIndicator(
                        icon = "—",
                        count = sheet.blankCount,
                        color = Grey600
                    )
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
private fun CompactScoreIndicator(
    icon: String,
    count: Int,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            color = color,
            style = AppTypography.text14Bold,
            fontSize = 14.sp
        )
        Text(
            text = count.toString(),
            color = color,
            style = AppTypography.text13SemiBold
        )
    }
}

// Grid Row Component
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
            ScannedSheetGridItem(
                sheet = sheet,
                isSelected = selectedSheets.contains(sheet.id),
                selectionMode = selectionMode,
                onCardClick = { onCardClick(sheet.id) },
                onLongClick = { onLongClick(sheet.id) },
                modifier = Modifier.weight(1f)
            )
        }
        // Fill empty space if odd number of items
        if (rowSheets.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
