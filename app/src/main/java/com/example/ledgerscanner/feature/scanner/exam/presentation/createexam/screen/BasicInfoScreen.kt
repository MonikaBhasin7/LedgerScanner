package com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.screen

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.ledgerscanner.base.network.OperationState
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.feature.scanner.exam.domain.model.BottomBarConfig
import com.example.ledgerscanner.feature.scanner.exam.domain.model.CreateExamConfig
import com.example.ledgerscanner.feature.scanner.exam.presentation.templateselection.TemplateSelectionActivity
import com.example.ledgerscanner.feature.scanner.exam.presentation.createexam.viewmodel.CreateExamViewModel
import com.example.ledgerscanner.feature.scanner.scan.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BasicInfoScreen(
    navController: NavHostController,
    createExamViewModel: CreateExamViewModel,
    modifier: Modifier = Modifier,
    updateBottomBar: (BottomBarConfig) -> Unit,
    viewMode: CreateExamConfig.Mode
) {
    val context = LocalContext.current
    var examName by rememberSaveable { mutableStateOf("") }
    var examDescription by rememberSaveable { mutableStateOf("") }
    var numberOfQuestionsText by rememberSaveable { mutableStateOf("") }

    var selectedTemplate by rememberSaveable { mutableStateOf<Template?>(null) }
    val selectedTemplateName = selectedTemplate?.name ?: ""
    var autofill by remember { mutableStateOf(false) }

    val examEntity by createExamViewModel.examEntity.collectAsState()
    val hasScannedSheets by createExamViewModel.hasScannedSheets.collectAsState()
    var initialTemplate by remember { mutableStateOf<Template?>(null) }

    LaunchedEffect(Unit) {
        createExamViewModel.syncTemplatesInBackground()
        examEntity?.let {
            autofill = true
            examName = it.examName
            examDescription = it.description ?: ""
            numberOfQuestionsText = it.totalQuestions.toString()
            selectedTemplate = it.template
            initialTemplate = it.template
        }
    }

    // When template is changed by user, force question count re-entry.
    LaunchedEffect(selectedTemplate) {
        if (selectedTemplate != null && selectedTemplate != initialTemplate) {
            numberOfQuestionsText = ""
        }
    }

    val templatePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val data = result.data ?: return@rememberLauncherForActivityResult
        val template = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(
                TemplateSelectionActivity.EXTRA_SELECTED_TEMPLATE,
                Template::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(TemplateSelectionActivity.EXTRA_SELECTED_TEMPLATE)
        }
        if (template != null) {
            selectedTemplate = template
        }
    }

    val numberOfQuestions by remember {
        derivedStateOf { numberOfQuestionsText.toIntOrNull() }
    }

    val enabled by remember {
        derivedStateOf {
            examName.isNotBlank()
                    && selectedTemplate != null
                    && numberOfQuestions != null
                    && numberOfQuestions!! > 0
        }
    }


    LaunchedEffect(enabled) {
        updateBottomBar(
            BottomBarConfig(
                enabled = enabled,
                onNext = {
                    if (autofill && examEntity != null) {
                        val desc = examDescription.ifBlank { null }
                        if (examEntity?.examName != examName
                            || examEntity?.description != desc
                            || examEntity?.template != selectedTemplate
                            || examEntity?.totalQuestions != numberOfQuestions
                        ) {
                            createExamViewModel.saveBasicInfo(
                                examName = examName,
                                description = desc,
                                template = selectedTemplate!!,
                                numberOfQuestions = numberOfQuestions!!,
                            )
                        } else {
                            createExamViewModel.changeOperationState(OperationState.Success)
                        }
                    } else {
                        createExamViewModel.saveBasicInfo(
                            examName = examName,
                            description = examDescription.ifBlank { null },
                            template = selectedTemplate!!,
                            numberOfQuestions = numberOfQuestions!!,
                        )
                    }
                },
            )
        )
    }

    Box {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GenericTextField(
                label = "Exam Name",
                value = examName,
                placeholder = "e.g., Physics Midterm",
                onValueChange = { examName = it },
                modifier = Modifier.fillMaxWidth()
            )

            GenericTextField(
                label = "Description (optional)",
                value = examDescription,
                placeholder = "Short description or instructions",
                onValueChange = { examDescription = it },
                modifier = Modifier
                    .fillMaxWidth(),
                singleLine = false,
                maxLines = 5,
            )

            Box(modifier = Modifier.padding(top = 6.dp)) {
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
                    onClick = if (hasScannedSheets) null else {
                        {
                            templatePickerLauncher.launch(
                                Intent(context, TemplateSelectionActivity::class.java)
                            )
                        }
                    }
                )
            }

            Box(modifier = Modifier.padding(top = 6.dp)) {
                GenericTextField(
                    label = "Number of questions",
                    value = numberOfQuestionsText,
                    placeholder = "e.g., 50",
                    prefix = { Text("# ", color = Grey500) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onValueChange = { input ->
                        if (hasScannedSheets) return@GenericTextField
                        val totalQues = selectedTemplate?.getTotalQuestions() ?: Int.MAX_VALUE

                        // Allow only digits, strip leading zeros
                        val filtered = input.filter { it.isDigit() }.trimStart('0')

                        if (filtered.isEmpty()) {
                            numberOfQuestionsText = ""
                            return@GenericTextField
                        }

                        // Parse number safely
                        val enteredValue = filtered.toIntOrNull() ?: return@GenericTextField

                        // Validate against total questions
                        if (enteredValue in 1..totalQues) {
                            numberOfQuestionsText = filtered
                        }
                    },
                    readOnly = selectedTemplate == null || hasScannedSheets,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
