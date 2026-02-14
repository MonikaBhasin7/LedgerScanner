package com.example.ledgerscanner.feature.scanner.results.ui.components.scannedSheets

import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.ledgerscanner.base.ui.theme.Grey300
import com.example.ledgerscanner.base.ui.theme.Grey400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange100
import com.example.ledgerscanner.base.ui.theme.Orange50
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.Red50
import com.example.ledgerscanner.base.ui.theme.Red600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.DateAndTimeUtils
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.utils.ScanResultUtils
import java.io.File
import kotlinx.coroutines.delay

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
    var statTooltip by remember(sheet.id) { mutableStateOf<StatTooltipInfo?>(null) }
    val cardInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(statTooltip) {
        if (statTooltip != null) {
            delay(2000)
            statTooltip = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = cardInteractionSource,
                indication = ripple(color = Blue100),
                onClick = {
                    if (selectionMode) onCardClick() else onViewDetails()
                },
                onLongClick = onLongClick
            )
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Blue500, RoundedCornerShape(18.dp))
                } else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50 else White
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sheet #${sheet.id}",
                                style = AppTypography.text16Bold,
                                color = Grey900
                            )
                            if (ScanResultUtils.isRecentSheet(sheet.scannedAt)) {
                                NewBadge()
                            }
                        }

                        sheet.enrollmentNumber?.let {
                            if (it.isNotEmpty()) {
                                Text(
                                    text = "Eno: ${sheet.enrollmentNumber}",
                                    style = AppTypography.text15SemiBold,
                                    color = Grey700,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MetaPill("Questions ${sheet.totalQuestions}")
                            MetaPill("Attempted $attempted")
                        }

                        ScorePanel(
                            score = sheet.score,
                            totalQuestions = sheet.totalQuestions,
                            percent = percent
                        )

                        if (statTooltip != null) {
                            StatTooltipBubble(
                                info = statTooltip!!,
                                pointerCenterX = statTooltip!!.pointerCenterX
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(
                                icon = "✓",
                                value = "${sheet.correctCount}",
                                bgColor = Green50,
                                contentColor = Green600,
                                onClick = { centerX ->
                                    statTooltip = StatTooltipInfo(
                                        icon = "✓",
                                        title = "Correct Answers",
                                        description = "Questions answered correctly on this sheet.",
                                        pointerCenterX = centerX
                                    )
                                }
                            )
                            StatChip(
                                icon = "✕",
                                value = "${sheet.wrongCount}",
                                bgColor = Red50,
                                contentColor = Red600,
                                onClick = { centerX ->
                                    statTooltip = StatTooltipInfo(
                                        icon = "✕",
                                        title = "Wrong Answers",
                                        description = "Questions attempted with incorrect choices.",
                                        pointerCenterX = centerX
                                    )
                                }
                            )
                            StatChip(
                                icon = "—",
                                value = "${sheet.blankCount}",
                                bgColor = Grey100,
                                contentColor = Grey600,
                                onClick = { centerX ->
                                    statTooltip = StatTooltipInfo(
                                        icon = "—",
                                        title = "Blank Answers",
                                        description = "Questions left unanswered by the student.",
                                        pointerCenterX = centerX
                                    )
                                }
                            )
                            if (!sheet.multipleMarksDetected.isNullOrEmpty()) {
                                StatChip(
                                    icon = "!",
                                    value = "${sheet.multipleMarksDetected.size}",
                                    bgColor = Orange50,
                                    contentColor = Orange600,
                                    onClick = { centerX ->
                                        statTooltip = StatTooltipInfo(
                                            icon = "⚠",
                                            title = "Multiple Marks",
                                            description = "Questions where more than one option was marked.",
                                            pointerCenterX = centerX
                                        )
                                    }
                                )
                            }
                        }
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
                        Text(
                            text = "View Result",
                            color = Blue500,
                            style = AppTypography.text14SemiBold,
                            modifier = Modifier.clickable { onViewDetails() }
                        )
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
                            checkedColor = Blue500,
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
    val imageShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .width(88.dp)
            .height(122.dp)
            .clip(imageShape)
            .background(Grey200, imageShape)
            .border(1.dp, Grey300, imageShape)
    ) {
        val file = imagePath?.let { File(it) }
        if (file != null && file.exists()) {
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
                                    style = AppTypography.text13SemiBold,
                                    color = Blue700
                                )
                                Text(
                                    text = "$percent%",
                                    style = AppTypography.text14Bold,
                                    color = Blue700
                                )
            }
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Blue500,
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
    contentColor: Color,
    onClick: (Float) -> Unit
) {
    var centerX by remember { mutableStateOf(0f) }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(999.dp))
            .border(
                width = 1.dp,
                color = contentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
            .onGloballyPositioned { coordinates ->
                centerX = coordinates.positionInParent().x + (coordinates.size.width / 2f)
            }
            .genericClick { onClick(centerX) }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = AppTypography.text13Bold, color = contentColor)
            Text(text = value, style = AppTypography.text14Bold, color = contentColor)
        }
    }
}

private data class StatTooltipInfo(
    val icon: String,
    val title: String,
    val description: String,
    val pointerCenterX: Float
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun StatTooltipBubble(info: StatTooltipInfo, pointerCenterX: Float) {
    val tooltipBg = Grey800
    val density = LocalDensity.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val pointerWidth = 20.dp
        val pointerHeight = 10.dp
        val horizontalInset = 10.dp
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val halfPointerPx = with(density) { pointerWidth.toPx() / 2f }
        val insetPx = with(density) { horizontalInset.toPx() }
        val clampedCenterX = pointerCenterX.coerceIn(
            insetPx + halfPointerPx,
            maxWidthPx - insetPx - halfPointerPx
        )
        val pointerStartDp = with(density) { (clampedCenterX - halfPointerPx).toDp() }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tooltipBg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = info.icon,
                            style = AppTypography.text14Bold,
                            color = Orange100
                        )
                        Text(
                            text = info.title,
                            style = AppTypography.text13Bold,
                            color = Orange100
                        )
                    }
                    Text(
                        text = info.description,
                        style = AppTypography.text12Regular,
                        color = White
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pointerHeight)
            ) {
                Canvas(
                    modifier = Modifier
                        .padding(start = pointerStartDp)
                        .width(pointerWidth)
                        .height(pointerHeight)
                ) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(size.width, 0f)
                        close()
                    }
                    drawPath(path = path, color = tooltipBg)
                }
            }
        }
    }
}
