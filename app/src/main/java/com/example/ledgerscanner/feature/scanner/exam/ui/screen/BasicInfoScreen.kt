package com.example.ledgerscanner.feature.scanner.exam.ui.screen

import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.ui.components.GenericDialog
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.feature.scanner.exam.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.viewmodel.CreateExamViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier = Modifier,
    updateBottomBar: (BottomBarConfig) -> Unit
) {
    val context = LocalContext.current

    var examName by rememberSaveable { mutableStateOf("") }
    var examDescription by rememberSaveable { mutableStateOf("") }
    var numberOfQuestionsText by rememberSaveable { mutableStateOf("") }

    var showSelectTemplate by rememberSaveable { mutableStateOf(false) }
    var selectedTemplate by rememberSaveable { mutableStateOf<Template?>(null) }
    val selectedTemplateName = selectedTemplate?.name ?: ""

    // When a template is selected, clear numberOfQuestionsText so user re-enters
    LaunchedEffect(selectedTemplate) {
        if (selectedTemplate != null) {
            numberOfQuestionsText = ""
        }
    }

    val numberOfQuestions by remember {
        derivedStateOf { numberOfQuestionsText.toIntOrNull() }
    }

    val enabled by remember {
        derivedStateOf {
            examName.isNotBlank()
                    && examDescription.isNotBlank()
                    && selectedTemplate != null
                    && numberOfQuestions != null
                    && numberOfQuestions!! > 0
        }
    }

    val saveExam: (Boolean) -> Unit = { saveInDb ->
        createExamViewModel.saveBasicInfo(
            examName = examName,
            description = examDescription,
            template = selectedTemplate!!,
            numberOfQuestions = numberOfQuestions!!,
            saveInDb = saveInDb,
        )
    }

    LaunchedEffect(enabled) {
        updateBottomBar(
            BottomBarConfig(
                enabled = enabled,
                onNext = { saveExam(false) },
                onSaveDraft = { saveExam(true) }
            )
        )
    }

    Scaffold { innerPadding ->
        Box {
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GenericTextField(
                    label = "Description (optional)",
                    value = examDescription,
                    placeholder = "Short description or instructions",
                    onValueChange = { examDescription = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 120.dp),
                    singleLine = false,
                    maxLines = 5,
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                    onClick = { showSelectTemplate = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                GenericTextField(
                    label = "Number of questions",
                    value = numberOfQuestionsText,
                    placeholder = "e.g., 50",
                    prefix = { Text("# ", color = Grey500) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { input ->
                        val totalQues = selectedTemplate?.getTotalQuestions() ?: Int.MAX_VALUE

                        // Allow only digits
                        val filtered = input.filter { it.isDigit() }

                        // Parse number safely
                        val enteredValue = filtered.toIntOrNull() ?: 0

                        // Validate against total questions
                        if (enteredValue <= totalQues) {
                            numberOfQuestionsText = filtered
                        }
                    },
                    readOnly = selectedTemplate == null,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (showSelectTemplate) {
                GenericDialog {
                    SelectTemplateScreen { template ->
                        selectedTemplate = template
                        showSelectTemplate = false
                    }
                }
            }
        }
    }
}