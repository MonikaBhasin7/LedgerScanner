package com.example.ledgerscanner.feature.scanner.exam.presentation.examlist.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.base.extensions.toCleanString
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamAction
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamActionPopupConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatistics
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStatus
import com.example.ledgerscanner.feature.scanner.exam.domain.model.QuickActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ExamCardRow(
    item: ExamEntity,
    examStatistics: ExamStatistics?,
    showCompletionCelebration: Boolean = false,
    showNoScanWalkthrough: Boolean = false,
    onDismissNoScanWalkthrough: () -> Unit = {},
    onClick: (ExamAction) -> Unit,
    onActionClick: (ExamAction) -> Unit,
    actionsProvider: (ExamStatus, Boolean) -> ExamActionPopupConfig
) {
    val isArchived = item.status == ExamStatus.ARCHIVED
    val badgeCelebrationProgress by animateFloatAsState(
        targetValue = if (showCompletionCelebration && item.status == ExamStatus.COMPLETED) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "completedBadgeMorph"
    )
    val hasLoadedStats = examStatistics != null
    val sheetCount = examStatistics?.sheetsCount ?: 0
    val actions = actionsProvider(item.status, sheetCount > 0)
    var showMenu by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var swipeOffsetPx by remember(item.id) { mutableStateOf(0f) }
    val animatedSwipeOffset by animateFloatAsState(
        targetValue = swipeOffsetPx,
        animationSpec = tween(durationMillis = 190),
        label = "cardSwipeOffset"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val revealWidthPx = with(density) { (maxWidth - 56.dp).toPx() }
        if (actions.menuItems.isNotEmpty() && animatedSwipeOffset < -1f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Blue100.copy(alpha = 0.28f))
                    .border(1.dp, Grey200, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .padding(bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Quick actions",
                        style = AppTypography.text11Medium,
                        color = Grey600
                    )

                    actions.menuItems.chunked(2).forEach { rowActions ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowActions.forEach { action ->
                                val actionTint =
                                    if (action.isDangerous) Color(0xFFD32F2F) else Blue500
                                val actionBg =
                                    if (action.isDangerous) Color(0xFFFFEBEE) else White
                                val actionBorder =
                                    if (action.isDangerous) Color(0xFFF2B8BE) else Grey200
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(actionBg)
                                        .border(1.dp, actionBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            swipeOffsetPx = 0f
                                            onActionClick(action)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.label,
                                        tint = actionTint,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = action.label,
                                        style = AppTypography.text11Medium,
                                        color = if (action.isDangerous) actionTint else Grey800,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(White)
                        .border(1.dp, Grey200, RoundedCornerShape(10.dp))
                        .clickable { swipeOffsetPx = 0f }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close actions",
                        tint = Grey600,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Close",
                        style = AppTypography.text10Medium,
                        color = Grey600
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                .border(1.dp, Grey200, RoundedCornerShape(14.dp))
                .pointerInput(item.id, actions.menuItems.size) {
                    if (actions.menuItems.isEmpty()) return@pointerInput
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            swipeOffsetPx =
                                (swipeOffsetPx + dragAmount).coerceIn(-revealWidthPx, 0f)
                        },
                        onDragEnd = {
                            swipeOffsetPx =
                                if (swipeOffsetPx <= -revealWidthPx * 0.35f) -revealWidthPx else 0f
                        },
                        onDragCancel = {
                            swipeOffsetPx =
                                if (swipeOffsetPx <= -revealWidthPx * 0.35f) -revealWidthPx else 0f
                        }
                    )
                },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isArchived) Grey100 else White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .genericClick {
                        if (swipeOffsetPx < -8f) {
                            swipeOffsetPx = 0f
                        } else {
                            actions.quickAction?.action?.let { onClick(it) }
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
//                verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExamIcon(status = item.status)
                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StatusBadge(
                                status = item.status,
                                celebrationProgress = badgeCelebrationProgress
                            )
                            Text(
                                text = item.examName,
                                color = Black,
                                style = AppTypography.text18Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Box {
                            IconButton(
                                onClick = {
                                    swipeOffsetPx = 0f
                                    showMenu = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More actions",
                                    tint = Grey500
                                )
                            }

                            ExamActionsPopup(
                                expanded = showMenu,
                                actions = actions,
                                onActionClick = { action ->
                                    showMenu = false
                                    onActionClick(action)
                                },
                                onDismiss = { showMenu = false },
                            )
                        }
                    }

                    Column {
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            val lastActivityAt = examStatistics?.lastScannedAt ?: item.createdAt
                            LastActivityChip(lastActivityAt = lastActivityAt)
                        }
                        Box(modifier = Modifier.padding(bottom = 8.dp)) {
                            ExamMetadata(
                                totalQuestions = item.totalQuestions,
                                createdAt = item.createdAt,
                                sheetsCount = sheetCount,
                                status = item.status
                            )
                        }
                        if (item.status == ExamStatus.DRAFT) {
                            Box(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                                DraftSetupProgressStrip(item = item)
                            }
                        }
                        Box(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 12.dp)) {
                            StatusDetailLine(status = item.status, sheetsCount = sheetCount)
                        }
                    }

                    AnimatedVisibility(
                        visible = examStatistics != null && examStatistics.hasStats() && sheetCount > 0,
                        enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                            animationSpec = tween(300),
                            expandFrom = Alignment.Top
                        ),
                        exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(
                            animationSpec = tween(120),
                            shrinkTowards = Alignment.Top
                        )
                    ) {
                        Box(modifier = Modifier.padding(bottom = 10.dp)) {
                            ExamStats(
                                avgScore = examStatistics?.avgScore,
                                topScore = examStatistics?.topScore,
                                lowestScore = examStatistics?.lowestScore,
                                isArchived = isArchived
                            )
                        }
                    }

                    val shouldNudgeScan =
                        hasLoadedStats && sheetCount == 0 && actions.quickAction?.action is ExamAction.ScanSheets

                    if (shouldNudgeScan) {
                        NoScanContextualNudge(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 8.dp)
                        )

                        if (showNoScanWalkthrough) {
                            NoScanWalkthroughTooltip(
                                onDismiss = onDismissNoScanWalkthrough,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }
                    }

                    ExamQuickActionButton(
                        config = actions.quickAction,
                        onClick = onActionClick,
                        primaryLabelOverride = if (shouldNudgeScan) "Start Scanning" else null,
                        showPulse = shouldNudgeScan
                    )
                }

                CompletionConfettiBurst(
                    visible = showCompletionCelebration && item.status == ExamStatus.COMPLETED,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(96.dp)
                        .padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun NoScanContextualNudge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Blue100.copy(alpha = 0.35f))
            .border(1.dp, Blue100, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Blue500,
                modifier = Modifier.size(14.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "No scans yet",
                    style = AppTypography.text13Medium,
                    color = Blue500
                )
                Text(
                    text = "Start scanning to unlock score insights and trends.",
                    style = AppTypography.text12Regular,
                    color = Grey600
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun NoScanWalkthroughTooltip(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val walkthroughTips = listOf(
        "Step 1: Start Scanning" to "Use the primary button to scan your first sheet.",
        "Step 2: Track Insights" to "After first scan, this card will show live score stats."
    )
    val carouselState = rememberLazyListState()
    val currentStep by remember {
        derivedStateOf {
            carouselState.firstVisibleItemIndex.coerceIn(
                0,
                walkthroughTips.size - 1
            )
        }
    }
    val tooltipBg = Grey800

    BoxWithConstraints(modifier = modifier) {
        val totalSteps = walkthroughTips.size
        val pageWidth = maxWidth

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tooltipBg, RoundedCornerShape(14.dp))
            ) {
                LazyRow(
                    state = carouselState,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    itemsIndexed(walkthroughTips) { _, tip ->
                        Column(
                            modifier = Modifier
                                .width(pageWidth)
                                .clip(RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tip.first,
                                    style = AppTypography.text12SemiBold,
                                    color = White
                                )
                                Text(
                                    text = "Dismiss",
                                    style = AppTypography.text11Medium,
                                    color = Blue100,
                                    modifier = Modifier.clickable { onDismiss() }
                                )
                            }

                            Text(
                                text = tip.second,
                                style = AppTypography.text10Regular,
                                color = White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
            Canvas(
                modifier = Modifier
                    .padding(start = 22.dp)
                    .size(width = 18.dp, height = 9.dp)
            ) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path = path, color = tooltipBg)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    val isActive = index == currentStep
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (isActive) 14.dp else 6.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isActive) Blue100 else Grey500.copy(alpha = 0.8f))
                    )
                }
            }
        }
    }
}

@Composable
fun ExamQuickActionButton(
    config: QuickActionButton?,
    onClick: (ExamAction) -> Unit,
    modifier: Modifier = Modifier,
    primaryLabelOverride: String? = null,
    showPulse: Boolean = false
) {
    if (config == null) return

    if (config.tertiaryAction != null) {
        val secondaryAction = config.secondaryAction ?: return
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GenericButton(
                    text = primaryLabelOverride ?: config.action.label,
                    type = config.style,
                    size = ButtonSize.SMALL,
                    icon = config.action.icon,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(config.action) }
                )
                GenericButton(
                    text = secondaryAction.label,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.SMALL,
                    icon = secondaryAction.icon,
                    modifier = Modifier.weight(1f),
                    onClick = { onClick(secondaryAction) }
                )
            }
            GenericButton(
                text = config.tertiaryAction.label,
                type = ButtonType.SECONDARY,
                size = ButtonSize.SMALL,
                icon = config.tertiaryAction.icon,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClick(config.tertiaryAction) }
            )
        }
        return
    }

    // Completed exam matches the reference: main outlined button + compact icon action
    if (config.style == ButtonType.SECONDARY && config.secondaryAction != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GenericButton(
                text = primaryLabelOverride ?: config.action.label,
                type = ButtonType.SECONDARY,
                size = ButtonSize.SMALL,
                icon = config.action.icon,
                modifier = Modifier.weight(1f),
                onClick = { onClick(config.action) }
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Grey200, RoundedCornerShape(12.dp))
                    .background(White)
                    .genericClick { onClick(config.secondaryAction) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.secondaryAction.icon,
                    contentDescription = config.secondaryAction.label,
                    tint = Grey600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        return
    }

    if (config.secondaryAction != null) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GenericButton(
                text = primaryLabelOverride ?: config.action.label,
                type = config.style,
                size = ButtonSize.SMALL,
                icon = config.action.icon,
                modifier = Modifier.weight(1f),
                onClick = { onClick(config.action) }
            )
            GenericButton(
                text = config.secondaryAction.label,
                type = ButtonType.SECONDARY,
                size = ButtonSize.SMALL,
                icon = config.secondaryAction.icon,
                modifier = Modifier.weight(1f),
                onClick = { onClick(config.secondaryAction) }
            )
        }
        return
    }

    if (showPulse) {
        val transition = rememberInfiniteTransition(label = "scan-ripple")
        val rippleOneScale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400),
                repeatMode = RepeatMode.Restart
            ),
            label = "rippleOneScale"
        )
        val rippleOneAlpha = transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400),
                repeatMode = RepeatMode.Restart
            ),
            label = "rippleOneAlpha"
        )
        val rippleTwoScale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.26f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, delayMillis = 420),
                repeatMode = RepeatMode.Restart
            ),
            label = "rippleTwoScale"
        )
        val rippleTwoAlpha = transition.animateFloat(
            initialValue = 0.12f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, delayMillis = 420),
                repeatMode = RepeatMode.Restart
            ),
            label = "rippleTwoAlpha"
        )

        Box(modifier = modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = rippleOneScale.value
                        scaleY = rippleOneScale.value
                        alpha = rippleOneAlpha.value
                    }
                    .clip(RoundedCornerShape(50.dp))
                    .background(Blue500)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        scaleX = rippleTwoScale.value
                        scaleY = rippleTwoScale.value
                        alpha = rippleTwoAlpha.value
                    }
                    .clip(RoundedCornerShape(50.dp))
                    .background(Blue500)
            )
            GenericButton(
                text = primaryLabelOverride ?: config.action.label,
                type = config.style,
                size = ButtonSize.SMALL,
                icon = config.action.icon,
                modifier = Modifier.fillMaxWidth(),
                onClick = { onClick(config.action) }
            )
        }
    } else {
        GenericButton(
            text = primaryLabelOverride ?: config.action.label,
            type = config.style,
            size = ButtonSize.SMALL,
            icon = config.action.icon,
            modifier = modifier.fillMaxWidth(),
            onClick = { onClick(config.action) }
        )
    }
}

@Composable
private fun ExamIcon(status: ExamStatus) {
    val bg = when (status) {
        ExamStatus.DRAFT -> Color(0xFFFFF2D6)
        ExamStatus.ACTIVE -> Color(0xFFE3F0FF)
        ExamStatus.COMPLETED -> Color(0xFFECEBFF)
        ExamStatus.ARCHIVED -> Grey200
    }
    val tint = when (status) {
        ExamStatus.DRAFT -> Color(0xFFFFB300)
        ExamStatus.ACTIVE -> Blue500
        ExamStatus.COMPLETED -> Color(0xFF6C63FF)
        ExamStatus.ARCHIVED -> Grey500
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.DateRange,
            contentDescription = "Exam Icon",
            tint = tint,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun ExamMetadata(
    totalQuestions: Int,
    createdAt: Long,
    sheetsCount: Int,
    status: ExamStatus
) {
    val formattedDate = formatTimestamp(createdAt)

    Text(
        text = "$totalQuestions questions • Created $formattedDate",
        color = Grey600,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = AppTypography.text13Medium
    )
}

@Composable
private fun LastActivityChip(lastActivityAt: Long) {
    val ageMillis = (System.currentTimeMillis() - lastActivityAt).coerceAtLeast(0L)
    val isStale = ageMillis > 7L * 24L * 60L * 60L * 1000L
    val (bg, textColor) = if (isStale) {
        Color(0xFFFFF3E0) to Color(0xFFB26A00)
    } else {
        Blue100.copy(alpha = 0.55f) to Blue500
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Last activity",
            style = AppTypography.text10Regular,
            color = textColor
        )
        Text(
            text = relativeTime(lastActivityAt),
            style = AppTypography.text10SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun DraftSetupProgressStrip(item: ExamEntity) {
    val basicDone = item.examName.isNotBlank() && item.totalQuestions > 0
    val answerKeyDone =
        (item.answerKey?.size ?: 0) >= item.totalQuestions && item.totalQuestions > 0
    val schemeDone = (item.marksPerCorrect ?: 0f) > 0f
    val steps = listOf(
        "Basic info" to basicDone,
        "Answer key" to answerKeyDone,
        "Scheme" to schemeDone
    )
    val completedCount = steps.count { it.second }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
//        Text(
//            text = "Setup progress  $completedCount/3",
//            style = AppTypography.text11Medium,
//            color = Grey600,
//            textAlign = TextAlign.Center
//        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            steps.forEach { (_, done) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (done) Blue500 else Grey200)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            steps.forEach { (label, done) ->
                Text(
                    text = label,
                    style = AppTypography.text10Regular,
                    color = if (done) Blue500 else Grey500,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ExamStats(
    avgScore: Float?,
    topScore: Float?,
    lowestScore: Float?,
    isArchived: Boolean = false
) {
    val validAvg = avgScore?.takeIf { !it.isNaN() && !it.isInfinite() }?.toCleanString()
    val validTop = topScore?.takeIf { !it.isNaN() && !it.isInfinite() }?.toCleanString()
    val validLow = lowestScore?.takeIf { !it.isNaN() && !it.isInfinite() }?.toCleanString()

    val statValueColor = if (isArchived) Grey600 else Blue500
    val statBgColor = if (isArchived) Grey200 else Color(0xFFE7F0FA)
    val statBorderColor = if (isArchived) Grey200 else Blue500.copy(alpha = 0.22f)
    val topValueColor = if (isArchived) Grey600 else Green600
    val topBgColor = if (isArchived) Grey200 else Color(0xFFEAF7EE)
    val topBorderColor = if (isArchived) Grey200 else Green600.copy(alpha = 0.22f)
    val lowValueColor = if (isArchived) Grey600 else Orange600
    val lowBgColor = if (isArchived) Grey200 else Color(0xFFFFF2E6)
    val lowBorderColor = if (isArchived) Grey200 else Orange600.copy(alpha = 0.22f)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            value = validAvg?.let { "$it%" } ?: "--",
            label = "AVG",
            valueColor = statValueColor,
            backgroundColor = statBgColor,
            borderColor = statBorderColor,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = validTop?.let { "$it%" } ?: "--",
            label = "TOP",
            valueColor = topValueColor,
            backgroundColor = topBgColor,
            borderColor = topBorderColor,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = validLow?.let { "$it%" } ?: "--",
            label = "LOW",
            valueColor = lowValueColor,
            backgroundColor = lowBgColor,
            borderColor = lowBorderColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    valueColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            style = AppTypography.text14Bold
        )
        Text(
            text = label,
            color = Grey500,
            style = AppTypography.text11Regular
        )
    }
}

@Composable
private fun StatusDetailLine(status: ExamStatus, sheetsCount: Int) {
    val (detail, textColor) = when (status) {
        ExamStatus.DRAFT -> Triple(
            Icons.Outlined.WarningAmber,
            "Incomplete setup",
            Grey800
        ) to Black

        ExamStatus.ACTIVE -> if (sheetsCount > 0) {
            Triple(Icons.Outlined.BarChart, "$sheetsCount sheets scanned", Grey600) to Grey800
        } else {
            Triple(Icons.Outlined.Description, "No sheets scanned yet", Grey600) to Grey800
        }

        ExamStatus.COMPLETED -> Triple(
            Icons.Filled.CheckCircle,
            "$sheetsCount sheets scanned",
            Grey600
        ) to Black

        ExamStatus.ARCHIVED -> Triple(
            Icons.Outlined.Description,
            "Archived • $sheetsCount sheets",
            Grey600
        ) to Grey800
    }
    val (icon, text, tint) = detail

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            color = Grey800,
            style = AppTypography.text13SemiBold
        )
    }
}

@Composable
private fun StatusBadge(
    status: ExamStatus,
    celebrationProgress: Float = 0f
) {
    val (bg, textColor) = when (status) {
        ExamStatus.DRAFT -> Color(0xFFFFF4D8) to Color(0xFF8A6A00)
        ExamStatus.ACTIVE -> Color(0xFFE5F6EA) to Color(0xFF2F8A45)
        ExamStatus.COMPLETED -> Color(0xFFE9EDFF) to Color(0xFF4F67D8)
        ExamStatus.ARCHIVED -> Grey200 to Grey600
    }
    val progress = celebrationProgress.coerceIn(0f, 1f)
    val completedBgStart = Color(0xFFE5F6EA)
    val completedTextStart = Color(0xFF2F8A45)
    val animatedBg =
        if (status == ExamStatus.COMPLETED) lerp(completedBgStart, bg, progress) else bg
    val animatedText =
        if (status == ExamStatus.COMPLETED) lerp(
            completedTextStart,
            textColor,
            progress
        ) else textColor
    val scale = if (status == ExamStatus.COMPLETED) 1f + (0.1f * progress) else 1f

    Box(
        modifier = Modifier
            .height(20.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .background(animatedBg)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.name,
            color = animatedText,
            style = AppTypography.text10Medium
        )
    }
}

@Composable
private fun CompletionConfettiBurst(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("confetti_burst.json")
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = visible,
        iterations = 1,
        restartOnPlay = true,
        speed = 1.1f
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun relativeTime(timestamp: Long): String {
    val diff = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minute = 60_000L
    val hour = 60L * minute
    val day = 24L * hour
    return when {
        diff < minute -> "just now"
        diff < hour -> "${diff / minute}m ago"
        diff < day -> "${diff / hour}h ago"
        diff < 2 * day -> "Yesterday"
        else -> "${diff / day}d ago"
    }
}
