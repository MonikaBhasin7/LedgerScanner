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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

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
    var resumeCooldownUntilMs by remember { mutableStateOf(0L) }

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
        onScanError = { message ->
            // BUG FIX: Show error feedback instead of silently swallowing
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        cooldownUntilMs = resumeCooldownUntilMs,
        onResumeCooldown = { resumeCooldownUntilMs = it },
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
    onScanError: (String) -> Unit,
    cooldownUntilMs: Long,
    onResumeCooldown: (Long) -> Unit,
    onPermissionRequest: () -> Unit,
) {
    val brightnessQuality by omrScannerViewModel.brightnessQuality.collectAsState()
    val cameraRef = remember {
        java.util.concurrent.atomic.AtomicReference<androidx.camera.core.Camera?>(null)
    }
    var isTorchOn by remember { mutableStateOf(false) }
    var autoTorchTriggered by remember { mutableStateOf(false) }

    // Auto-enable torch when brightness is consistently poor/failed
    LaunchedEffect(brightnessQuality) {
        val level = brightnessQuality?.brightnessCheck?.level
        val brightness = brightnessQuality?.brightnessCheck?.value ?: 128.0
        if (level != null && level <= com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel.POOR
            && brightness < 80.0 && !autoTorchTriggered
        ) {
            val camera = cameraRef.get()
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                camera.cameraControl.enableTorch(true)
                isTorchOn = true
                autoTorchTriggered = true
            }
        }
        // Reset auto-torch flag when brightness improves (user moved to bright area)
        if (level != null && level >= com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel.GOOD) {
            autoTorchTriggered = false
        }
    }

    when (cameraPermissionStatus) {
        PermissionStatus.PermissionGranted -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    omrScannerViewModel = omrScannerViewModel,
                    imageCapture = imageCapture,
                    examEntity = examEntity,
                    navController = navController,
                    onScanError = onScanError,
                    cooldownUntilMs = cooldownUntilMs,
                    onResumeCooldown = onResumeCooldown,
                    cameraRef = cameraRef
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

                // Torch toggle button
                TorchToggleButton(
                    isTorchOn = isTorchOn,
                    onToggle = {
                        val camera = cameraRef.get()
                        if (camera?.cameraInfo?.hasFlashUnit() == true) {
                            val newState = !isTorchOn
                            camera.cameraControl.enableTorch(newState)
                            isTorchOn = newState
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                )
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
    navController: NavHostController,
    onScanError: (String) -> Unit,
    cooldownUntilMs: Long,
    onResumeCooldown: (Long) -> Unit,
    cameraRef: java.util.concurrent.atomic.AtomicReference<androidx.camera.core.Camera?>
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
    // BUG FIX: Use AtomicLong so cooldown updates are visible to the analyzer closure
    // (cooldownUntilMs was captured by the factory closure and never updated)
    val cooldownRef = remember { AtomicLong(cooldownUntilMs) }
    // Keep the ref in sync with compose state
    LaunchedEffect(cooldownUntilMs) { cooldownRef.set(cooldownUntilMs) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    omrScannerViewModel.enableScanning()
                    isCapturing.set(false)
                    onResumeCooldown(System.currentTimeMillis() + 1200)
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
                isCapturing = isCapturing,
                cooldownRef = cooldownRef,
                onScanError = onScanError,
                cameraRef = cameraRef
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
    isCapturing: AtomicBoolean,
    cooldownRef: AtomicLong,
    onScanError: (String) -> Unit,
    cameraRef: java.util.concurrent.atomic.AtomicReference<androidx.camera.core.Camera?>
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
        imageCapture = imageCapture,
        cooldownRef = cooldownRef,
        onScanError = onScanError,
        cameraRef = cameraRef
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
    imageCapture: ImageCapture,
    cooldownRef: AtomicLong,
    onScanError: (String) -> Unit,
    cameraRef: java.util.concurrent.atomic.AtomicReference<androidx.camera.core.Camera?>
) {
    val analysisUseCase = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .build()

    analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
        scope.launch(Dispatchers.Main) {
            try {
                // Skip if scanning is disabled
                if (!omrScannerViewModel.isScanningEnabled.value) {
                    return@launch
                }

                // BUG FIX: Read from AtomicLong instead of closure-captured value
                if (System.currentTimeMillis() < cooldownRef.get()) {
                    return@launch
                }

                if (isCapturing.get()) {
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
                    debug = com.example.ledgerscanner.BuildConfig.ENABLE_IMAGE_LOGS,
                    onAnchorsDetected = {
                        // Update overlay with detected anchors
                        overlay.setDetectedAnchors(it)
                    },
                    onStabilityUpdate = { stability ->
                        overlay.setStabilityProgress(
                            stability.stableFrameCount,
                            stability.requiredFrames
                        )
                    },
                    onGeometryUpdate = { geometry ->
                        overlay.setGeometryRejected(!geometry.isValid)
                    },
                    onBrightnessUpdate = { level ->
                        val lightingQuality = when (level) {
                            com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel.FAILED,
                            com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel.POOR -> {
                                val brightness = omrScannerViewModel.brightnessQuality.value
                                    ?.brightnessCheck?.value ?: 128.0
                                if (brightness < 128.0) OverlayView.LightingQuality.TOO_DARK
                                else OverlayView.LightingQuality.TOO_BRIGHT
                            }
                            com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel.ACCEPTABLE -> {
                                val brightness = omrScannerViewModel.brightnessQuality.value
                                    ?.brightnessCheck?.value ?: 128.0
                                if (brightness < 100.0) OverlayView.LightingQuality.TOO_DARK
                                else if (brightness > 180.0) OverlayView.LightingQuality.TOO_BRIGHT
                                else OverlayView.LightingQuality.GOOD
                            }
                            else -> OverlayView.LightingQuality.GOOD
                        }
                        overlay.setLightingQuality(lightingQuality)
                    }
                )
                when (scanResult) {
                    is UiState.Error -> {
                        // BUG FIX: Actually show error to user instead of swallowing it
//                        onScanError(scanResult.message ?: "Scan failed. Try again.")
                    }
                    is UiState.Idle, is UiState.Loading -> {}
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
            } finally {
                // BUG FIX: Always close imageProxy in finally block
                imageProxy.close()
            }
        }
    }

    bindCameraUseCases(
        context = context,
        lifecycleOwner = lifecycleOwner,
        previewView = previewView,
        imageCapture = imageCapture,
        analysisUseCase = analysisUseCase,
        onCameraBound = { camera -> cameraRef.set(camera) }
    )
}

private fun bindCameraUseCases(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture,
    analysisUseCase: ImageAnalysis,
    onCameraBound: (androidx.camera.core.Camera) -> Unit = {}
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture,
            analysisUseCase
        )
        onCameraBound(camera)
    }, ContextCompat.getMainExecutor(context))
}

@Composable
private fun TorchToggleButton(
    isTorchOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .background(
                color = if (isTorchOn) androidx.compose.ui.graphics.Color(0xFFFFC107)
                else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = if (isTorchOn) Icons.Default.FlashOn
            else Icons.Default.FlashOff,
            contentDescription = if (isTorchOn) "Turn off torch" else "Turn on torch",
            tint = if (isTorchOn) androidx.compose.ui.graphics.Color.Black
            else Grey500
        )
    }
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
