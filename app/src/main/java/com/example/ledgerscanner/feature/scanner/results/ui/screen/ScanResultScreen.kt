package com.example.ledgerscanner.feature.scanner.results.ui.screen

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.components.ButtonSize
import com.example.ledgerscanner.base.ui.components.ButtonType
import com.example.ledgerscanner.base.ui.components.ErrorDialog
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.LoadingDialog
import com.example.ledgerscanner.base.ui.components.SuccessDialog
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Blue75
import com.example.ledgerscanner.base.ui.theme.Green500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.ui.theme.Grey700
import com.example.ledgerscanner.base.ui.theme.Grey900
import com.example.ledgerscanner.base.ui.theme.Orange500
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.AnswerModel
import com.example.ledgerscanner.feature.scanner.results.model.AnswerStatus
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import com.example.ledgerscanner.feature.scanner.results.model.StudentDetailsForScanResult
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
    val studentDetailsRef =
        remember { mutableStateOf(StudentDetailsForScanResult(null, null, null)) }

    val totalSheetCounts by scannedSheetsViewModel.sheetsCountByExamId.collectAsState()
    val saveAndContinue: () -> Unit = {
        val details = studentDetailsRef.value
        scannedSheetsViewModel.saveSheet(
            details,
            imageProcessResult,
            examEntity.id
        )
    }

    val saveSheetResponse by scannedSheetsViewModel.saveSheetState.collectAsState()

    var onScanNextSelected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scannedSheetsViewModel.resetSaveSheetState()
    }

    LaunchedEffect(saveSheetResponse) {
        when (saveSheetResponse) {
            is UiState.Error -> {
                Toast.makeText(
                    context,
                    (saveSheetResponse as UiState.Error<Long>).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is UiState.Idle -> {}
            is UiState.Loading -> {}
            is UiState.Success -> {}
        }
    }

    Scaffold(topBar = {
        GenericToolbar(
            title = "Scan Result",
            onBackClick = handleBack,
        )
    }, bottomBar = {
        ActionButtonsSection(
            onSaveAndContinue = {
                saveAndContinue()
            },
            onRetryScan = {
                scannedSheetsViewModel.resetSaveSheetState()
                handleBack()
            },
            onScanNext = {
                onScanNextSelected = true
                saveAndContinue()
            },
            onViewAllSheets = {

            },
            sheetCount = totalSheetCounts,
        )
    }, containerColor = Grey100) { paddingValues ->
        imageProcessResult?.let {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // OMR Sheet Preview
                OmrSheetPreview(it.finalBitmap, it.debugBitmaps)
                Spacer(Modifier.height(16.dp))

                // Student Details
                StudentDetailsSection(
                    imageProcessResult?.barcodeId,
                    studentDetailsRef = studentDetailsRef
                )

                Spacer(Modifier.height(16.dp))

                // Score Summary
                ScoreSummaryCard(
                    imageProcessResult?.evaluation
                )

                Spacer(Modifier.height(16.dp))

                // Question Details
                QuestionDetailsSection(
                    evaluation = imageProcessResult?.evaluation,
                    expanded = questionDetailsExpanded,
                    onToggle = { questionDetailsExpanded = !questionDetailsExpanded }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
        SaveStatusDialog(saveSheetResponse, onScanNextSelected, {
            if (saveSheetResponse is UiState.Success) {
                scannedSheetsViewModel.resetSaveSheetState()
                navController.popBackStack()
            }
        })
    }

//    LaunchedEffect(Unit) {
//        omrScannerViewModel.setCapturedResult(
//            OmrImageProcessResult(
//                success = true,
//                reason = null,
//                finalBitmap = null, // You'll need to provide actual bitmap
////                debugBitmaps = hashMapOf(
////                    "bubbles_detected" to bitmap1, // Provide actual bitmaps
////                    "full_image_with_anchors" to bitmap2,
////                    "roi_anchor_3" to bitmap3,
////                    "warped" to bitmap4,
////                    "roi_anchor_1" to bitmap5,
////                    "roi_anchor_2" to bitmap6,
////                    "image_proxy_gray_mat" to bitmap7,
////                    "roi_anchor_0" to bitmap8
////                ),
//                detectedBubbles = listOf(
//                    BubbleResult(AnchorPoint(239.5, 699.5), 0, 0, 0.8959336754836162),
//                    BubbleResult(AnchorPoint(302.5, 731.5), 1, 1, 0.8881958152388472),
//                    BubbleResult(AnchorPoint(366.5, 762.5), 2, 2, 0.885063824187393),
//                    BubbleResult(AnchorPoint(303.5, 794.5), 3, 1, 0.8895380971180419),
//                    BubbleResult(AnchorPoint(365.5, 825.5), 4, 2, 0.883668903803132),
//                    BubbleResult(AnchorPoint(303.5, 856.5), 5, 1, 0.8988024740097381),
//                    BubbleResult(AnchorPoint(365.5, 888.5), 6, 2, 0.8917752335833662),
//                    BubbleResult(AnchorPoint(240.5, 919.5), 7, 0, 0.9093038557704961),
//                    BubbleResult(AnchorPoint(303.5, 950.5), 8, 1, 0.9050401368601132),
//                    BubbleResult(AnchorPoint(366.5, 982.5), 9, 2, 0.9046190288195816),
//                    BubbleResult(AnchorPoint(303.5, 1014.5), 10, 1, 0.9032241084353204),
//                    BubbleResult(AnchorPoint(366.5, 1045.5), 11, 2, 0.9040136860113173),
//                    BubbleResult(AnchorPoint(302.5, 1077.5), 12, 1, 0.9007764179497302),
//                    BubbleResult(AnchorPoint(240.5, 1108.5), 13, 0, 0.9072246348203711),
//                    BubbleResult(AnchorPoint(366.5, 1139.5), 14, 2, 0.8946440321094881),
//                    BubbleResult(AnchorPoint(302.5, 1170.5), 15, 1, 0.8996710093433347),
//                    BubbleResult(AnchorPoint(367.5, 1202.5), 16, 2, 0.8902223976839058),
//                    BubbleResult(AnchorPoint(240.5, 1233.5), 17, 0, 0.9123305698118174),
//                    BubbleResult(AnchorPoint(240.5, 1266.5), 18, 0, 0.9214896696933808),
//                    BubbleResult(AnchorPoint(303.5, 1297.5), 19, 1, 0.9173838662981971),
//                    BubbleResult(AnchorPoint(241.5, 1328.5), 20, 0, 0.9353072772733254),
//                    BubbleResult(AnchorPoint(304.5, 1360.5), 21, 1, 0.9373864982234504),
//                    BubbleResult(AnchorPoint(240.5, 1391.5), 22, 0, 0.6227661534412423),
//                    BubbleResult(AnchorPoint(303.5, 1391.5), 22, 1, 0.6292670088169496),
//                    BubbleResult(AnchorPoint(366.5, 1391.5), 22, 2, 0.940334254507172),
//                    BubbleResult(AnchorPoint(240.5, 1422.5), 23, 0, 0.6225555994209764),
//                    BubbleResult(AnchorPoint(302.5, 1423.5), 23, 1, 0.9552835899460456),
//                    BubbleResult(AnchorPoint(429.5, 1422.5), 23, 3, 0.608659034083432),
//                    BubbleResult(AnchorPoint(240.5, 1455.5), 24, 0, 0.9588893275430977),
//                    BubbleResult(AnchorPoint(303.5, 1454.5), 24, 1, 0.6584550598762995),
//                    BubbleResult(AnchorPoint(429.5, 1454.5), 24, 3, 0.6238715620476378),
//                    BubbleResult(AnchorPoint(743.5, 699.5), 25, 1, 0.8676931175154625),
//                    BubbleResult(AnchorPoint(804.5, 730.5), 26, 2, 0.8670614554546651),
//                    BubbleResult(AnchorPoint(742.5, 762.5), 27, 1, 0.8718252401631794),
//                    BubbleResult(AnchorPoint(679.5, 794.5), 28, 0, 0.8636662718778787),
//                    BubbleResult(AnchorPoint(742.5, 826.5), 29, 1, 0.8740623766285037),
//                    BubbleResult(AnchorPoint(805.5, 857.5), 30, 2, 0.887590472430583),
//                    BubbleResult(AnchorPoint(743.5, 889.5), 31, 1, 0.8832214765100671),
//                    BubbleResult(AnchorPoint(681.5, 920.5), 32, 0, 0.8845111198841953),
//                    BubbleResult(AnchorPoint(742.5, 949.5), 33, 1, 0.895091459402553),
//                    BubbleResult(AnchorPoint(806.5, 982.5), 34, 2, 0.8927227266745624),
//                    BubbleResult(AnchorPoint(743.5, 1014.5), 35, 1, 0.8942755625740229),
//                    BubbleResult(AnchorPoint(680.5, 1045.5), 36, 0, 0.8937491775233584),
//                    BubbleResult(AnchorPoint(742.5, 1078.5), 37, 1, 0.8916699565732333),
//                    BubbleResult(AnchorPoint(807.5, 1109.5), 38, 2, 0.8915909988156336),
//                    BubbleResult(AnchorPoint(742.5, 1139.5), 39, 1, 0.8834320305303329),
//                    BubbleResult(AnchorPoint(681.5, 1171.5), 40, 0, 0.889564416370575),
//                    BubbleResult(AnchorPoint(743.5, 1203.5), 41, 1, 0.9002237136465324),
//                    BubbleResult(AnchorPoint(806.5, 1234.5), 42, 2, 0.8991446242926701),
//                    BubbleResult(AnchorPoint(679.5, 1265.5), 43, 0, 0.8808001052770101),
//                    BubbleResult(AnchorPoint(743.5, 1297.5), 44, 1, 0.9043558362942492),
//                    BubbleResult(AnchorPoint(871.5, 1298.5), 44, 3, 0.8075536254770365),
//                    BubbleResult(AnchorPoint(743.5, 1328.5), 45, 1, 0.9033820239505198),
//                    BubbleResult(AnchorPoint(744.5, 1360.5), 46, 1, 0.9265692854322938),
//                    BubbleResult(AnchorPoint(869.5, 1359.5), 46, 3, 0.6094486116594289),
//                    BubbleResult(AnchorPoint(680.5, 1391.5), 47, 0, 0.9302276615344124),
//                    BubbleResult(AnchorPoint(743.5, 1391.5), 47, 1, 0.6364258455059877),
//                    BubbleResult(AnchorPoint(869.5, 1391.5), 47, 3, 0.6163968943282011),
//                    BubbleResult(AnchorPoint(680.5, 1423.5), 48, 0, 0.9349388077378602),
//                    BubbleResult(AnchorPoint(743.5, 1422.5), 48, 1, 0.6497697065403343),
//                    BubbleResult(AnchorPoint(869.5, 1422.5), 48, 3, 0.6217660218449796),
//                    BubbleResult(AnchorPoint(680.5, 1454.5), 49, 0, 0.942755625740229),
//                    BubbleResult(AnchorPoint(743.5, 1454.5), 49, 1, 0.6696933807079879),
//                    BubbleResult(AnchorPoint(869.5, 1454.5), 49, 3, 0.6345834978286616)
//                ),
//                evaluation = EvaluationResult(
//                    correctnessMarks = listOf(
//                        false, false, false, false, false, false, false, true, false, false,
//                        false, false, false, true, false, false, false, true, true, false,
//                        true, false, false, false, false, false, false, false, true, false,
//                        false, false, true, false, false, false, true, false, false, false,
//                        true, false, false, true, false, false, false, false, false, false
//                    ),
//                    correctCount = 10,
//                    incorrectCount = 40,
//                    unansweredCount = 0,
//                    multipleMarksQuestions = listOf(22, 23, 24, 44, 46, 47, 48, 49),
//                    totalQuestions = 50,
//                    marksObtained = 70.0f,
//                    maxMarks = 50.0f,
//                    percentage = 100.0f,
//                    answerMap = mapOf(
//                        0 to AnswerModel(mutableListOf(0), -1),
//                        1 to AnswerModel(mutableListOf(1), 0),
//                        2 to AnswerModel(mutableListOf(2), 0),
//                        3 to AnswerModel(mutableListOf(1), 0),
//                        4 to AnswerModel(mutableListOf(2), 0),
//                        5 to AnswerModel(mutableListOf(1), 0),
//                        6 to AnswerModel(mutableListOf(2), 0),
//                        7 to AnswerModel(mutableListOf(0), 0),
//                        8 to AnswerModel(mutableListOf(1), 0),
//                        9 to AnswerModel(mutableListOf(2), 0),
//                        10 to AnswerModel(mutableListOf(1), 0),
//                        11 to AnswerModel(mutableListOf(2), 0),
//                        12 to AnswerModel(mutableListOf(1), 0),
//                        13 to AnswerModel(mutableListOf(0), 0),
//                        14 to AnswerModel(mutableListOf(2), 0),
//                        15 to AnswerModel(mutableListOf(1), 0),
//                        16 to AnswerModel(mutableListOf(2), 0),
//                        17 to AnswerModel(mutableListOf(0), 0),
//                        18 to AnswerModel(mutableListOf(0), 0),
//                        19 to AnswerModel(mutableListOf(1), 0),
//                        20 to AnswerModel(mutableListOf(0), 0),
//                        21 to AnswerModel(mutableListOf(1), 0),
//                        22 to AnswerModel(mutableListOf(0, 1, 2), 0),
//                        23 to AnswerModel(mutableListOf(0, 1, 3), 0),
//                        24 to AnswerModel(mutableListOf(0, 1, 3), 0),
//                        25 to AnswerModel(mutableListOf(1), 0),
//                        26 to AnswerModel(mutableListOf(2), 0),
//                        27 to AnswerModel(mutableListOf(1), 0),
//                        28 to AnswerModel(mutableListOf(0), 0),
//                        29 to AnswerModel(mutableListOf(1), 0),
//                        30 to AnswerModel(mutableListOf(2), 0),
//                        31 to AnswerModel(mutableListOf(1), 0),
//                        32 to AnswerModel(mutableListOf(0), 0),
//                        33 to AnswerModel(mutableListOf(1), 0),
//                        34 to AnswerModel(mutableListOf(2), 0),
//                        35 to AnswerModel(mutableListOf(1), 0),
//                        36 to AnswerModel(mutableListOf(0), 0),
//                        37 to AnswerModel(mutableListOf(1), 0),
//                        38 to AnswerModel(mutableListOf(2), 0),
//                        39 to AnswerModel(mutableListOf(1), 0),
//                        40 to AnswerModel(mutableListOf(0), 0),
//                        41 to AnswerModel(mutableListOf(1), 0),
//                        42 to AnswerModel(mutableListOf(2), 0),
//                        43 to AnswerModel(mutableListOf(0), 0),
//                        44 to AnswerModel(mutableListOf(1, 3), 0),
//                        45 to AnswerModel(mutableListOf(1), 0),
//                        46 to AnswerModel(mutableListOf(1, 3), 0),
//                        47 to AnswerModel(mutableListOf(0, 1, 3), 0),
//                        48 to AnswerModel(mutableListOf(0, 1, 3), 0),
//                        49 to AnswerModel(mutableListOf(0, 1, 3), 0)
//                    )
//                )
//            )
//        )
//    }
}

@Composable
private fun SaveStatusDialog(
    saveSheetResponse: UiState<Long>,
    onScanNextSelected: Boolean,
    onDismiss: () -> Unit
) {
    when (saveSheetResponse) {
        is UiState.Idle -> {
            // No dialog
        }

        is UiState.Loading -> {
            LoadingDialog(message = "Saving scan result...")
        }

        is UiState.Success -> {
            if(onScanNextSelected) {
                onDismiss()
            } else {
                SuccessDialog(
                    message = "Scan saved successfully!",
                    onDismiss = onDismiss
                )
            }
        }

        is UiState.Error -> {
            ErrorDialog(
                message = saveSheetResponse.message,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onSaveAndContinue: () -> Unit,
    onRetryScan: () -> Unit,
    onScanNext: () -> Unit,
    onViewAllSheets: () -> Unit,
    sheetCount: UiState<Int>,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            GenericButton(
                text = "Save & Continue",
                onClick = onSaveAndContinue,
                size = ButtonSize.LARGE,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GenericButton(
                    text = "🔄 Retry Scan",
                    onClick = onRetryScan,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f)
                )

                GenericButton(
                    text = "📷 Scan Next",
                    onClick = onScanNext,
                    type = ButtonType.SECONDARY,
                    size = ButtonSize.MEDIUM,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            when (sheetCount) {
                is UiState.Error -> {}
                is UiState.Loading -> {}
                is UiState.Success -> {
                    Text(
                        text = "View All Scanned Sheets (${sheetCount.data})",
                        style = AppTypography.label3Bold,
                        color = Blue500,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAllSheets() },
                        textAlign = TextAlign.Center
                    )
                }

                is UiState.Idle -> {}
            }

        }
    }
}

@Composable
private fun QuestionDetailsSection(
    evaluation: EvaluationResult?,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    evaluation?.let { evaluation ->
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.elevatedCardElevation(1.dp),
            onClick = onToggle
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Details",
                        style = AppTypography.label2Bold,
                        color = Grey900
                    )

                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Grey600
                    )
                }

                // Question items
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val itemsToShow = if (expanded) {
                        evaluation.answerMap.entries.toList()
                    } else {
                        evaluation.answerMap.entries.take(3) // Show first 3 items
                    }

                    itemsToShow.forEach { (questionNumber, answerModel) ->
                        QuestionDetailItem(questionNumber, answerModel)
                    }

                    // Show "View more" hint when collapsed
                    if (!expanded && evaluation.answerMap.size > 3) {
                        Text(
                            text = "Tap to view ${evaluation.answerMap.size - 3} more...",
                            style = AppTypography.body3Regular,
                            color = Blue500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionDetailItem(
    questionNumber: Int,
    answerModel: AnswerModel
) {
    val status = answerModel.getStatus()

    val (backgroundColor, textColor, icon) = when (status) {
        AnswerStatus.CORRECT -> Triple(Green500.copy(alpha = 0.1f), Green500, "✓")
        AnswerStatus.INCORRECT -> Triple(Red500.copy(alpha = 0.1f), Red500, "✗")
        AnswerStatus.UNANSWERED -> Triple(Grey200, Grey600, "—")
        AnswerStatus.MULTIPLE_MARKS -> Triple(Orange500.copy(alpha = 0.1f), Orange500, "⚠")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = AppTypography.body2Medium,
                color = textColor
            )

            Text(
                text = "Q${questionNumber + 1}",
                style = AppTypography.body3Medium,
                color = Grey900
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (answerModel.isAttempted()) {
                    answerModel.userSelected.joinToString(", ") { getOptionLetter(it) }
                } else {
                    "Not answered"
                },
                style = AppTypography.body3Regular,
                color = if (answerModel.isAttempted()) textColor else Grey600
            )

            if (answerModel.correctAnswer != -1 && !answerModel.isCorrect()) {
                Text(
                    text = "→ ${getOptionLetter(answerModel.correctAnswer)}",
                    style = AppTypography.body3Regular,
                    color = Green500
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

private fun getOptionLetter(index: Int): String = ('A' + index).toString()

@Composable
private fun StudentDetailsSection(
    barcodeId: String?,
    studentDetailsRef: MutableState<StudentDetailsForScanResult>
) {
    var studentName by remember { mutableStateOf<String?>(null) }
    var rollNumber by remember { mutableStateOf<Int?>(null) }
    var barcodeId by remember { mutableStateOf(barcodeId) }

    LaunchedEffect(studentName, rollNumber, barcodeId) {
        studentDetailsRef.value = StudentDetailsForScanResult(
            studentName,
            rollNumber,
            barcodeId
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Student Details",
                style = AppTypography.label2Bold,
                color = Grey900
            )

            Spacer(Modifier.height(12.dp))

            // Name field
            GenericTextField(
                value = studentName ?: "",
                label = "Name",
                onValueChange = {
                    studentName = it
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Roll number field
            GenericTextField(
                value = rollNumber?.toString() ?: "",
                label = "Roll number",
                onValueChange = {
                    rollNumber = it.toIntOrNull()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Barcode id
            GenericTextField(
                value = barcodeId ?: "",
                label = "Barcode Id",
                onValueChange = {
                    barcodeId = it
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ScoreSummaryCard(evaluation: EvaluationResult?) {
    evaluation?.let { evaluation ->
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Blue75),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Blue75,
                                White
                            )
                        )
                    )
                    .padding(16.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Large score
                    Text(
                        text = "${evaluation.marksObtained}/${evaluation.maxMarks}",
                        style = AppTypography.title2ExtraBold,
                        color = Black,
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "(${evaluation.percentage}%)",
                        style = AppTypography.h4Bold,
                        color = Blue500
                    )

                    Spacer(Modifier.height(16.dp))

                    HorizontalDivider(thickness = 0.3.dp)

                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreStat(
                            icon = Icons.Outlined.Check,
                            count = evaluation.correctCount,
                            label = "Correct",
                            color = Green500
                        )

                        ScoreStat(
                            icon = Icons.Outlined.Cancel,
                            count = evaluation.incorrectCount,
                            label = "Wrong",
                            color = Red500
                        )

                        ScoreStat(
                            icon = Icons.Outlined.RadioButtonUnchecked,
                            count = evaluation.unansweredCount,
                            label = "Blank",
                            color = Grey600
                        )

                        ScoreStat(
                            icon = Icons.Outlined.Warning,
                            count = evaluation.multipleMarksQuestions.size,
                            label = "Multiple\nAnswers",
                            color = Orange500
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreStat(
    icon: ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = count.toString(),
                style = AppTypography.h3ExtraBold,
                color = color
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = AppTypography.body3Regular,
            color = Grey700
        )
    }
}

@Composable
private fun OmrSheetPreview(
    finalBitmap: Bitmap?,
    debugBitmaps: HashMap<String, Bitmap>
) {
    val debugList = remember(debugBitmaps) {
        debugBitmaps.values.toList()
    }

    val pages: List<Bitmap> = remember(finalBitmap, debugList) {
        buildList {
            finalBitmap?.let { add(it) }
            addAll(debugList)
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .wrapContentSize(unbounded = true)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = Grey200),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .width(250.dp)
                        .height(300.dp)
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = pages[page].asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "${pagerState.currentPage + 1} / ${pages.size}",
                    style = AppTypography.body3Regular,
                    color = Grey600
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Tap to zoom",
                    style = AppTypography.body3Regular,
                    color = Grey600
                )
            }
        }
    }
}