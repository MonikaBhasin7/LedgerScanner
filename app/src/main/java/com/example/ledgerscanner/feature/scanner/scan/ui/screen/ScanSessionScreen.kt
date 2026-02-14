package com.example.ledgerscanner.feature.scanner.scan.ui.screen

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue50
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Green50
import com.example.ledgerscanner.base.ui.theme.Green600
import com.example.ledgerscanner.base.ui.theme.Orange600
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity

private enum class SessionActionType {
    ANSWER_KEY,
    RESULTS,
    CHECKLIST
}

@Composable
fun ScanSessionScreen(
    navController: NavHostController,
    scanResultViewModel: ScanResultViewModel,
    examEntity: ExamEntity,
    onViewResults: () -> Unit
) {
    val context = LocalContext.current
    val handleBack = rememberBackHandler(navController)
    val sheetsCountByExamId by scanResultViewModel.sheetsCountByExamId.collectAsState()

    LaunchedEffect(examEntity.id) {
        scanResultViewModel.getCountByExamId(examEntity.id)
    }

    val scannedSheets = (sheetsCountByExamId as? UiState.Success)?.data ?: 0
    val hasTemplate = examEntity.template.questions.isNotEmpty()

    val onViewAnswerKey: () -> Unit = {
        context.startActivity(
            Intent(context, CreateExamActivity::class.java).apply {
                putExtra(
                    CreateExamActivity.CONFIG,
                    CreateExamConfig(
                        examEntity = examEntity,
                        mode = CreateExamConfig.Mode.VIEW,
                        targetScreen = ExamStep.ANSWER_KEY
                    )
                )
            }
        )
    }

    val onStartScanning: () -> Unit = {
        if (!hasTemplate) {
            Toast.makeText(
                context,
                "Template is missing. Please update the exam.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            navController.navigate(ScanBaseActivity.SCANNER_SCREEN) {
                launchSingleTop = true
            }
        }
    }

    BackHandler(onBack = handleBack)

    val sections = listOf(
        SessionActionType.ANSWER_KEY,
        SessionActionType.RESULTS,
        SessionActionType.CHECKLIST
    )

    Scaffold(
        topBar = {
            GenericToolbar(title = "Scan Session", onBackClick = handleBack)
        },
        bottomBar = {
            SessionBottomBar(
                hasTemplate = hasTemplate,
                onStartScanning = onStartScanning
            )
        },
        containerColor = Grey100
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SessionOverviewCard(
                    examEntity = examEntity,
                    scannedSheets = scannedSheets,
                    hasTemplate = hasTemplate
                )
            }

            item {
                Text(
                    text = "Session Controls",
                    style = AppTypography.text14SemiBold,
                    color = Grey900,
                    modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                )
            }

            items(sections) { section ->
                when (section) {
                    SessionActionType.ANSWER_KEY -> {
                        SessionActionCard(
                            icon = Icons.Filled.CheckCircle,
                            iconTint = Green600,
                            title = "Answer Key",
                            subtitle = "Review configured correct options before scanning.",
                            badgeText = "Ready",
                            badgeBackground = Green50,
                            badgeTextColor = Green600,
                            actionText = "Open Answer Key",
                            onClick = onViewAnswerKey
                        )
                    }

                    SessionActionType.RESULTS -> {
                        ResultsActionCard(
                            state = sheetsCountByExamId,
                            onOpen = onViewResults
                        )
                    }

                    SessionActionType.CHECKLIST -> {
                        ScanChecklistCard()
                    }
                }
            }

            item {
                Box(modifier = Modifier.height(76.dp))
            }
        }
    }
}

@Composable
private fun SessionOverviewCard(
    examEntity: ExamEntity,
    scannedSheets: Int,
    hasTemplate: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = listOf(Blue75, White)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Blue100)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = Blue500,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = examEntity.examName,
                        style = AppTypography.text16SemiBold,
                        color = Grey900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasTemplate) {
                            "Everything is set for this scan session"
                        } else {
                            "Template setup is incomplete"
                        },
                        style = AppTypography.text12Regular,
                        color = Grey700
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(text = "${examEntity.totalQuestions} Questions")
                InfoChip(text = "${examEntity.template.options_per_question} Options")
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactStatCard(
                    label = "Sheets Scanned",
                    value = scannedSheets.toString(),
                    icon = Icons.Outlined.BarChart,
                    valueColor = Blue500,
                    modifier = Modifier.weight(1f)
                )

                CompactStatCard(
                    label = "Template",
                    value = if (hasTemplate) "Ready" else "Missing",
                    icon = Icons.Outlined.TaskAlt,
                    valueColor = if (hasTemplate) Green600 else Orange600,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label,
                    style = AppTypography.text10Medium,
                    color = Grey600
                )
                Text(
                    text = value,
                    style = AppTypography.text13SemiBold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun ResultsActionCard(
    state: UiState<Int>,
    onOpen: () -> Unit
) {
    val count = (state as? UiState.Success)?.data ?: 0
    val hasResults = count > 0

    val title = when (state) {
        is UiState.Loading, is UiState.Idle<*> -> "Checking scanned sheets"
        is UiState.Error -> "Could not load scanned sheets"
        is UiState.Success -> if (hasResults) "$count sheets scanned" else "No sheets scanned yet"
    }

    val subtitle = when (state) {
        is UiState.Loading, is UiState.Idle<*> -> "Please wait while scan summary is loading."
        is UiState.Error -> state.message
        is UiState.Success -> if (hasResults) {
            "Open results to review performance and details."
        } else {
            "Start scanning to generate results."
        }
    }

    SessionActionCard(
        icon = Icons.Outlined.BarChart,
        iconTint = if (hasResults) Blue500 else Grey600,
        title = title,
        subtitle = subtitle,
        badgeText = if (hasResults) "Active" else "Empty",
        badgeBackground = if (hasResults) Blue50 else Grey200,
        badgeTextColor = if (hasResults) Blue500 else Grey600,
        actionText = if (hasResults) "Open Results" else "Results unavailable",
        enabled = hasResults,
        onClick = onOpen
    )
}

@Composable
private fun SessionActionCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBackground: Color,
    badgeTextColor: Color,
    actionText: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = {
            if (enabled) onClick()
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = AppTypography.text14SemiBold,
                        color = Grey900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ActionStateBadge(
                    text = badgeText,
                    backgroundColor = badgeBackground,
                    textColor = badgeTextColor
                )
            }

            Text(
                text = subtitle,
                style = AppTypography.text12Regular,
                color = Grey700
            )

            Text(
                text = if (enabled) "$actionText  ->" else actionText,
                style = AppTypography.text12SemiBold,
                color = if (enabled) Blue500 else Grey500
            )
        }
    }
}

@Composable
private fun ActionStateBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.text11SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun ScanChecklistCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Grey200, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Capture Checklist",
                    style = AppTypography.text14SemiBold,
                    color = Grey900
                )
            }

            ChecklistLine("Keep all four corner anchors visible")
            ChecklistLine("Avoid shadows and extreme perspective")
            ChecklistLine("Hold device steady for a sharp image")
        }
    }
}

@Composable
private fun ChecklistLine(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .clip(RoundedCornerShape(50))
                .background(Blue500)
        )
        Text(
            text = text,
            style = AppTypography.text12Regular,
            color = Grey700
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Blue50)
            .border(1.dp, Blue100, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = AppTypography.text11SemiBold,
            color = Blue500
        )
    }
}

@Composable
private fun SessionBottomBar(
    hasTemplate: Boolean,
    onStartScanning: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .background(White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GenericButton(
                icon = Icons.Outlined.CameraAlt,
                text = "Start Scanning",
                size = ButtonSize.LARGE,
                onClick = onStartScanning,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = if (hasTemplate) {
                    "Tip: Keep the sheet flat and include all 4 corner anchors."
                } else {
                    "Template is missing. Configure the answer key before scanning."
                },
                style = AppTypography.text12Regular,
                color = Grey500
            )
        }
    }
}
