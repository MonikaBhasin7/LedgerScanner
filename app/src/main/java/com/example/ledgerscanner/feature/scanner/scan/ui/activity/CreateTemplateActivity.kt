package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.ledgerscanner.BuildConfig
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.*
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
                    onProcess = {
                        val result =
                            selectedBitmap?.let { TemplateProcessor().generateTemplateJson(it) }
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
                }

                result != null && BuildConfig.ENABLE_IMAGE_LOGS -> {
                    WarpedImageDialog(
                        warpedBitmap = result.finalBitmap,
                        intermediateBitmaps = result.debugBitmaps,
                        onDismiss = { clearResult() }
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
        selectedBitmap: android.graphics.Bitmap?,
        result: com.example.ledgerscanner.feature.scanner.scan.model.OmrTemplateResult?,
        onReselect: () -> Unit,
        onProcess: () -> Unit,
        onViewJson: (String) -> Unit
    ) {
        result?.let {
            if (it.success && it.finalBitmap != null && !it.templateJson.isNullOrEmpty()) {
                GenericButton(
                    text = "View Json",
                    enabled = selectedBitmap != null,
                    modifier = Modifier
                        .padding(bottom = 4.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    onClick = { onViewJson(it.templateJson!!) }
                )
            }
        }

        GenericButton(
            text = if (result != null) "Reselect another Image" else "Process template",
            enabled = selectedBitmap != null,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .fillMaxWidth(),
            onClick = { if (result != null) onReselect() else onProcess() }
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
                style = MaterialTheme.typography.bodyLarge,
                color = Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap to choose the template image.\nMake sure the sheet fills the frame.",
                style = MaterialTheme.typography.bodySmall,
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
                style = MaterialTheme.typography.labelLarge
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
                Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
            }
        }
    }
}