package com.example.ledgerscanner.feature.scanner.statistics.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.statistics.screen.ExamStatisticsScreen
import com.example.ledgerscanner.feature.scanner.statistics.viewModel.ExamStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExamStatisticsActivity : ComponentActivity() {

    private val viewModel: ExamStatisticsViewModel by viewModels()

    private val examEntity: ExamEntity by lazy {
        intent.getParcelableExtra<ExamEntity>(EXTRA_EXAM_ENTITY)
            ?: throw IllegalStateException("ExamEntity is required")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                ExamStatisticsScreen(
                    navController = rememberNavController(),
                    examStatisticsViewModel = viewModel,
                    examEntity = examEntity,
                )
            }
        }
    }

    companion object {
        private const val EXTRA_EXAM_ENTITY = "extra_exam_entity"

        fun launchExamStatisticsScreen(context: Context, examEntity: ExamEntity): Intent {
            return Intent(context, ExamStatisticsActivity::class.java).apply {
                putExtra(EXTRA_EXAM_ENTITY, examEntity)
            }
        }
    }
}