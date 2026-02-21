package com.example.ledgerscanner.feature.scanner.results.ui.screen

import android.content.Intent
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
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.ScoreSummaryCard
import com.example.ledgerscanner.feature.scanner.results.ui.components.result.StudentDetailsSection
import com.example.ledgerscanner.feature.scanner.results.utils.updateAnswerAndRecalculate
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
    var editableScanResult by remember(scanResultEntity) { mutableStateOf(scanResultEntity) }
    val studentDetailsRef = remember {
        mutableStateOf(StudentDetailsForScanResult(null, null, null))
    }
    val barcodeValueState = remember { mutableStateOf(scanResultEntity.barCode) }
    val barcodeLockedState = remember { mutableStateOf(!scanResultEntity.barCode.isNullOrBlank()) }
    val hasBarcode = !barcodeValueState.value.isNullOrBlank()

    val saveSheetResponse by scanResultViewModel.saveSheetState.collectAsState()
    val isSaving = saveSheetResponse is UiState.Loading


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
        if (!isSaving) {
            scanResultViewModel.resetSaveSheetState()
            scanResultViewModel.saveSheet(
                studentDetailsRef.value,
                editableScanResult,
                examEntity.id
            )
        }
    }

    LaunchedEffect(Unit, isViewMode) {
        scanResultViewModel.resetSaveSheetState()
    }

    LaunchedEffect(saveSheetResponse, isViewMode) {
        if (isViewMode) return@LaunchedEffect

        when (saveSheetResponse) {
            is UiState.Success -> {
                scanResultViewModel.resetSaveSheetState()
                handleBack()
            }
            is UiState.Error -> Unit
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            GenericToolbar(
                title = if (isViewMode) "Result Details" else "Scan Result",
                onBackClick = handleBack,
            )
        },
        bottomBar = {
            if (!isViewMode) {
                ActionButtonsSection(
                    onSaveAndContinue = {
                        saveAndContinue()
                    },
                    onRetryScan = {
                        scanResultViewModel.resetSaveSheetState()
                        handleBack()
                    },
                    isSaveEnabled = hasBarcode,
                    isActionInProgress = isSaving,
                    saveState = saveSheetResponse,
                )
            }
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
                editableScanResult.clickedRawImagePath,
                editableScanResult.scannedImagePath,
                editableScanResult.debugImagesPath
            )

            Spacer(Modifier.height(16.dp))

            StudentDetailsSection(
                barcodeId = barcodeValueState.value,
                enrollmentNumber = editableScanResult.enrollmentNumber,
                studentDetailsRef = studentDetailsRef,
                barcodeLocked = barcodeLockedState.value,
                isReadOnly = isViewMode,
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

            ScoreSummaryCard(editableScanResult, examEntity)

            Spacer(Modifier.height(8.dp))

            ReviewRequiredCard(
                scanResultEntity = editableScanResult,
                examEntity = examEntity,
                originalScanResultEntity = scanResultEntity,
                onAnswerChange = { questionIndex, selectedOption ->
                    editableScanResult = editableScanResult.updateAnswerAndRecalculate(
                        examEntity = examEntity,
                        questionIndex = questionIndex,
                        selectedOption = selectedOption
                    )
                },
                isReadOnly = isViewMode,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))
            
            QuestionDetailsSection(
                scanResultEntity = editableScanResult,
                examEntity = examEntity,
                expanded = questionDetailsExpanded,
                onToggle = { questionDetailsExpanded = !questionDetailsExpanded }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
