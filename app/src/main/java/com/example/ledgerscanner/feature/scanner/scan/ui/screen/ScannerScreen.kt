package com.example.ledgerscanner.feature.scanner.scan.ui.screen

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.activity.ScanBaseActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.custom_ui.OverlayView
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

    // Compose-friendly launchers created via BaseActivity
    val cameraPermissionLauncher = activity?.createPermissionLauncherComposeSpecific {
        cameraPermissionStatus = it
    }

    val appSettingsLauncher = activity?.createActivityLauncherComposeSpecific {
        cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
    }

    // Request permission on first composition
    LaunchedEffect(Unit) {
        cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
    }

    CameraViewOrPermissionCard(
        context = context,
        lifecycleOwner = lifecycleOwner,
        omrScannerViewModel = omrScannerViewModel,
        imageCapture = imageCapture,
        omrTemplate = omrTemplate,
        navController = navController,
        cameraPermissionStatus = cameraPermissionStatus,
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
        }
    )
}

@Composable
fun ScanningTipsCard() {
    @Composable
    fun TipRow(icon: ImageVector, text: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = AppTypography.body3Regular,
                color = Color.Gray
            )
        }
    }

    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Grey200,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Grey100),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Scanning tips",
                style = AppTypography.label2Medium,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

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

    // single-threaded analyzer executor remembered for lifecycle
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Media sound; loaded and released with lifecycle
    val mediaActionSound = remember {
        MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) }
    }
    DisposableEffect(Unit) {
        onDispose {
            // MediaActionSound has no explicit release API; keep this for symmetry
        }
    }

    val isCapturing = remember { AtomicBoolean(false) }

    if (cameraPermissionStatus == PermissionStatus.PermissionGranted) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(RoundedCornerShape(12.dp)),
            factory = { ctx ->
                val container = FrameLayout(ctx)

                val previewView = PreviewView(ctx).apply {
                    this.scaleType = PreviewView.ScaleType.FIT_CENTER
                }
                container.addView(
                    previewView,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                // overlay (keeps your tested behaviour)
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

                // ImageAnalysis use case (same configuration)
                val analysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                analysisUseCase.setAnalyzer(cameraExecutor, { imageProxy ->
                    scope.launch(Dispatchers.Main) {
                        val displayed = computeDisplayedImageRect(
                            previewView.width.toFloat(),
                            previewView.height.toFloat(),
                            imageProxy.width,
                            imageProxy.height,
                            imageProxy.imageInfo.rotationDegrees,
                            useFill = (previewView.scaleType == PreviewView.ScaleType.FILL_CENTER)
                        )

                        overlay.setPreviewRect(displayed)

                        val (omrImageProcessResult, detectedAnchors) = omrScannerViewModel.processOmrFrame(
                            imageProxy,
                            omrTemplate,
                            overlay.getAnchorSquaresOnScreen(),
                            overlay.getPreviewRect(),
                            debug = true
                        )

                        // NEW: Update overlay with detected anchors
                        overlay.setDetectedAnchors(detectedAnchors)

                        if (omrImageProcessResult.success && isCapturing.compareAndSet(false, true)) {
                            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                            omrScannerViewModel.setOmrImageProcessResult(omrImageProcessResult)
                            navController.navigate(ScanBaseActivity.CAPTURE_PREVIEW_SCREEN)
                        }

                        imageProxy.close()
                    }
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
            }
        )
    } else {
        PermissionPlaceholderCard(
            onEnableClick = { takePermissionCallback() }
        )
    }
}

/**
 * Compute the rectangle (in view coordinates) where the camera image buffer is actually
 * drawn inside the PreviewView when using FIT_CENTER or FILL_CENTER (letterbox/crop).
 *
 * @param viewW view width in px (PreviewView.width)
 * @param viewH view height in px (PreviewView.height)
 * @param bufferW image buffer width in px (ImageProxy.width)
 * @param bufferH image buffer height in px (ImageProxy.height)
 * @param rotationDegrees imageInfo.rotationDegrees from the ImageProxy
 * @param useFill true if PreviewView.ScaleType == FILL_CENTER (image is scaled to fill & cropped),
 *                false if FIT_CENTER (image is letterboxed)
 */
fun computeDisplayedImageRect(
    viewW: Float,
    viewH: Float,
    bufferW: Int,
    bufferH: Int,
    rotationDegrees: Int,
    useFill: Boolean
): RectF {
    // defensive
    if (viewW <= 0f || viewH <= 0f || bufferW <= 0 || bufferH <= 0) {
        return RectF(0f, 0f, viewW.coerceAtLeast(0f), viewH.coerceAtLeast(0f))
    }

    // 1) Determine the logical image width/height taking rotation into account.
    //    Image proxy buffer is given in sensor orientation; rotationDegrees tells how it's rotated to display.
    val (imgWf, imgHf) = if (rotationDegrees % 180 == 0) {
        bufferW.toFloat() to bufferH.toFloat()
    } else {
        // swap dims for 90/270
        bufferH.toFloat() to bufferW.toFloat()
    }

    // 2) Compute scale used by PreviewView
    val scale = if (useFill) {
        // fill = crop (scale up until both dimensions >= view)
        maxOf(viewW / imgWf, viewH / imgHf)
    } else {
        // fit = letterbox (scale down until both dimensions <= view)
        minOf(viewW / imgWf, viewH / imgHf)
    }

    // 3) Compute displayed image size
    val dispW = imgWf * scale
    val dispH = imgHf * scale

    // 4) center the displayed image inside the view
    val left = (viewW - dispW) / 2f
    val top = (viewH - dispH) / 2f
    val right = left + dispW
    val bottom = top + dispH

    // 5) sanity clamp to view rect (avoid tiny float inaccuracies)
    val l = left.coerceAtLeast(0f).coerceAtMost(viewW)
    val t = top.coerceAtLeast(0f).coerceAtMost(viewH)
    val r = right.coerceAtLeast(l).coerceAtMost(viewW)
    val b = bottom.coerceAtLeast(t).coerceAtMost(viewH)

    // quick sanity: if result degenerate, fallback to full view
    if ((r - l) < 1f || (b - t) < 1f) {
        return RectF(0f, 0f, viewW, viewH)
    }

    return RectF(l, t, r, b)
}

@Composable
private fun PermissionPlaceholderCard(onEnableClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = Grey200,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .customBorder(
                    width = 2.dp,
                    color = Blue500,
                    style = BorderStyle.Dashed,
                    cornerRadius = 12.dp
                )
                .padding(36.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "We need access to your camera\nto scan OMR sheets.",
                    style = AppTypography.body3Regular,
                    color = Grey500,
                    textAlign = TextAlign.Center
                )

                GenericButton(
                    text = "Enable Camera",
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = onEnableClick
                )
            }
        }
    }
}