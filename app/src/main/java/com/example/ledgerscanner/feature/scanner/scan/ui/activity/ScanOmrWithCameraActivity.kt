package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.compose.CapturedPreviewScreen
import com.example.ledgerscanner.feature.scanner.scan.ui.compose.ScannerScreen
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class ScanOmrWithCameraActivity : BaseActivity() {

    companion object {
        const val TAG = "ScanOmrWithCamera"
        const val ARG_TEMPLATE = "template"
        const val SCANNER_SCREEN = "scanner_screen"
        const val CAPTURE_PREVIEW_SCREEN = "capture_preview_screen"
    }

    lateinit var omrTemplate: Template
    private val omrScannerViewModel: OmrScannerViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        omrTemplate = intent.getParcelableExtra(ARG_TEMPLATE) ?: run {
            finish()
            return
        }

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    content = { innerPadding ->
                        val navController = rememberNavController()
                        NavHost(navController, startDestination = SCANNER_SCREEN) {
                            composable(SCANNER_SCREEN) {
                                ScannerScreen(navController, omrScannerViewModel, omrTemplate)
                            }
                            composable(CAPTURE_PREVIEW_SCREEN) { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("id")
                                CapturedPreviewScreen(
                                    navController,
                                    omrScannerViewModel,
                                    innerPadding
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}