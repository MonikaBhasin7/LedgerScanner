package com.example.ledgerscanner.feature.scanner.scan.ui.compose

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.media.MediaActionSound
import android.net.Uri
import android.provider.Settings
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.enums.PermissionStatus
import com.example.ledgerscanner.base.extensions.BorderStyle
import com.example.ledgerscanner.base.extensions.customBorder
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.OverlayView
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanOmrWithCamera
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(
    navController: NavHostController,
    omrScannerViewModel: OmrScannerViewModel,
    omrTemplate: Template
) {
    val context = LocalContext.current
    val activity = remember { context as? BaseActivity }
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var cameraPermissionStatus by remember { mutableStateOf(PermissionStatus.PermissionDenied) }

    val cameraPermissionLauncher = activity?.createPermissionLauncherComposeSpecific {
        cameraPermissionStatus = it
    }

    val appSettingsLauncher = activity?.createActivityLauncherComposeSpecific {
        cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
    }

    CameraViewOrPermissionCard(
        context,
        lifecycleOwner,
        omrScannerViewModel,
        imageCapture,
        omrTemplate,
        navController,
        cameraPermissionStatus,
        takePermissionCallback = {
            if (cameraPermissionStatus == PermissionStatus.PermissionPermanentlyDenied) {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                appSettingsLauncher?.launch(intent)
            } else {
                cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
            }
        },
    )
}

@Composable
fun ScanningTipsCard() {

    @Composable
    fun TipRow(icon: ImageVector, text: String) {
        Row(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Companion.Gray,
                modifier = Modifier.Companion.size(16.dp)
            )
            Spacer(modifier = Modifier.Companion.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Companion.Gray
            )
        }
    }

    Card(
        modifier = Modifier.Companion
            .padding(top = 12.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Grey200,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Grey100),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.Companion.padding(16.dp)) {
            Text(
                text = "Scanning tips",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Companion.Black
            )

            Spacer(modifier = Modifier.Companion.height(8.dp))

            TipRow(
                icon = Icons.Outlined.OpenWith,
                text = "Fill the frame, include all four corners."
            )
            TipRow(
                icon = Icons.Outlined.WbSunny,
                text = "Avoid glare and shadows on bubbles."
            )
            TipRow(
                icon = Icons.Outlined.Crop,
                text = "Perspective will be corrected automatically."
            )
        }
    }
}

@Composable
private fun CameraViewOrPermissionCard(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    omrScannerViewModel: OmrScannerViewModel,
    imageCapture: ImageCapture,
    omrTemplate: Template,
    navController: NavHostController,
    cameraPermissionStatus: PermissionStatus,
    takePermissionCallback: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val mediaActionSound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
    }
    val cameraExecutor = Executors.newSingleThreadExecutor()

    val isCapturing = remember {
        AtomicBoolean(false)
    }

    if (cameraPermissionStatus == PermissionStatus.PermissionGranted) {
        AndroidView(
            modifier = Modifier.Companion
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
            factory = { ctx ->
                val container = FrameLayout(ctx)

                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                container.addView(
                    previewView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // 2) Our overlay
                val overlay = OverlayView(ctx).apply {
                    setWillNotDraw(false)
                    bringToFront()
                    setTemplateSpec(omrTemplate)
                }
                container.addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // set default preview rect after first layout (full view)
                container.doOnLayout {
                    overlay.setPreviewRect(
                        RectF(
                            0f, 0f,
                            container.width.toFloat(),
                            container.height.toFloat()
                        )
                    )
                }

                val analysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysisUseCase.setAnalyzer(cameraExecutor, { imageProxy ->
                    val (omrImageProcessResult, centers) = overlay.detectAnchorsInsideOverlay(
                        imageProxy,
                        omrTemplate,
                        debug = true
                    )
                    if (omrImageProcessResult.success && isCapturing.compareAndSet(
                            false,
                            true
                        )
                    ) {
                        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                        omrScannerViewModel.setOmrImageProcessResult(omrImageProcessResult)
                        scope.launch(Dispatchers.Main) {
                            navController.navigate(ScanOmrWithCamera.CAPTURE_PREVIEW_SCREEN)
                        }
                    }
                    imageProxy.close()
                })


                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        analysisUseCase
                    )
                }, ContextCompat.getMainExecutor(ctx))
                container
            })
    } else {
        Box(
            contentAlignment = Alignment.Companion.Center,
            modifier = Modifier.Companion
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    color = Grey200,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Companion.Center,
                modifier = Modifier.Companion
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .customBorder(
                        width = 2.dp,
                        color = Blue500,
                        style = BorderStyle.Dashed,
                        cornerRadius = 12.dp
                    )
                    .padding(36.dp)
            ) {
                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    Text(
                        "We need access to your camera\nto scan OMR sheets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Grey500,
                        textAlign = TextAlign.Companion.Center
                    )

                    GenericButton(
                        text = "Enable Camera",
                        icon = Icons.Default.PhotoCamera,
                        modifier = Modifier.Companion
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        onClick = {
                            takePermissionCallback()
                        }
                    )
                }
            }
        }
    }
}