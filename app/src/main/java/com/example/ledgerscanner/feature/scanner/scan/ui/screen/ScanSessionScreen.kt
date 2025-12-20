package com.example.omrscanner.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Green400
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.base.utils.ui.genericClick
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

    val handleBack = rememberBackHandler(navController)

    // Extract data from examEntity
    val examName = examEntity.examName
    val totalQuestions = examEntity.totalQuestions
    val optionsPerQuestion = examEntity.template.options_per_question
    val isAnswerKeySet = examEntity.answerKey != null && examEntity.answerKey.isNotEmpty()
    val scannedSheetsCount = examEntity.sheetsCount ?: 0

    val onViewAnswerKey: () -> Unit = {
        // TODO: Navigate to answer key view screen
        // navController.navigate("answer_key_view/${examEntity.id}")
    }
    val onViewResults: () -> Unit = {
        navController.navigate("all_scanned_sheets/${examEntity.id}")
    }
    val onStartScanning: () -> Unit = {
        navController.navigate(ScanBaseActivity.SCANNER_SCREEN)
    }
    val onSetAnswerKey: () -> Unit = {
        navController.navigate("answer_key_input/${examEntity.id}")
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            GenericToolbar("Scan Session", onBackClick = handleBack)
        }
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

            HorizontalDivider(color = Blue75, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(24.dp))

            ReadyState(
                scannedSheetsCount = scannedSheetsCount,
                onViewAnswerKey = onViewAnswerKey,
                onViewResults = onViewResults,
                onStartScanning = onStartScanning
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
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.EditNote,
            contentDescription = "",
            modifier = Modifier.size(48.dp).padding(end = 8.dp),
            tint = Blue500
        )

        Column {
            Text(
                text = examName,
                style = AppTypography.h2Bold,
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
        // Answer Key Set Card
        StatusCard(
            icon = Icons.Rounded.CheckCircle,
            iconColor = Green400,
            title = "Answer Key Set",
            linkText = "View Answer Key ->",
            onLinkClick = onViewAnswerKey,
            backgroundColor = Color.White,
            borderColor = Blue75,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scanned Sheets Card
        StatusCard(
            icon = Icons.Filled.BarChart,
            iconColor = Black,
            title = "Scanned: ",
            subtitle = "$scannedSheetsCount sheets",
            linkText = "View Results ->",
            onLinkClick = onViewResults,
            backgroundColor = Color.White,
            borderColor = Blue75
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ready to Scan Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Blue500,
                    shape = RoundedCornerShape(12.dp)
                )
                .align(Alignment.CenterHorizontally),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Ready to scan?",
                    style = AppTypography.label1Bold,
                    color = Black
                )

                Spacer(modifier = Modifier.height(16.dp))

                GenericButton(
                    icon = Icons.Outlined.CameraAlt,
                    text = "Start Scanning",
                    onClick = onStartScanning,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tip
        Text(
            text = "Tip: Ensure good lighting and include all 4 corner anchors",
            style = AppTypography.body3Regular,
            color = Grey500,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
            defaultElevation = 1.5.dp,
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        onClick = onLinkClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = title,
                    style = AppTypography.body3Medium,
                    color = Grey900
                )
                if(!subtitle.isNullOrEmpty())
                    Text(
                        text = subtitle,
                        style = AppTypography.h4Bold,
                        color = Black
                    )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = linkText,
                style = AppTypography.label3Medium,
                color = Blue500,
                modifier = Modifier.genericClick { onLinkClick() }
            )
        }
    }
}