package com.example.ledgerscanner.feature.scanner.exam.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.feature.scanner.exam.model.ExamStep
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.SaveAndNextBarWidget
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier = Modifier,
    moveToNextScreen: () -> Unit
) {
    val context = LocalContext.current
    var examName by rememberSaveable { mutableStateOf("") }
    var examDescription by rememberSaveable { mutableStateOf("") }
    var numberOfQuestionsText by rememberSaveable { mutableStateOf("") }
    var numberOfQuestions: Int? = null

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val selectedTemplate = savedStateHandle
        ?.getLiveData<Template>(CreateExamActivity.SELECTED_TEMPLATE)
        ?.observeAsState()

    val selectedTemplateName = selectedTemplate?.value?.name ?: ""

    LaunchedEffect(selectedTemplate) {
        if (selectedTemplate != null) {
            numberOfQuestionsText = ""
        }
    }

    var enabled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(examName, examDescription, selectedTemplate, numberOfQuestionsText) {
        numberOfQuestions = numberOfQuestionsText.toIntOrNull()
        enabled = examName.isNotBlank() &&
                examDescription.isNotBlank() &&
                selectedTemplate != null &&
                numberOfQuestions != null && numberOfQuestions!! > 0
    }

    val perStepState by createExamViewModel.perStepState.collectAsState()
    LaunchedEffect(perStepState) {
        if (perStepState.first == ExamStep.BASIC_INFO) {
            when (perStepState.second) {
                is OperationState.Error -> Toast.makeText(
                    context,
                    (perStepState.second as OperationState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()

                OperationState.Success -> {
                    moveToNextScreen()
                }

                OperationState.Idle -> {}
                OperationState.Loading -> {}

            }
        }
    }

    Scaffold(bottomBar = {
        val saveExam: (Boolean) -> Unit = { saveInDb ->
            createExamViewModel.saveBasicInfo(
                examName = examName,
                description = examDescription,
                template = selectedTemplate?.value!!,
                numberOfQuestions = numberOfQuestions!!,
                saveInDb = saveInDb,
            )
        }

        SaveAndNextBarWidget(
            onNext = { saveExam(false) },
            onSaveDraft = { saveExam(true) },
            enabled = enabled
        )
    }) { innerPadding ->
        Column(
            modifier = modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
        ) {
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
                        imageVector = Icons.Default.Pages,
                        contentDescription = null,
                        tint = Grey500,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                onValueChange = {},
                onClick = {
                    navController.navigate(CreateExamActivity.ROUTE_SELECT_TEMPLATE)
                }
            )

            Spacer(modifier = Modifier.Companion.height(12.dp))

            GenericTextField(
                label = "Number of questions",
                value = numberOfQuestionsText,
                placeholder = "e.g., 50",
                prefix = { Text("# ", color = Grey500) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Companion.Number),
                onValueChange = { input ->
                    val totalQues =
                        selectedTemplate?.value?.getTotalQuestions() ?: Int.MAX_VALUE

                    // Allow only digits
                    val filtered = input.filter { it.isDigit() }

                    // Parse number safely
                    val enteredValue = filtered.toIntOrNull() ?: 0

                    // Validate against total questions
                    if (enteredValue <= totalQues) {
                        numberOfQuestionsText = filtered
                    }
                },
                readOnly = (selectedTemplate?.value == null),
                modifier = Modifier.Companion.fillMaxWidth()
            )

            Spacer(modifier = Modifier.Companion.height(12.dp))

        }
    }
}