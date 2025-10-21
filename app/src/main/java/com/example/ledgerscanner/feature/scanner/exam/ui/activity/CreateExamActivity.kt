package com.example.ledgerscanner.feature.scanner.exam.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.BasicInfoScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.SelectTemplateScreen
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateExamActivity : ComponentActivity() {

    companion object {
        const val ROUTE_SELECT_TEMPLATE = "select_template_route"
        const val ROUTE_CREATE_EXAM = "create_exam_route"
        const val SELECTED_TEMPLATE = "selected_template"
    }

    private val createExamViewModel: CreateExamViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                ) { innerPadding ->
                    val navController = rememberNavController()
                    NavHost(
                        navController,
                        startDestination = ROUTE_CREATE_EXAM
                    ) {
                        composable(ROUTE_CREATE_EXAM) {
                            BasicInfoScreen(
                                navController,
                                createExamViewModel,
                                modifier = Modifier
                            )
                        }
                        composable(ROUTE_SELECT_TEMPLATE) {
                            SelectTemplateScreen(
                                navController
                            )
                        }
                    }
                }
            }
        }
    }
}