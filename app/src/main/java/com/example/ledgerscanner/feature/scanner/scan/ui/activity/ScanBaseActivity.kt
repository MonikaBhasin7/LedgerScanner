package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
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
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.ui.activity.ScanResultActivity
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import com.example.ledgerscanner.feature.scanner.scan.ui.screen.ScannerScreen
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
import com.example.omrscanner.ui.screens.ScanSessionScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScanBaseActivity : BaseActivity() {

    companion object {
        const val TAG = "ScanOmrWithCamera"
        const val ARG_TEMPLATE = "template"
        const val ARG_EXAM_ENTITY = "exam_entity"
        const val SCANNER_SCREEN = "scanner_screen"
        const val SCANNER_SESSION_SCREEN = "scan_session_screen"
    }

    private var examEntity: ExamEntity? = null

    private val omrScannerViewModel: OmrScannerViewModel by viewModels()
    private val scanResultViewModel: ScanResultViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        examEntity = intent.getParcelableExtra(ARG_EXAM_ENTITY) ?: run {
            Toast.makeText(
                this@ScanBaseActivity, "Exam not found!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    content = { innerPadding ->
                        val navController = rememberNavController()
                        NavHost(navController, startDestination = SCANNER_SESSION_SCREEN) {
                            composable(SCANNER_SESSION_SCREEN) {
                                ScanSessionScreen(
                                    navController,
                                    scanResultViewModel,
                                    examEntity!!,
                                    onViewResults = {
                                        Intent(
                                            this@ScanBaseActivity,
                                            ScanResultActivity::class.java
                                        ).apply {
                                            putExtra(ScanResultActivity.ARG_EXAM_ENTITY, examEntity)
                                            putExtra(
                                                ScanResultActivity.ARG_DESTINATION_SCREEN,
                                                ScanResultActivity.SCANNED_SHEETS_SCREEN
                                            )
                                        }.apply {
                                            startActivity(this)
                                        }
                                    }
                                )
                            }
                            composable(SCANNER_SCREEN) {
                                ScannerScreen(
                                    navController,
                                    omrScannerViewModel,
                                    examEntity!!
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}