package com.example.ledgerscanner.feature.scanner.scan.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.components.ToolbarAction
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey600
import com.example.ledgerscanner.base.utils.rememberBackHandler
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel

@Composable
fun ScanResultScreen(
    navController: NavHostController,
    omrScannerViewModel: OmrScannerViewModel,
    innerPadding: PaddingValues
) {
    val handleBack = rememberBackHandler(navController)
    val omrImageProcessResult by omrScannerViewModel.omrImageProcessResult.collectAsState()

    Scaffold(topBar = {
        GenericToolbar(
            title = "Scan Result",
            onBackClick = handleBack,
            actions = listOf(
                ToolbarAction.Icon(
                    icon = Icons.Outlined.MoreVert,
                    contentDescription = "Menu",
                    onClick = { /* Show menu */ }
                )
            )
        )
    }, containerColor = Grey100) { paddingValues ->
        omrImageProcessResult?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // OMR Sheet Preview
                OmrSheetPreview(it.finalBitmap, it.debugBitmaps)
            }
        }

    }
}

@Composable
private fun OmrSheetPreview(
    finalBitmap: Bitmap?,
    debugBitmaps: HashMap<String, Bitmap>
) {
    // Convert map → list to maintain order
    val debugList = remember(debugBitmaps) {
        debugBitmaps.values.toList()
    }

    // Build pages list
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Grey200),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // ✅ Fixed height for the pager container
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize(), // ✅ Fill the pager height
                    contentAlignment = Alignment.Center // ✅ Center the image
                ) {
                    Image(
                        bitmap = pages[page].asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit, // ✅ Fit within the box
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Page indicator text
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