package com.example.ledgerscanner.feature.scanner.results.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ActionButtonsSection
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.OmrSheetPreview
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.QuestionDetailsSection
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.SaveStatusDialog
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ScoreSummaryCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.StudentDetailsSection
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScannedSheetsViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult

@Composable
fun ScanResultScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    imageProcessResult: OmrImageProcessResult,
    scannedSheetsViewModel: ScannedSheetsViewModel,
) {
    val context = LocalContext.current
    val handleBack = rememberBackHandler(navController)

    var questionDetailsExpanded by remember { mutableStateOf(false) }
    val studentDetailsRef = remember {
        mutableStateOf(StudentDetailsForScanResult(null, null, null))
    }

    val totalSheetCounts by scannedSheetsViewModel.sheetsCountByExamId.collectAsState()
    val saveSheetResponse by scannedSheetsViewModel.saveSheetState.collectAsState()
    var onScanNextSelected by remember { mutableStateOf(false) }

    val saveAndContinue: () -> Unit = {
        scannedSheetsViewModel.saveSheet(
            studentDetailsRef.value,
            imageProcessResult,
            examEntity.id
        )
    }

    LaunchedEffect(Unit) {
        scannedSheetsViewModel.resetSaveSheetState()
    }

    LaunchedEffect(saveSheetResponse) {
        if (saveSheetResponse is UiState.Error) {
            Toast.makeText(
                context,
                (saveSheetResponse as UiState.Error<Long>).message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            GenericToolbar(
                title = "Scan Result",
                onBackClick = handleBack,
            )
        },
        bottomBar = {
            ActionButtonsSection(
                onSaveAndContinue = saveAndContinue,
                onRetryScan = {
                    scannedSheetsViewModel.resetSaveSheetState()
                    handleBack()
                },
                onScanNext = {
                    onScanNextSelected = true
                    saveAndContinue()
                },
                onViewAllSheets = { /* Navigate to scanned sheets */ },
                sheetCount = totalSheetCounts,
            )
        },
        containerColor = Grey100
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OmrSheetPreview(
                imageProcessResult.finalBitmap,
                imageProcessResult.debugBitmaps
            )

            Spacer(Modifier.height(16.dp))

            StudentDetailsSection(
                barcodeId = imageProcessResult.barcodeId,
                studentDetailsRef = studentDetailsRef
            )

            Spacer(Modifier.height(16.dp))

            ScoreSummaryCard(imageProcessResult.evaluation)

            Spacer(Modifier.height(16.dp))

            QuestionDetailsSection(
                evaluation = imageProcessResult.evaluation,
                expanded = questionDetailsExpanded,
                onToggle = { questionDetailsExpanded = !questionDetailsExpanded }
            )

            Spacer(Modifier.height(24.dp))
        }

        SaveStatusDialog(
            saveSheetResponse = saveSheetResponse,
            onScanNextSelected = onScanNextSelected,
            onDismiss = {
                if (saveSheetResponse is UiState.Success) {
                    scannedSheetsViewModel.resetSaveSheetState()
                    navController.popBackStack()
                }
            }
        )
    }
}