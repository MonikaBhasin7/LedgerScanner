package com.example.ledgerscanner.feature.scanner.scan.ui.activity

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.Black
import com.example.ledgerscanner.base.ui.theme.Blue100
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.image.ImageUtils
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.CreateTemplateViewModel
import com.google.gson.GsonBuilder

class CreateTemplateActivity : BaseActivity() {

    private val pickImageViewModel: CreateTemplateViewModel by viewModels()

    companion object {
        const val PICK_IMAGE = "pick_image"
        const val TEMPLATE_JSON = "template_json"
        const val VIEW_JSON = "view_json"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    content = { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            val navController = rememberNavController()
                            NavHost(navController, startDestination = PICK_IMAGE) {
                                composable(PICK_IMAGE) {
                                    PickImageScreen(navController)
                                }
                                composable(VIEW_JSON) {
                                    val jsonArg = navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.get<String>(TEMPLATE_JSON)
                                    ViewJsonScreen(navController, jsonArg)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun PickImageScreen(navController: NavHostController) {
        val context = LocalContext.current
        val pickedBitmap by pickImageViewModel.pickedBitmap.collectAsState()
        val templateResult by pickImageViewModel.templateResult.collectAsState()

        LaunchedEffect(templateResult) {
            templateResult?.let {
                if (!it.success && !it.reason.isNullOrEmpty()) {
                    Toast.makeText(context, it.reason, Toast.LENGTH_SHORT).show()
                }
            }
        }
        val pickLauncher =
            createActivityLauncherComposeSpecific(ActivityResultContracts.GetContent()) { uri ->
                if (uri == null) {
                    return@createActivityLauncherComposeSpecific
                }
                val b = ImageUtils.loadBitmapCorrectOrientation(
                    context,
                    uri,
                    reqWidth = 1080,
                    reqHeight = 1920
                )
                pickImageViewModel.setBitmap(b)
            }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                templateResult?.let {
                    if (it.success && it.finalBitmap != null) {
                        Image(
                            bitmap = it.finalBitmap.asImageBitmap(),
                            contentDescription = "Captured image",
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(16.dp),
                            filterQuality = FilterQuality.High
                        )
                    } else {
                        WarpedImageDialog(
                            warpedBitmap = it.finalBitmap,
                            intermediateBitmaps = it.debugBitmaps,
                            onDismiss = { pickImageViewModel.setTemplateResult(null) },
                        )
                    }
                } ?: run {
                    pickedBitmap?.asImageBitmap()?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Captured image",
                            modifier = Modifier.Companion
                                .fillMaxSize()
                                .padding(16.dp),
                            contentScale = ContentScale.Companion.Fit
                        )
                    } ?: run {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    pickLauncher.launch("image/*")
                                }
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
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // small tips row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TipChip(text = "Auto perspective", color = Color(0xFF0EA5A4))
                                TipChip(text = "Avoid glare", color = Color(0xFFF97316))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TipChip(text = "High contrast", color = Color(0xFF3B82F6))
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }

            templateResult?.let {
                if (it.success && it.finalBitmap != null && !it.templateJson.isNullOrEmpty()) {
                    GenericButton(
                        text = "View Json",
                        enabled = pickedBitmap != null,
                        modifier = Modifier
                            .padding(bottom = 4.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth(),
                        onClick = {
                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                TEMPLATE_JSON,
                                it.templateJson
                            )
                            navController.navigate(VIEW_JSON)
                        }
                    )
                }
            }

            GenericButton(
                text = if (templateResult != null) "Reselect another Image" else "Process template",
                enabled = pickedBitmap != null,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (templateResult != null) {
                        pickImageViewModel.setTemplateResult(null)
                        pickImageViewModel.setBitmap(null)
                    } else {
                        val r = pickedBitmap?.let { TemplateProcessor().generateTemplateJson(it) }
                        pickImageViewModel.setTemplateResult(r)
                    }
                }
            )
        }

    }

    @Composable
    fun ViewJsonScreen(navController: NavHostController, templateJson: String?) {
        val formatted = try {
            val gson = GsonBuilder().setPrettyPrinting().create()
            gson.toJson(gson.fromJson(templateJson, Template::class.java))
        } catch (e: Exception) {
            templateJson // fallback
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
            modifier = Modifier
                .padding(end = 6.dp),
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