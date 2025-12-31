package com.example.omrscanner.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Green400
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey800
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.exam.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.ScannedSheetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSessionScreen(
    navController: NavHostController,
    omrScannerViewModel: OmrScannerViewModel,
    scannedSheetsViewModel: ScannedSheetsViewModel,
    examEntity: ExamEntity
) {
    val context = LocalContext.current

    val handleBack = rememberBackHandler(navController)

    // Extract data from examEntity
    val examName = examEntity.examName
    val totalQuestions = examEntity.totalQuestions
    val optionsPerQuestion = examEntity.template.options_per_question

    val onViewAnswerKey: () -> Unit = {
        context.startActivity(
            Intent(
                context,
                CreateExamActivity::class.java
            ).apply {
                putExtra(
                    CreateExamActivity.CONFIG, CreateExamConfig(
                        examEntity = examEntity,
                        mode = CreateExamConfig.Mode.VIEW,
                        targetScreen = ExamStep.ANSWER_KEY
                    )
                )
            })
    }
    val onViewResults: () -> Unit = {
        navController.navigate(ScanBaseActivity.SCANNED_SHEETS_SCREEN)
    }
    val onStartScanning: () -> Unit = {
        navController.navigate(ScanBaseActivity.SCANNER_SCREEN)
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            GenericToolbar("Scan Session", onBackClick = handleBack)
        },
        bottomBar = {
            ScannerButton(onStartScanning)
        },
        containerColor = Grey100
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Exam Header
            ExamHeader(
                examName = examName,
                totalQuestions = totalQuestions,
                optionsPerQuestion = optionsPerQuestion
            )
            HorizontalDivider(
                color = Grey200,
                thickness = 1.dp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReadyState(
                scannedSheetsViewModel = scannedSheetsViewModel,
                examId = examEntity.id,
                onViewAnswerKey = onViewAnswerKey,
                onViewResults = onViewResults,
            )
        }
    }
}

@Composable
fun ScannerButton(onStartScanning: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column {
            GenericButton(
                icon = Icons.Outlined.CameraAlt,
                text = "Start Scanning",
                size = ButtonSize.LARGE,
                onClick = onStartScanning,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "💡 Tip: Ensure good lighting and include all 4 corner anchors",
                style = AppTypography.body3Regular,
                color = Grey500,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Left
            )
        }
    }


}

@Composable
private fun ExamHeader(
    examName: String,
    totalQuestions: Int,
    optionsPerQuestion: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Blue100)
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = "",
                modifier = Modifier.size(32.dp),
                tint = Blue500
            )
        }

        Column {
            Text(
                text = examName,
                style = AppTypography.h1ExtraBold,
                color = Grey800
            )
            Text(
                text = "$totalQuestions Questions • $optionsPerQuestion Options",
                style = AppTypography.body3Regular,
                color = Grey600
            )
        }
    }
}

@Composable
private fun ReadyState(
    scannedSheetsViewModel: ScannedSheetsViewModel,
    examId: Int,
    onViewAnswerKey: () -> Unit,
    onViewResults: () -> Unit
) {
    val sheetsCountByExamId by scannedSheetsViewModel.sheetsCountByExamId.collectAsState()

    LaunchedEffect(Unit) {
        scannedSheetsViewModel.getCountByExamId(examId)
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Answer Key Set Card
        StatusCard(
            icon = Icons.Filled.CheckCircle,
            iconColor = Green400,
            title = "Answer Key Set",
            linkText = "View Answer Key →",
            onLinkClick = onViewAnswerKey,
            backgroundColor = White,
            borderColor = Grey200,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scanned Sheets Card - Updated Implementation
        when (val state = sheetsCountByExamId) {
            is UiState.Loading -> {
                SheetsStatusCard(
                    sheetsCount = null,
                    isLoading = true,
                    onViewResults = onViewResults
                )
            }

            is UiState.Success -> {
                SheetsStatusCard(
                    sheetsCount = state.data,
                    isLoading = false,
                    onViewResults = onViewResults
                )
            }

            is UiState.Error -> {
                SheetsStatusCard(
                    sheetsCount = null,
                    isLoading = false,
                    errorMessage = state.message,
                    onViewResults = onViewResults
                )
            }

            is UiState.Idle<*> -> {}
        }
    }
}

@Composable
private fun SheetsStatusCard(
    sheetsCount: Int?,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    avgScore: Int? = null,
    topScore: Int? = null,
    onViewResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Grey200,
                shape = RoundedCornerShape(12.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        onClick = {
            if (sheetsCount != null && sheetsCount > 0) {
                onViewResults()
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = if (sheetsCount != null && sheetsCount > 0) Blue500 else Grey600,
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                when {
                    // Loading
                    isLoading -> {
                        Text(
                            text = "Loading...",
                            style = AppTypography.label2Medium,
                            color = Grey600
                        )
                    }

                    // Error
                    errorMessage != null -> {
                        Text(
                            text = "Error loading sheets",
                            style = AppTypography.label2Medium,
                            color = Grey900
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = errorMessage,
                            style = AppTypography.body3Regular,
                            color = Grey600
                        )
                    }

                    // No sheets
                    sheetsCount == null || sheetsCount == 0 -> {
                        Text(
                            text = "No sheets scanned yet",
                            style = AppTypography.label2Medium,
                            color = Grey900
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Ready to scan your first sheet?",
                            style = AppTypography.body3Regular,
                            color = Grey600
                        )
                    }

                    // Single sheet
                    sheetsCount == 1 -> {
                        Row {
                            Text(
                                text = "Scanned: ",
                                style = AppTypography.body3Regular,
                                color = Grey600
                            )
                            Text(
                                text = "1 sheet",
                                style = AppTypography.label2Bold,
                                color = Grey900
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "View Results →",
                            style = AppTypography.label3Medium,
                            color = Blue500
                        )
                    }

                    // Multiple sheets
                    else -> {
                        Row {
                            Text(
                                text = "Scanned: ",
                                style = AppTypography.body3Regular,
                                color = Grey600
                            )
                            Text(
                                text = "$sheetsCount sheets",
                                style = AppTypography.label2Bold,
                                color = Grey900
                            )
                        }

                        // Optional stats
                        if (avgScore != null && topScore != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Avg: $avgScore% • Top: $topScore%",
                                style = AppTypography.body4Regular,
                                color = Grey500
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "View Results →",
                            style = AppTypography.label3Medium,
                            color = Blue500
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    linkText: String,
    onLinkClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color,
    subtitle: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onLinkClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = AppTypography.body1Medium,
                    color = Grey900
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = AppTypography.h4Bold,
                        color = Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = linkText,
                style = AppTypography.body3Medium,
                color = Blue500,
                modifier = Modifier.genericClick { onLinkClick() }
            )
        }
    }
}