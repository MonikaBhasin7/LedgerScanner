package com.example.ledgerscanner.feature.scanner.results.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.ui.screen.ScanResultScreen
import com.example.ledgerscanner.feature.scanner.results.ui.screen.ScannedSheetsScreen
import com.example.ledgerscanner.feature.scanner.results.viewmodel.ScanResultViewModel
import dagger.hilt.android.AndroidEntryPoint

// ===========================================================================
// 👤 Author: Monika Bhasin
// 📅 Created: 31/12/25
// ===========================================================================

@AndroidEntryPoint
class ScanResultActivity : BaseActivity() {

    companion object {
        const val TAG = "ScanResultActivity"
        private const val ARG_EXAM_ENTITY = "exam_entity"
        private const val ARG_DESTINATION_SCREEN = "destination_screen"
        private const val ARG_IMAGE_PROCESS_RESULT = "image_process_result"
        private const val SCAN_RESULT_SCREEN = "scan_result_screen"
        private const val SCANNED_SHEETS_SCREEN = "scanned_sheets_screen"

        /**
         * Launch ScanResultScreen to show scan results
         * @param context The context to launch from
         * @param examEntity The exam entity
         * @param imageProcessResult The OMR scan result to display
         */
        fun launchScanResultScreen(
            context: Context,
            examEntity: ExamEntity,
            scanResultEntity: ScanResultEntity?
        ) {
            val intent = Intent(context, ScanResultActivity::class.java).apply {
                putExtra(ARG_EXAM_ENTITY, examEntity)
                putExtra(ARG_DESTINATION_SCREEN, SCAN_RESULT_SCREEN)
                putExtra(ARG_IMAGE_PROCESS_RESULT, scanResultEntity)
            }
            context.startActivity(intent)
        }

        /**
         * Launch ScannedSheetsScreen to show all scanned sheets for an exam
         * @param context The context to launch from
         * @param examEntity The exam entity
         */
        fun launchScannedSheetsScreen(
            context: Context,
            examEntity: ExamEntity
        ) {
            val intent = Intent(context, ScanResultActivity::class.java).apply {
                putExtra(ARG_EXAM_ENTITY, examEntity)
                putExtra(ARG_DESTINATION_SCREEN, SCANNED_SHEETS_SCREEN)
            }
            context.startActivity(intent)
        }
    }

    private var examEntity: ExamEntity? = null
    private var scanResultEntity: ScanResultEntity? = null
    private var destinationScreen: String = SCANNED_SHEETS_SCREEN // Default
    private val scanResultViewModel: ScanResultViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        examEntity = intent.getParcelableExtra(ARG_EXAM_ENTITY) ?: run {
            Toast.makeText(
                this@ScanResultActivity, "Exam not found!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        destinationScreen = intent.getStringExtra(ARG_DESTINATION_SCREEN)
            ?: SCANNED_SHEETS_SCREEN


        if (destinationScreen == SCAN_RESULT_SCREEN) {
            scanResultEntity = intent.getParcelableExtra(ARG_IMAGE_PROCESS_RESULT) ?: run {
                Toast.makeText(
                    this@ScanResultActivity, "Having issue in processing the image!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return
            }
        }

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    content = { _ ->
                        val navController = rememberNavController()
                        NavHost(navController, startDestination = destinationScreen) {
                            composable(SCAN_RESULT_SCREEN) { backStackEntry ->
                                ScanResultScreen(
                                    navController,
                                    examEntity!!,
                                    scanResultEntity!!,
                                    scanResultViewModel
                                )
                            }
                            composable(SCANNED_SHEETS_SCREEN) {
                                ScannedSheetsScreen(
                                    navController,
                                    examEntity!!,
                                    scanResultViewModel
                                )
                            }
                            composable(
                                route = "manual_correction/{examId}?questions={questions}",
                                arguments = listOf(
                                    navArgument("examId") { type = NavType.IntType },
                                    navArgument("questions") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->
                                val examId = backStackEntry.arguments?.getInt("examId") ?: 0
                                val questionsString =
                                    backStackEntry.arguments?.getString("questions") ?: ""
                                val questionIndices =
                                    questionsString.split(",").mapNotNull { it.toIntOrNull() }
                            }
                        }
                    }
                )
            }
        }
    }
}