package com.example.omrscanner.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSessionScreen(
    navController: NavHostController,
    omrScannerViewModel: OmrScannerViewModel,
    examEntity: ExamEntity
) {
    val context = LocalContext.current

    // Extract data from examEntity
    val examName = examEntity.examName
    val totalQuestions = examEntity.totalQuestions
    val optionsPerQuestion = examEntity.template.options_per_question
    val isAnswerKeySet = examEntity.answerKey != null && examEntity.answerKey.isNotEmpty()
    val scannedSheetsCount = examEntity.sheetsCount ?: 0

    // Navigation handlers
    val onBackClick: () -> Unit = { navController.popBackStack() }
    val onViewAnswerKey: () -> Unit = {
        // TODO: Navigate to answer key view screen
        // navController.navigate("answer_key_view/${examEntity.id}")
    }
    val onViewResults: () -> Unit = {
        // Navigate to all scanned sheets screen
        navController.navigate("all_scanned_sheets/${examEntity.id}")
    }
    val onStartScanning: () -> Unit = {
        // Launch camera activity for scanning
        navController.navigate(ScanBaseActivity.SCANNER_SCREEN)
    }
    val onSetAnswerKey: () -> Unit = {
        // Navigate to answer key input screen
        navController.navigate("answer_key_input/${examEntity.id}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Session",
                        style = AppTypography.h4Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFAFAFA)
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

            Spacer(modifier = Modifier.height(24.dp))

            // State Section
            if (isAnswerKeySet && scannedSheetsCount > 0) {
                // Ready State
                ReadyState(
                    scannedSheetsCount = scannedSheetsCount,
                    onViewAnswerKey = onViewAnswerKey,
                    onViewResults = onViewResults,
                    onStartScanning = onStartScanning
                )
            } else {
                // Warning State
                WarningState(
                    isAnswerKeySet = isAnswerKeySet,
                    scannedSheetsCount = scannedSheetsCount,
                    onSetAnswerKey = onSetAnswerKey,
                    onStartScanning = onStartScanning
                )
            }
        }
    }
}

@Composable
private fun ExamHeader(
    examName: String,
    totalQuestions: Int,
    optionsPerQuestion: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exam Icon
            Text(
                text = "📝",
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column {
                Text(
                    text = examName,
                    style = AppTypography.h3Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "$totalQuestions Questions • $optionsPerQuestion Options",
                    style = AppTypography.body3Regular,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
private fun ReadyState(
    scannedSheetsCount: Int,
    onViewAnswerKey: () -> Unit,
    onViewResults: () -> Unit,
    onStartScanning: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Section Label
        Text(
            text = "READY STATE",
            style = AppTypography.label4Bold,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Answer Key Set Card
        StatusCard(
            icon = "✓",
            iconColor = Color(0xFF4CAF50),
            title = "Answer Key Set",
            linkText = "View Answer Key",
            onLinkClick = onViewAnswerKey,
            backgroundColor = Color.White,
            borderColor = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scanned Sheets Card
        StatusCard(
            icon = "📊",
            iconColor = Color(0xFF757575),
            title = "Scanned: $scannedSheetsCount sheets",
            linkText = "View\nResults",
            onLinkClick = onViewResults,
            backgroundColor = Color.White,
            borderColor = Color(0xFFE0E0E0)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ready to Scan Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = Color(0xFF2196F3),
                    shape = RoundedCornerShape(12.dp)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ready to scan?",
                    style = AppTypography.label1Bold,
                    color = Color(0xFF212121)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text(
                        text = "📷",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Start Scanning",
                        style = AppTypography.label2Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip
        Text(
            text = "Tip: Ensure good lighting and include all 4 corner anchors",
            style = AppTypography.body3Regular,
            color = Color(0xFF757575),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun WarningState(
    isAnswerKeySet: Boolean,
    scannedSheetsCount: Int,
    onSetAnswerKey: () -> Unit,
    onStartScanning: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        // Section Label
        Text(
            text = "WARNING STATE",
            style = AppTypography.label4Bold,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Answer Key Warning Card
        if (!isAnswerKeySet) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = Color(0xFFFF9800),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Answer Key Not Set",
                            style = AppTypography.label2SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = onSetAnswerKey,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Set Answer Key Now",
                            style = AppTypography.label3Medium,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = " →",
                            style = AppTypography.label3Medium,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // No Sheets Scanned Card
        if (scannedSheetsCount == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFE0E0E0)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sheets scanned yet",
                        style = AppTypography.body3Regular,
                        color = Color(0xFF757575)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Setup Required Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFE0E0E0)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Setup required",
                    style = AppTypography.label2Medium,
                    color = Color(0xFF757575),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = onStartScanning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0E0E0),
                        contentColor = Color(0xFF9E9E9E)
                    ),
                    enabled = false
                ) {
                    Text(
                        text = "Start Scanning",
                        style = AppTypography.label2Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    icon: String,
    iconColor: Color,
    title: String,
    linkText: String,
    onLinkClick: () -> Unit,
    backgroundColor: Color,
    borderColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp,
                    color = iconColor,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = title,
                    style = AppTypography.label2Medium,
                    color = Color(0xFF212121)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onLinkClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = linkText,
                    style = AppTypography.label3Medium,
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = " →",
                    style = AppTypography.label3Medium,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}