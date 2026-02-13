package com.example.ledgerscanner.feature.scanner.results.ui.screen

import android.content.Intent
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.utils.navigateBackOrFinish
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ActionButtonsSection
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.OmrSheetPreview
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.QuestionDetailsSection
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ReviewRequiredCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.SaveStatusDialog
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ScoreSummaryCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.StudentDetailsSection
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.BarcodeScanActivity

@Composable
fun ScanResultScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    scanResultEntity: ScanResultEntity,
    scanResultViewModel: ScanResultViewModel,
    isViewMode: Boolean,
) {
    val context = LocalContext.current
    val handleBack = rememberBackHandler(navController)

    var questionDetailsExpanded by remember { mutableStateOf(false) }
    val studentDetailsRef = remember {
        mutableStateOf(StudentDetailsForScanResult(null, null, null))
    }
    val barcodeValueState = remember { mutableStateOf(scanResultEntity.barCode) }
    val barcodeLockedState = remember { mutableStateOf(!scanResultEntity.barCode.isNullOrBlank()) }
    val hasBarcode = !barcodeValueState.value.isNullOrBlank()

    val totalSheetCounts by scanResultViewModel.sheetsCountByExamId.collectAsState()
    val saveSheetResponse by scanResultViewModel.saveSheetState.collectAsState()
    var onScanNextSelected by remember { mutableStateOf(false) }


    val barcodeScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val code = data?.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE)
        if (!code.isNullOrBlank()) {
            barcodeValueState.value = code
            barcodeLockedState.value = true
        }
    }

    val saveAndContinue: () -> Unit = {
        scanResultViewModel.saveSheet(
            studentDetailsRef.value,
            scanResultEntity,
            examEntity.id
        )
    }

    LaunchedEffect(Unit) {
        scanResultViewModel.resetSaveSheetState()
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
                    scanResultViewModel.resetSaveSheetState()
                    handleBack()
                },
                onScanNext = {
                    onScanNextSelected = true
                    saveAndContinue()
                },
                onViewAllSheets = { /* Navigate to scanned sheets */ },
                sheetCount = totalSheetCounts,
                isSaveEnabled = hasBarcode,
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
                scanResultEntity.clickedRawImagePath,
                scanResultEntity.scannedImagePath,
                scanResultEntity.debugImagesPath
            )

            Spacer(Modifier.height(16.dp))

            StudentDetailsSection(
                barcodeId = barcodeValueState.value,
                enrollmentNumber = scanResultEntity.enrollmentNumber,
                studentDetailsRef = studentDetailsRef,
                barcodeLocked = barcodeLockedState.value,
                onBarcodeChange = { value ->
                    if (!barcodeLockedState.value) {
                        barcodeValueState.value = value
                        if (value.isNotBlank()) {
                            barcodeLockedState.value = true
                        }
                    }
                },
                onScanBarcode = {
                    val intent = Intent(context, BarcodeScanActivity::class.java)
                    barcodeScanLauncher.launch(intent)
                }
            )

            Spacer(Modifier.height(16.dp))

            ScoreSummaryCard(scanResultEntity, examEntity)

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(16.dp))

            ReviewRequiredCard(
                scanResultEntity,
                onReviewClick = {
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            QuestionDetailsSection(
                scanResultEntity = scanResultEntity,
                examEntity = examEntity,
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
                    scanResultViewModel.resetSaveSheetState()
                    handleBack()
                }
            }
        )
    }
}
