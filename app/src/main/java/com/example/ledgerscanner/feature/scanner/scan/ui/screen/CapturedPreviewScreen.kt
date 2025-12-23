//package com.example.ledgerscanner.feature.scanner.scan.ui.screen
//
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.asImageBitmap
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import com.example.ledgerscanner.BuildConfig
//import com.example.ledgerscanner.base.ui.components.GenericToolbar
//import com.example.ledgerscanner.base.ui.theme.AppTypography
//import com.example.ledgerscanner.base.ui.theme.White
//import com.example.ledgerscanner.feature.scanner.scan.ui.compose.ScoreSummaryCard
//import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
//import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
//
//@Composable
//fun CapturedPreviewScreen(
//    navController: NavHostController,
//    omrScannerViewModel: OmrScannerViewModel,
//    innerPadding: PaddingValues
//) {
//    val omrImageProcessResult by omrScannerViewModel.omrImageProcessResult.collectAsState()
//    var showDialog by remember { mutableStateOf<Boolean>(true) }
//
//    Scaffold(
//        containerColor = White,
//        topBar = {
//            GenericToolbar(title = "Capture Preview", onBackClick = {
//                navController.popBackStack()
//            })
//        },
////        bottomBar = {
////            GenericButton(
////                "Submit",
////                onClick = {},
////                modifier = Modifier
////                    .fillMaxWidth()
////                    .padding(innerPadding)
////                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
////            )
////        }
//    ) { innerPadding ->
//        Box(modifier = Modifier.padding(innerPadding)) {
//            Column {
//                InfoBanner(text = "For testing purposes, option 2 is assumed to be the correct answer for all questions. Each correct answer is awarded 1 mark, while incorrect answers receive 0 marks.")//todo monika remove in future
//                Box(modifier = Modifier.weight(1f)) {
//                    omrImageProcessResult?.finalBitmap?.asImageBitmap()?.let {
//                        Image(
//                            bitmap = it,
//                            contentDescription = "image",
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .padding(12.dp),
//                            alignment = Alignment.Center,
//                            contentScale = ContentScale.Fit
//                        )
//                    } ?: run {
//                        Box(
//                            modifier = Modifier.fillMaxSize(),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "No image available",
//                                style = AppTypography.body1Regular
//                            )
//                        }
//                    }
//                }
//                omrImageProcessResult?.evaluation?.let { evaluation ->
//                    ScoreSummaryCard(
//                        evaluation = evaluation,
//                        modifier = Modifier // Optional modifier
//                    )
//                }
//            }
//
//
//            if (showDialog && BuildConfig.ENABLE_IMAGE_LOGS)
//                WarpedImageDialog(
//                    warpedBitmap = omrImageProcessResult?.finalBitmap,
//                    intermediateBitmaps = omrImageProcessResult?.debugBitmaps,
//                    onDismiss = {
//                        showDialog = false
//                    },
//                )
//        }
//    }
//
//}
//
//@Composable
//private fun InfoBanner(text: String) {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 8.dp)
//            .background(
//                color = MaterialTheme.colorScheme.surfaceVariant,
//            ),
//        contentAlignment = Alignment.Center
//    ) {
//        Text(
//            text = text,
//            style = AppTypography.body2Medium,
//            color = MaterialTheme.colorScheme.onSurfaceVariant,
//            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
//            textAlign = TextAlign.Center
//        )
//    }
//}