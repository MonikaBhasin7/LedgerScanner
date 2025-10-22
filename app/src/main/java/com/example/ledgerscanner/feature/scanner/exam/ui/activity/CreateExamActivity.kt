package com.example.ledgerscanner.feature.scanner.exam.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.components.GenericRectangularLoader
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.StepListWidget
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.AnswerKeyScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.BasicInfoScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.SelectTemplateScreen
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateExamActivity : ComponentActivity() {

    companion object {
        const val ROUTE_SELECT_TEMPLATE = "select_template_route"
        const val ROUTE_BASIC_INFO = "basic_info_route"
        const val ROUTE_ANSWER_KEY = "answer_key_route"
        const val SELECTED_TEMPLATE = "selected_template"
    }

    private val createExamViewModel: CreateExamViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val perStepState by createExamViewModel.perStepState.collectAsState()

            LedgerScannerTheme {
                Scaffold(
                    topBar = {
                        GenericToolbar(title = "Create Exam", onBackClick = {
                            finish()
                        })
                    }
                ) { innerPadding ->
                    Box {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            StepListWidget(
                                steps = ExamStep.entries,
                                currentStep = perStepState.first,
                                onStepSelected = {}
                            )

                            Spacer(modifier = Modifier.Companion.height(12.dp))

                            NavHost(
                                navController,
                                startDestination = ROUTE_BASIC_INFO
                            ) {
                                composable(ROUTE_BASIC_INFO) {
                                    BasicInfoScreen(
                                        navController,
                                        createExamViewModel,
                                        modifier = Modifier,
                                        moveToNextScreen = {
                                            createExamViewModel.updateStepState(
                                                ExamStep.BASIC_INFO.next(),
                                                OperationState.Idle
                                            )
                                            navController.navigate(ROUTE_ANSWER_KEY)
                                        }
                                    )
                                }
                                composable(ROUTE_ANSWER_KEY) {
                                    AnswerKeyScreen(
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

                        if (perStepState.second is OperationState.Loading)
                            GenericRectangularLoader()
                    }
                }
            }
        }
    }
}