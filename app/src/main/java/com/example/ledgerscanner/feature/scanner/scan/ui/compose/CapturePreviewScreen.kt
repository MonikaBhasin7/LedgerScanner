package com.example.ledgerscanner.feature.scanner.scan.ui.compose

import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat.enableEdgeToEdge
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel

@Composable
fun CapturePreviewScreen(
    navController: NavHostController,
    omrScannerViewModel: OmrScannerViewModel
) {
    val omrImageProcessResult by omrScannerViewModel.omrImageProcessResult.collectAsState()
    var showDialog by remember { mutableStateOf<Boolean>(true) }

    Scaffold(
        modifier = Modifier
            .safeDrawingPadding(),
        topBar = {
            GenericToolbar(title = "Capture Preview", onBackClick = {
                navController.popBackStack()
            })
        },
        bottomBar = {
            GenericButton(
                "Submit",
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            omrImageProcessResult?.finalBitmap?.asImageBitmap()?.let {
                Image(
                    bitmap = it,
                    contentDescription = "image",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No image available",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (showDialog)
                WarpedImageDialog(
                    warpedBitmap = omrImageProcessResult?.finalBitmap,
                    intermediateBitmaps = omrImageProcessResult?.debugBitmaps,
                    onDismiss = {
                        showDialog = false
                    },
                )
        }
    }

}