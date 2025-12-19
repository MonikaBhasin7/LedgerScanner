package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericTextField
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.Red50
import com.example.ledgerscanner.base.ui.theme.Red500
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.image.ImageUtils
import com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.CreateTemplateViewModel
import com.google.gson.GsonBuilder

class CreateTemplateActivity : BaseActivity() {

    private val createTemplateViewModel: CreateTemplateViewModel by viewModels()

    companion object {
        const val ROUTE_PICK_IMAGE = "pick_image"
        const val TEMPLATE_JSON = "template_json"
        const val ROUTE_VIEW_JSON = "view_json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(containerColor = White) { innerPadding ->
                    Box(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController, startDestination = ROUTE_PICK_IMAGE) {
                            composable(ROUTE_PICK_IMAGE) { PickTemplateImageScreen(navController) }
                            composable(ROUTE_VIEW_JSON) {
                                val jsonArg = navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.get<String>(TEMPLATE_JSON)
                                TemplateJsonViewer(navController, jsonArg)
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------------- MAIN SCREEN ----------------------

    @Composable
    fun PickTemplateImageScreen(navController: NavHostController) {
        val context = LocalContext.current
        val selectedBitmap by createTemplateViewModel.pickedBitmap.collectAsState()
        val templateProcessingResult by createTemplateViewModel.templateResult.collectAsState()

        val pickImageLauncher =
            createActivityLauncherComposeSpecific(ActivityResultContracts.GetContent()) { uri ->
                uri?.let {
                    val bitmap = ImageUtils.loadBitmapCorrectOrientation(
                        context, uri, reqWidth = 1080, reqHeight = 1920
                    )
                    createTemplateViewModel.setBitmap(bitmap)
                }
            }

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 📷 Image / Preview Section
                TemplatePreviewSection(
                    selectedBitmap = selectedBitmap,
                    result = templateProcessingResult,
                    pickImageLauncher = { pickImageLauncher.launch("image/*") },
                    clearResult = { createTemplateViewModel.setTemplateResult(null) },
                    modifier = Modifier.weight(1f)
                )

                // 🔘 Buttons Section
                TemplateButtonsSection(
                    selectedBitmap = selectedBitmap,
                    result = templateProcessingResult,
                    onReselect = {
                        createTemplateViewModel.setTemplateResult(null)
                        createTemplateViewModel.setBitmap(null)
                    },
                    onProcess = { questionsPerColumn, numberOfColumns ->
                        val result =
                            selectedBitmap?.let {
                                TemplateProcessor().generateTemplateJson(
                                    it,
                                    questionsPerColumn = questionsPerColumn,
                                    numberOfColumns = numberOfColumns
                                )
                            }
                        createTemplateViewModel.setTemplateResult(result)
                    },
                    onViewJson = { json ->
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            TEMPLATE_JSON,
                            json
                        )
                        navController.navigate(ROUTE_VIEW_JSON)
                    }
                )
            }

            // 🚨 Error message overlay
            templateProcessingResult?.let {
                if (!it.success && !it.reason.isNullOrEmpty()) {
                    ErrorBanner(it.reason)
                }
            }
        }
    }

    // ---------------------- UI COMPONENTS ----------------------

    @Composable
    private fun TemplatePreviewSection(
        selectedBitmap: Bitmap?,
        result: OmrTemplateResult?,
        pickImageLauncher: () -> Unit,
        clearResult: () -> Unit,
        modifier: Modifier
    ) {
        var showDebuggableDialog by remember { mutableStateOf(true) }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            when {
                result != null && result.success && result.finalBitmap != null -> {
                    Image(
                        bitmap = result.finalBitmap.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        filterQuality = FilterQuality.High
                    )

                    if (BuildConfig.ENABLE_IMAGE_LOGS && showDebuggableDialog)
                        WarpedImageDialog(
                            warpedBitmap = result.finalBitmap,
                            intermediateBitmaps = result.debugBitmaps,
                            onDismiss = { showDebuggableDialog = false }
                        )
                }

                selectedBitmap != null -> {
                    Image(
                        bitmap = selectedBitmap.asImageBitmap(),
                        contentDescription = "Selected image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                else -> EmptyPickerPrompt(onPickClick = pickImageLauncher)
            }
        }
    }

    @Composable
    private fun TemplateButtonsSection(
        selectedBitmap: Bitmap?,
        result: OmrTemplateResult?,
        onReselect: () -> Unit,
        onProcess: (Int, Int) -> Unit,
        onViewJson: (String) -> Unit
    ) {
        var numberOfQuestions by remember { mutableStateOf<Int?>(null) }
        var numberOfQuestionsError by remember { mutableStateOf<String?>(null) }

        var numberOfColumns by remember { mutableStateOf<Int?>(null) }
        result?.let {
            if (it.success && it.finalBitmap != null && !it.templateJson.isNullOrEmpty()) {
                GenericButton(
                    text = "View Json",
                    enabled = selectedBitmap != null,
                    modifier = Modifier
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    onClick = { onViewJson(it.templateJson) }
                )
            }
        }


        if (result == null && selectedBitmap != null) {
            GenericTextField(
                label = "Questions per column",
                value = (numberOfQuestions ?: "").toString(),
                placeholder = "e.g., 50",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    numberOfQuestions = filtered.toIntOrNull()

                    // Clear error when user starts typing
                    if (numberOfQuestionsError != null && filtered.isNotEmpty()) {
                        numberOfQuestionsError = null
                    }
                },
                isError = numberOfQuestionsError != null,
                supportingText = numberOfQuestionsError?.let {
                    { Text(it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )

            GenericTextField(
                label = "Number of Columns",
                value = (numberOfColumns ?: "").toString(),
                placeholder = "e.g., 2",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }
                    numberOfColumns = filtered.toIntOrNull()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }


        GenericButton(
            text = if (result != null) "Reselect another Image" else "Process template",
            enabled = selectedBitmap != null,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            onClick = {
                if (result != null) onReselect() else {
                    when {
                        numberOfQuestions == null -> {
                            numberOfQuestionsError = "Please enter number of questions"
                        }

                        numberOfQuestions!! <= 0 -> {
                            numberOfQuestionsError = "Number of questions must be greater than 0"
                        }

                        else -> {
                            numberOfQuestionsError = null
                            onProcess(numberOfQuestions!!, numberOfColumns ?: 1)
                        }
                    }
                }
            }
        )
    }

    @Composable
    private fun EmptyPickerPrompt(onPickClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onPickClick() }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Blue100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = Blue500,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Pick Image from Gallery",
                style = AppTypography.body1Regular,
                color = Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap to choose the template image.\nMake sure the sheet fills the frame.",
                style = AppTypography.body3Regular,
                color = Grey500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TipChip(text = "Auto perspective", color = Color(0xFF0EA5A4))
                TipChip(text = "Avoid glare", color = Color(0xFFF97316))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TipChip(text = "High contrast", color = Color(0xFF3B82F6))
        }
    }

    @Composable
    private fun ErrorBanner(message: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Red50)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                message,
                color = Red500,
                modifier = Modifier.padding(12.dp),
                style = AppTypography.label1Medium
            )
        }
    }

    @Composable
    fun TemplateJsonViewer(navController: NavHostController, templateJson: String?) {
        val formatted = try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(gson.fromJson(templateJson, Template::class.java))
        } catch (e: Exception) {
            templateJson
        }
        Text(
            text = formatted.toString(),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }

    @Composable
    private fun TipChip(text: String, color: Color) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.padding(end = 6.dp),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = text, style = AppTypography.body3Regular, color = color)
            }
        }
    }
}