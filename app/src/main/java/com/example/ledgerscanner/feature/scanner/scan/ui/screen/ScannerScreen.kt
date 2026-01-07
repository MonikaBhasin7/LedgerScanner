package com.example.ledgerscanner.feature.scanner.scan.ui.screen

//import androidx.compose.ui.platform.LocalLifecycleOwner
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.example.ledgerscanner.base.enums.PermissionStatus
import com.example.ledgerscanner.base.extensions.BorderStyle
import com.example.ledgerscanner.base.extensions.customBorder
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.theme.AppTypography
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.utils.image.ImageUtils
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.ui.activity.ScanResultActivity
import com.example.ledgerscanner.feature.scanner.scan.ui.components.BrightnessQualityBadge
import com.example.ledgerscanner.feature.scanner.scan.ui.custom_ui.OverlayView
import com.example.ledgerscanner.feature.scanner.scan.viewmodel.OmrScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val CORNER_RADIUS_DP = 12

@Composable
fun ScannerScreen(
    navController: NavHostController,
    examEntity: ExamEntity,
    omrScannerViewModel: OmrScannerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = remember { context as? BaseActivity }
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Use rememberSaveable to survive config changes
    var cameraPermissionStatus by rememberSaveable {
        mutableStateOf(PermissionStatus.PermissionDenied)
    }

    // Compose-friendly launchers created via BaseActivity
    val cameraPermissionLauncher =
        activity?.createPermissionLauncherComposeSpecific { status ->
            cameraPermissionStatus = status
        }

    val appSettingsLauncher =
        activity?.createActivityLauncherComposeSpecific {
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
        examEntity = examEntity,
        navController = navController,
        cameraPermissionStatus = cameraPermissionStatus,
        onPermissionRequest = {
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
private fun CameraViewOrPermissionCard(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    omrScannerViewModel: OmrScannerViewModel,
    imageCapture: ImageCapture,
    examEntity: ExamEntity,
    navController: NavHostController,
    cameraPermissionStatus: PermissionStatus,
    onPermissionRequest: () -> Unit,
) {
    val brightnessQuality by omrScannerViewModel.brightnessQuality.collectAsState()
    when (cameraPermissionStatus) {
        PermissionStatus.PermissionGranted -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    omrScannerViewModel = omrScannerViewModel,
                    imageCapture = imageCapture,
                    examEntity = examEntity,
                    navController = navController
                )

                // Overlay brightness quality indicator
                brightnessQuality?.let { report ->
                    BrightnessQualityBadge(
                        report = report,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp)
                    )
                }
            }
        }

        else -> {
            PermissionPlaceholderCard(onEnableClick = onPermissionRequest)
        }
    }
}

@Composable
private fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    omrScannerViewModel: OmrScannerViewModel,
    imageCapture: ImageCapture,
    examEntity: ExamEntity,
    navController: NavHostController
) {
    val scope = rememberCoroutineScope()

    // Single-threaded analyzer executor remembered for lifecycle
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Media sound; loaded and released with lifecycle
    val mediaActionSound = remember {
        MediaActionSound().apply {
            load(MediaActionSound.SHUTTER_CLICK)
        }
    }

    val isCapturing = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    omrScannerViewModel.enableScanning()
                }

                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    omrScannerViewModel.disableScanning()
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraExecutor.shutdown()
            mediaActionSound.release()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(CORNER_RADIUS_DP.dp)),
        factory = { ctx ->
            createCameraContainer(
                context = ctx,
                lifecycleOwner = lifecycleOwner,
                examEntity = examEntity,
                imageCapture = imageCapture,
                cameraExecutor = cameraExecutor,
                scope = scope,
                omrScannerViewModel = omrScannerViewModel,
                navController = navController,
                mediaActionSound = mediaActionSound,
                isCapturing = isCapturing
            )
        }
    )
}

private fun createCameraContainer(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    examEntity: ExamEntity,
    imageCapture: ImageCapture,
    cameraExecutor: java.util.concurrent.Executor,
    scope: kotlinx.coroutines.CoroutineScope,
    omrScannerViewModel: OmrScannerViewModel,
    navController: NavHostController,
    mediaActionSound: MediaActionSound,
    isCapturing: AtomicBoolean
): FrameLayout {
    val container = FrameLayout(context)

    val previewView = PreviewView(context).apply {
        scaleType = PreviewView.ScaleType.FIT_CENTER
    }

    container.addView(
        previewView,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    )

    // Overlay setup
    val overlay = OverlayView(context).apply {
        setWillNotDraw(false)
        bringToFront()
        setTemplateSpec(examEntity.template)
    }

    container.addView(
        overlay,
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    )

    // Set default preview rect after first layout
    container.doOnLayout {
        overlay.setPreviewRect(
            RectF(
                0f,
                0f,
                container.width.toFloat(),
                container.height.toFloat()
            )
        )
    }

    // Setup image analysis
    setupImageAnalysis(
        previewView = previewView,
        overlay = overlay,
        cameraExecutor = cameraExecutor,
        scope = scope,
        omrScannerViewModel = omrScannerViewModel,
        examEntity = examEntity,
        navController = navController,
        mediaActionSound = mediaActionSound,
        isCapturing = isCapturing,
        context = context,
        lifecycleOwner = lifecycleOwner,
        imageCapture = imageCapture
    )

    return container
}

private fun setupImageAnalysis(
    previewView: PreviewView,
    overlay: OverlayView,
    cameraExecutor: java.util.concurrent.Executor,
    scope: kotlinx.coroutines.CoroutineScope,
    omrScannerViewModel: OmrScannerViewModel,
    examEntity: ExamEntity,
    navController: NavHostController,
    mediaActionSound: MediaActionSound,
    isCapturing: AtomicBoolean,
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageCapture: ImageCapture
) {
    val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()

    analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
        scope.launch(Dispatchers.Main) {
            // Skip if scanning is disabled
            if (!omrScannerViewModel.isScanningEnabled.value) {
                imageProxy.close()
                return@launch
            }

            val displayed = ImageUtils.computeDisplayedImageRect(
                viewW = previewView.width.toFloat(),
                viewH = previewView.height.toFloat(),
                bufferW = imageProxy.width,
                bufferH = imageProxy.height,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                useFill = previewView.scaleType == PreviewView.ScaleType.FILL_CENTER
            )

            overlay.setPreviewRect(displayed)

            val scanResult = omrScannerViewModel.processOmrFrame(
                context,
                imageProxy,
                examEntity,
                overlay.getAnchorSquaresOnScreen(),
                previewBounds = overlay.getPreviewRect(),
                debug = true,
                onAnchorsDetected = {
                    // Update overlay with detected anchors
                    overlay.setDetectedAnchors(it)
                }
            )
            when (scanResult) {
                is UiState.Error, is UiState.Idle, is UiState.Loading -> {}
                is UiState.Success -> {
                    if (isCapturing.compareAndSet(false, true)) {
                        mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
                        omrScannerViewModel.setCapturedResult(scanResult.data)

                        ScanResultActivity.launchScanResultScreen(
                            context = context,
                            examEntity,
                            scanResult.data
                        )
                    }
                }
            }

            imageProxy.close()
        }
    }

    bindCameraUseCases(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        analysisUseCase = analysisUseCase
    )
}

private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    analysisUseCase: ImageAnalysis
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
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
    }, ContextCompat.getMainExecutor(context))
}

@Composable
private fun PermissionPlaceholderCard(onEnableClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Grey200,
                shape = RoundedCornerShape(CORNER_RADIUS_DP.dp)
            )
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(CORNER_RADIUS_DP.dp))
                .customBorder(
                    width = 2.dp,
                    color = Blue500,
                    style = BorderStyle.Dashed,
                    cornerRadius = CORNER_RADIUS_DP.dp
                )
                .padding(36.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "We need access to your camera\nto scan OMR sheets.",
                    style = AppTypography.body3Regular,
                    color = Grey500,
                    textAlign = TextAlign.Center
                )

                GenericButton(
                    text = "Enable Camera",
                    icon = Icons.Default.PhotoCamera,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = onEnableClick
                )
            }
        }
    }
}