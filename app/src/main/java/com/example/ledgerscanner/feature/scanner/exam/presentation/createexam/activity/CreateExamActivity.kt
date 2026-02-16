package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ledgerscanner.feature.scanner.exam.domain.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.component.SaveAndNextBarWidget
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.component.StepListWidget
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen.AnswerKeyScreen
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen.BasicInfoScreen
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen.MarkingDefaultsScreen
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen.ReviewScreen
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.CreateExamViewModel
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
            var showExitDialog by remember { mutableStateOf(false) }

            BackHandler {
                if (perStepState.first != ExamStep.BASIC_INFO) {
                    val prevStep = perStepState.first.prev()
                    createExamViewModel.updateStepState(prevStep, OperationState.Idle)
                    navController.popBackStack()
                } else {
                    showExitDialog = true
                }
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Discard changes?") },
                    text = { Text("You have unsaved progress. Are you sure you want to exit?") },
                    confirmButton = {
                        TextButton(onClick = {
                            showExitDialog = false
                            finish()
                        }) { Text("Discard") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                    }
                )
            }

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
                        val currentStep = perStepState.first
                        if (currentStep == ExamStep.REVIEW) {
                            finish()
                            return@LaunchedEffect
                        }
                        val nextStep = currentStep.next()
                        createExamViewModel.updateStepState(nextStep, OperationState.Idle)
                        navController.navigate(nextStep.title)
                    }
                }
            }

            LedgerScannerTheme {
                Scaffold(topBar = {
                    GenericToolbar(title = getToolbarTitle(), onBackClick = {
                        if (perStepState.first != ExamStep.BASIC_INFO) {
                            val prevStep = perStepState.first.prev()
                            createExamViewModel.updateStepState(prevStep, OperationState.Idle)
                            navController.popBackStack()
                        } else {
                            showExitDialog = true
                        }
                    })
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
                                onStepSelected = { index ->
                                    val targetStep = ExamStep.entries[index]
                                    if (targetStep.ordinal < perStepState.first.ordinal) {
                                        createExamViewModel.updateStepState(
                                            targetStep,
                                            OperationState.Idle
                                        )
                                        navController.navigate(targetStep.title) {
                                            popUpTo(targetStep.title) { inclusive = true }
                                        }
                                    }
                                }
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

    fun getToolbarTitle(): String {
        if (config?.mode == CreateExamConfig.Mode.VIEW && config?.targetScreen != null) {
            return config?.targetScreen?.title ?: "Create Exam"
        }
        return "Create Exam"
    }

    @Composable
    fun HandleNavigation(
        navController: NavHostController,
        config: CreateExamConfig?,
        updateBottomBar: (BottomBarConfig) -> Unit
    ) {
        val startDestination = config?.targetScreen?.title ?: ExamStep.BASIC_INFO.title
        val viewMode = config?.mode ?: CreateExamConfig.Mode.EDIT

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