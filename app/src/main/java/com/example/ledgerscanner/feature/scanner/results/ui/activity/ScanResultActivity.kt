package com.example.ledgerscanner.feature.scanner.results.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.navigation.NavHostController
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

/**
 * Activity for displaying scan results and scanned sheets list
 *
 * Routes:
 * - SCAN_RESULT_SCREEN: Shows individual scan result details
 * - SCANNED_SHEETS_SCREEN: Shows list of all scanned sheets for an exam
 *
 * @author Monika Bhasin
 * @created 31/12/25
 */
@AndroidEntryPoint
class ScanResultActivity : BaseActivity() {

    private lateinit var navController: NavHostController
    private var examEntity: ExamEntity? = null
    private var scanResultEntity: ScanResultEntity? = null
    private var isViewMode: Boolean = false
    private var destinationScreen: String = SCANNED_SHEETS_SCREEN

    private val scanResultViewModel: ScanResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Extract intent data
        if (!extractIntentData()) {
            return // Activity will be finished if data is invalid
        }

        setContent {
            LedgerScannerTheme {
                navController = rememberNavController()

                Scaffold(
                    containerColor = White
                ) { _ ->
                    NavHost(
                        navController = navController,
                        startDestination = destinationScreen
                    ) {
                        // Scan Result Screen - shows individual scan details
                        composable(SCAN_RESULT_SCREEN) {
                            val exam = examEntity
                            val scanResult = scanResultEntity

                            if (exam == null || scanResult == null) {
                                Toast.makeText(
                                    this@ScanResultActivity,
                                    "Missing data for scan result",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                return@composable
                            }

                            ScanResultScreen(
                                navController = navController,
                                examEntity = exam,
                                scanResultEntity = scanResult,
                                scanResultViewModel = scanResultViewModel,
                                isViewMode = isViewMode
                            )
                        }

                        // Scanned Sheets Screen - shows list of all scans
                        composable(SCANNED_SHEETS_SCREEN) {
                            val exam = examEntity

                            if (exam == null) {
                                Toast.makeText(
                                    this@ScanResultActivity,
                                    "Exam not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                return@composable
                            }

                            ScannedSheetsScreen(
                                navController = navController,
                                examEntity = exam,
                                scanResultViewModel = scanResultViewModel
                            )
                        }

                        // Manual Correction Screen (optional)
                        composable(
                            route = "manual_correction/{examId}?questions={questions}",
                            arguments = listOf(
                                navArgument("examId") { type = NavType.IntType },
                                navArgument("questions") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val examId = backStackEntry.arguments?.getInt("examId") ?: 0
                            val questionsString =
                                backStackEntry.arguments?.getString("questions") ?: ""
                            val questionIndices = questionsString
                                .split(",")
                                .mapNotNull { it.toIntOrNull() }

                            // TODO: Implement ManualCorrectionScreen
                            Log.d(
                                TAG,
                                "Manual correction for exam $examId, questions: $questionIndices"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Extract and validate data from intent
     * @return true if data is valid, false otherwise (and finishes activity)
     */
    private fun extractIntentData(): Boolean {
        // Get exam entity (required for all screens)
        examEntity = intent.getParcelableExtra(ARG_EXAM_ENTITY)
        if (examEntity == null) {
            Toast.makeText(
                this,
                "Exam not found",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return false
        }

        // Get destination screen
        destinationScreen = intent.getStringExtra(ARG_DESTINATION_SCREEN)
            ?: SCANNED_SHEETS_SCREEN

        // Get scan result if navigating to scan result screen
        if (destinationScreen == SCAN_RESULT_SCREEN) {
            scanResultEntity = intent.getParcelableExtra(ARG_SCAN_RESULT_ENTITY)
            isViewMode = intent.getBooleanExtra(ARG_IS_VIEW_MODE, false)

            if (scanResultEntity == null) {
                Toast.makeText(
                    this,
                    "Scan result not found",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return false
            }
        }

        return true
    }

    /**
     * Navigate to scan result screen within this activity
     * Used when already in ScanResultActivity and want to view different scan
     */
    fun navigateToScanResult(
        examEntity: ExamEntity,
        scanResultEntity: ScanResultEntity?,
        isViewMode: Boolean
    ) {
        if (scanResultEntity == null) {
            Toast.makeText(
                this,
                "Scan result not found",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Update instance variables
        this.examEntity = examEntity
        this.scanResultEntity = scanResultEntity
        this.isViewMode = isViewMode


        // Navigate using NavController
        if (::navController.isInitialized) {
            navController.navigate(SCAN_RESULT_SCREEN) {
                // Pop up to scanned sheets to avoid building up back stack
                popUpTo(SCANNED_SHEETS_SCREEN) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    /**
     * Navigate to scanned sheets screen within this activity
     */
    fun navigateToScannedSheets(examEntity: ExamEntity) {
        this.examEntity = examEntity

        if (::navController.isInitialized) {
            navController.navigate(SCANNED_SHEETS_SCREEN) {
                popUpTo(SCANNED_SHEETS_SCREEN) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    companion object {
        private const val TAG = "ScanResultActivity"

        // Intent extra keys
        private const val ARG_EXAM_ENTITY = "exam_entity"
        private const val ARG_SCAN_RESULT_ENTITY = "scan_result_entity"
        private const val ARG_DESTINATION_SCREEN = "destination_screen"
        private const val ARG_IS_VIEW_MODE = "is_view_mode"

        // Screen routes
        const val SCAN_RESULT_SCREEN = "scan_result_screen"
        const val SCANNED_SHEETS_SCREEN = "scanned_sheets_screen"

        /**
         * Launch ScanResultScreen to show scan results
         *
         * Smart navigation:
         * - If called from ScanResultActivity: navigates within activity
         * - If called from different activity: starts new activity
         *
         * @param context The context to launch from
         * @param examEntity The exam entity
         * @param scanResultEntity The scan result to display (null for new scan from scanner)
         * @param isViewMode true if viewing saved result, false if showing fresh scan
         */
        fun launchScanResultScreen(
            context: Context,
            examEntity: ExamEntity,
            scanResultEntity: ScanResultEntity?,
            isViewMode: Boolean = false
        ) {
            when (context) {
                is ScanResultActivity -> {
                    // ✅ Same activity - navigate internally
                    context.navigateToScanResult(
                        examEntity = examEntity,
                        scanResultEntity = scanResultEntity,
                        isViewMode = isViewMode
                    )
                }

                else -> {
                    // ✅ Different activity - start new activity
                    val intent = Intent(context, ScanResultActivity::class.java).apply {
                        putExtra(ARG_EXAM_ENTITY, examEntity)
                        putExtra(ARG_DESTINATION_SCREEN, SCAN_RESULT_SCREEN)
                        putExtra(ARG_SCAN_RESULT_ENTITY, scanResultEntity)
                        putExtra(ARG_IS_VIEW_MODE, isViewMode)
                    }
                    context.startActivity(intent)
                }
            }
        }

        /**
         * Launch ScannedSheetsScreen to show all scanned sheets for an exam
         *
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
}