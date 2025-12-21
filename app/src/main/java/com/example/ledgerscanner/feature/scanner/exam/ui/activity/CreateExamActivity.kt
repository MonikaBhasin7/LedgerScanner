package com.example.ledgerscanner.feature.scanner.exam.ui.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.components.GenericRectangularLoader
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.feature.scanner.exam.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.SaveAndNextBarWidget
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.StepListWidget
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.AnswerKeyScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.BasicInfoScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.MarkingDefaultsScreen
import com.example.ledgerscanner.feature.scanner.exam.ui.screen.ReviewScreen
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateExamActivity : ComponentActivity() {
    private val createExamViewModel: CreateExamViewModel by viewModels()
    private val config: CreateExamConfig? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(CONFIG, CreateExamConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(CONFIG)
        }
    }

    companion object {
        const val CONFIG = "create_exam_config"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        config?.let {
            createExamViewModel.setExamEntity(it.examEntity)
        }

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()
            val perStepState by createExamViewModel.perStepState.collectAsState()
            var bottomBarConfig by remember { mutableStateOf(BottomBarConfig()) }

            LaunchedEffect(perStepState) {
                when (val state = perStepState.second) {
                    is OperationState.Error -> {
                        val errMsg = state.message
                        Toast.makeText(context, errMsg, Toast.LENGTH_SHORT).show()
                    }

                    OperationState.Idle -> {
                    }

                    OperationState.Loading -> {
                    }

                    OperationState.Success -> {
                        if (perStepState.first == ExamStep.REVIEW) {
                            finish()
                            return@LaunchedEffect
                        }
                        navController.navigate(perStepState.first.next().title)
                    }
                }
            }

            LedgerScannerTheme {
                Scaffold(topBar = {
                    GenericToolbar(title = "Create Exam", onBackClick = { finish() })
                }, bottomBar = {
                    SaveAndNextBarWidget(
                        enabled = bottomBarConfig.enabled,
                        buttonText = bottomBarConfig.buttonText,
                        onNext = {
                            bottomBarConfig.onNext()
                        },
                    )
                }) { innerPadding ->
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

                            Spacer(modifier = Modifier.height(12.dp))

                            HandleNavigation(navController, config, updateBottomBar = {
                                bottomBarConfig = it
                            })
                        }

                        if (perStepState.second is OperationState.Loading) {
                            GenericRectangularLoader()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HandleNavigation(
        navController: NavHostController,
        config: CreateExamConfig?,
        updateBottomBar: (BottomBarConfig) -> Unit
    ) {
        val startDestination = config?.targetScreen?.title ?: ExamStep.BASIC_INFO.title
        val viewMode = config?.mode ?: CreateExamConfig.Mode.VIEW

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(ExamStep.BASIC_INFO.title) {
                createExamViewModel.updateStepState(
                    ExamStep.BASIC_INFO,
                    OperationState.Idle
                )
                BasicInfoScreen(
                    navController = navController,
                    createExamViewModel = createExamViewModel,
                    updateBottomBar = updateBottomBar,
                    viewMode = viewMode,
                    modifier = Modifier
                )
            }

            composable(ExamStep.ANSWER_KEY.title) {
                createExamViewModel.updateStepState(
                    ExamStep.ANSWER_KEY,
                    OperationState.Idle
                )
                AnswerKeyScreen(
                    navController = navController,
                    createExamViewModel = createExamViewModel,
                    updateBottomBar = updateBottomBar,
                    viewMode = viewMode,
                    modifier = Modifier
                )
            }

            composable(ExamStep.MARKING.title) {
                createExamViewModel.updateStepState(
                    ExamStep.MARKING,
                    OperationState.Idle
                )
                MarkingDefaultsScreen(
                    createExamViewModel = createExamViewModel,
                    updateBottomBar = updateBottomBar,
                    viewMode = viewMode,
                    modifier = Modifier
                )
            }

            composable(ExamStep.REVIEW.title) {
                createExamViewModel.updateStepState(
                    ExamStep.REVIEW,
                    OperationState.Idle
                )
                ReviewScreen(
                    createExamViewModel = createExamViewModel,
                    updateBottomBar = updateBottomBar,
                    onEditStep = { step ->
                        navController.navigate(step.title) {
                            createExamViewModel.updateStepState(
                                step,
                                OperationState.Idle
                            )
                            popUpTo(step.title) { inclusive = true }
                        }
                    },
                    viewMode = viewMode,
                    modifier = Modifier
                )
            }
        }
    }
}