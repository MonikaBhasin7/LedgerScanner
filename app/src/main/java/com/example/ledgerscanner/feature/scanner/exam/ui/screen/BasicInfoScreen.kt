package com.example.ledgerscanner.feature.scanner.exam.ui.screen

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
import androidx.compose.material.icons.filled.Pages
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.utils.ui.genericClick
import com.example.ledgerscanner.feature.scanner.exam.ui.activity.CreateExamActivity
import com.example.ledgerscanner.feature.scanner.exam.ui.compose.SaveAndNextBarWidget
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Basic\nInfo", "Answer\nKey", "Marking", "Review")
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

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

    Scaffold(topBar = {
        GenericToolbar(title = "Create Exam", onBackClick = {
            navController.popBackStack()
        })
    }, bottomBar = {
        SaveAndNextBarWidget(onNext = {
            createExamViewModel.saveBasicInfo(
                examName,
                examDescription,
                selectedTemplate?.value!!,
                numberOfQuestions!!, false
            )
        }, onSaveDraft = {
            createExamViewModel.saveBasicInfo(
                examName,
                examDescription,
                selectedTemplate?.value!!,
                numberOfQuestions!!, true
            )
        }, enabled = enabled)
    }) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
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