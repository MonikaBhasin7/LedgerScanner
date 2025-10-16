package com.example.ledgerscanner.feature.scanner.exam.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.SelectTemplateScreen
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateExamActivity : ComponentActivity() {

    companion object {
        const val ROUTE_SELECT_TEMPLATE = "select_template_route"
        const val ROUTE_CREATE_EXAM = "create_exam_route"
        const val SELECTED_TEMPLATE = "selected_template"
    }

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
                            CreateExamScreen(
                                navController,
                                modifier = Modifier.Companion.padding(
                                    innerPadding
                                )
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

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun CreateExamScreen(
        navController: NavHostController,
        modifier: Modifier = Modifier.Companion
    ) {
        val steps = listOf("Basic\nInfo", "Answer\nKey", "Marking", "Review")
        var currentStep by rememberSaveable { mutableIntStateOf(0) }

        var examName by rememberSaveable { mutableStateOf("") }
        var examDescription by rememberSaveable { mutableStateOf("") }
        var numberOfQuestionsText by rememberSaveable { mutableStateOf("") }

        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        val selectedTemplate = savedStateHandle
            ?.getLiveData<Template>(SELECTED_TEMPLATE)
            ?.observeAsState()

        val selectedTemplateName = selectedTemplate?.value?.name ?: ""


        Scaffold(topBar = {
            GenericToolbar(title = "Create Exam", onBackClick = {
                navController.popBackStack()
            })
        }) { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.Companion.height(8.dp))

                StepListWidget(
                    steps = steps,
                    currentStep = currentStep,
                    onStepSelected = { index -> currentStep = index }
                )

                Spacer(modifier = Modifier.Companion.height(12.dp))

                GenericTextField(
                    label = "Exam Name",
                    value = examName,
                    placeholder = "e.g., Physics Midterm",
                    onValueChange = { examName = it },
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                Spacer(modifier = Modifier.Companion.height(12.dp))

                GenericTextField(
                    label = "Description (optional)",
                    value = examDescription,
                    placeholder = "Short description or instructions",
                    onValueChange = { examDescription = it },
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp),
                    singleLine = false,
                    maxLines = 5,
                )

                Spacer(modifier = Modifier.Companion.height(12.dp))

                GenericTextField(
                    label = "Exam to be conducted on which answer sheet",
                    value = selectedTemplateName,
                    placeholder = "No Sheet Selected",
                    prefix = {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = Grey500,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    onValueChange = {},
                    onClick = {
                        navController.navigate(ROUTE_SELECT_TEMPLATE)
                    }
                )

                Spacer(modifier = Modifier.Companion.height(12.dp))

                GenericTextField(
                    label = "Number of questions",
                    value = numberOfQuestionsText,
                    placeholder = "e.g., 50",
                    prefix = { Text("#", color = Grey500) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }
                        numberOfQuestionsText = filtered
                    },
                    modifier = Modifier.Companion.fillMaxWidth()
                )

                val numberOfQuestions: Int? = numberOfQuestionsText.toIntOrNull()
                Spacer(modifier = Modifier.Companion.height(12.dp))

            }
        }
    }

    @Composable
    private fun StepListWidget(
        steps: List<String>,
        currentStep: Int,
        onStepSelected: (Int) -> Unit,
        modifier: Modifier = Modifier.Companion
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            steps.forEachIndexed { index, label ->
                StepItem(
                    label = label,
                    isActive = index == currentStep,
                    onClick = { if (index != currentStep) onStepSelected(index) }
                )
            }
        }
    }

    @Composable
    private fun StepItem(label: String, isActive: Boolean, onClick: () -> Unit) {
        Box(
            modifier = Modifier.Companion
                .genericClick { onClick() }
                .padding(vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                Box(
                    modifier = Modifier.Companion
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Blue500 else Grey200)
                )

                Spacer(modifier = Modifier.Companion.width(8.dp))

                Text(
                    text = label,
                    style = if (isActive) AppTypography.label2Bold else AppTypography.label2SemiBold,
                    color = if (isActive) Black else Grey500
                )
            }
        }
    }
}