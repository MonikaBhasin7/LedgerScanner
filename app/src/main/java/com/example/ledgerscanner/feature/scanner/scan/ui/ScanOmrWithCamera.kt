package com.example.ledgerscanner.feature.scanner.scan.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.doOnLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ledgerscanner.base.enums.PermissionStatus
import com.example.ledgerscanner.base.extensions.BorderStyle
import com.example.ledgerscanner.base.extensions.customBorder
import com.example.ledgerscanner.base.extensions.loadJsonFromAssets
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericButton
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.Blue500
import com.example.ledgerscanner.base.ui.theme.Grey100
import com.example.ledgerscanner.base.ui.theme.Grey200
import com.example.ledgerscanner.base.ui.theme.Grey500
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White
import com.example.ledgerscanner.base.utils.FileUtils
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.ui.dialog.WarpedImageDialog
import org.opencv.core.Point
import java.io.File
import java.util.concurrent.Executors

class ScanOmrWithCamera : BaseActivity() {

    companion object {
        const val TAG = "ScanOmrWithCamera"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                val imageCapture = remember { ImageCapture.Builder().build() }
                var cameraReady by remember { mutableStateOf(false) }
                Scaffold(
                    containerColor = White,
                    topBar = {
                        GenericToolbar(title = "Scan OMR with Camera") {
                            finish()
                        }
                    },
                    bottomBar = {
                        CaptureButton(imageCapture, cameraReady)
                    },
                    content = { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp)
                        ) {
                            CameraWidget(imageCapture, cameraReadyCallback = {
                                cameraReady = true
                            })
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun CaptureButton(imageCapture: ImageCapture, cameraReady: Boolean) {
        val context = LocalContext.current

        val pickLauncher =
            createActivityLauncherComposeSpecific(ActivityResultContracts.GetContent()) { uri ->
                val intent =
                    Intent(context, PreviewImageActivity::class.java).apply {
                        putExtra("image_uri", uri)
                    }
                context.startActivity(intent)
            }

        val cropLauncher = createActivityLauncherComposeSpecific(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val resultUri = com.yalantis.ucrop.UCrop.getOutput(res.data!!)
                if (resultUri != null) {
                    context.startActivity(
                        Intent(
                            context,
                            PreviewImageActivity::class.java
                        ).apply {
                            putExtra("image_uri", resultUri)
                        })
                }
            } else if (res.resultCode == Activity.RESULT_CANCELED) {

            } else {
                val err = com.yalantis.ucrop.UCrop.getError(res.data!!)
            }
        }

        Column {
            Divider(
                color = Grey200,  // or any color you want
                thickness = 1.dp,
            )
            Box(
                Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .background(color = Grey100)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    GenericButton(
                        text = "Pick Image from Gallery",
                        icon = Icons.Default.Photo,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            pickLauncher.launch("image/*")
                        }
                    )

                    GenericButton(
                        text = "Capture Image",
                        icon = Icons.Default.PhotoCamera,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val photoFile = File(
                                FileUtils.getOutputDirectory(context),
                                "${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions =
                                ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        onImageCaptured(Uri.fromFile(photoFile))
                                    }

                                    private fun onImageCaptured(fromFile: Uri) {
                                        val destFile = File(
                                            context.cacheDir,
                                            "crop_${System.currentTimeMillis()}.jpg"
                                        )
                                        val destUri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            destFile
                                        )

                                        val options = com.yalantis.ucrop.UCrop.Options().apply {
                                            setToolbarTitle("Crop")
                                            setCompressionFormat(Bitmap.CompressFormat.PNG)
                                            setCompressionQuality(92)
                                            setHideBottomControls(false)     // show rotate/scale controls bar
                                            setFreeStyleCropEnabled(true)    // user can drag corners
                                        }

                                        val intent = com.yalantis.ucrop.UCrop.of(fromFile, destUri)
                                            .withAspectRatio(
                                                1f,
                                                1f
                                            )        // change or remove for free aspect
                                            .withMaxResultSize(1080, 1080)  // clamp output size
                                            .withOptions(options)
                                            .getIntent(context)

                                        cropLauncher.launch(intent)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Toast.makeText(
                                            context, "Exception while capturing image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun CameraWidget(imageCapture: ImageCapture, cameraReadyCallback: () -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        var cameraPermissionStatus by remember { mutableStateOf(PermissionStatus.PermissionDenied) }

        val cameraPermissionLauncher = createPermissionLauncherComposeSpecific {
            cameraPermissionStatus = it
        }

        val appSettingsLauncher = createActivityLauncherComposeSpecific {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        LaunchedEffect(Unit) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        var showFinalProcessedImageDialog by remember { mutableStateOf(false) }
        var omrResult by remember { mutableStateOf<OmrResult?>(null) }

        Column {
            if (showFinalProcessedImageDialog && omrResult != null)
                WarpedImageDialog(
                    warpedBitmap = omrResult?.finalBitmap,
                    intermediateBitmaps = omrResult?.debugBitmaps,
                    onDismiss = { showFinalProcessedImageDialog = false },
                    onRetry = {
                        showFinalProcessedImageDialog = false
                    },
                    onSave = {
                        showFinalProcessedImageDialog = false
                    }
                )
            CameraViewOrPermissionCard(
                context,
                lifecycleOwner,
                imageCapture,
                cameraPermissionStatus,
                takePermissionCallback = {
                    if (cameraPermissionStatus == PermissionStatus.PermissionPermanentlyDenied) {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        appSettingsLauncher.launch(intent)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }, cameraReadyCallback, showDebugImageDialog = {
                    showFinalProcessedImageDialog = true
                    omrResult = it
                }
            )
            ScanningTipsCard()
        }
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
                    style = MaterialTheme.typography.bodySmall,
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
                    style = MaterialTheme.typography.labelMedium,
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
    fun CameraViewOrPermissionCard(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        imageCapture: ImageCapture,
        cameraPermissionStatus: PermissionStatus,
        takePermissionCallback: () -> Unit,
        cameraReadyCallback: () -> Unit,
        showDebugImageDialog: (OmrImageProcessResult) -> Unit //todo monika remove in future
    ) {
        val haptic = LocalHapticFeedback.current
        val fraction = 0.65f
        val anchors = listOf(
            // TL, TR, BR, BL (or any order — the overlay just plots)
            Point(470.5, 269.5),
            Point(719.0, 269.5),
            Point(728.5, 1395.0),
            Point(471.0, 1394.5),
        )

        val cameraExecutor = Executors.newSingleThreadExecutor()
        var omrImageProcessResultGlobal: OmrImageProcessResult? = null
        var anchorPointsOnCapturedImage = listOf<Point>()


        if (cameraPermissionStatus == PermissionStatus.PermissionGranted) {
            AndroidView(
                modifier = Modifier
                    .clickable {
                        omrImageProcessResultGlobal?.let {
                            println("$TAG - points - $anchorPointsOnCapturedImage")
                            showDebugImageDialog(it)
                        }
                    }
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
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

                    // 2) Our overlay
                    val overlay = OverlayView(ctx).apply {
                        setWillNotDraw(false)
                        bringToFront()
                        setTemplateSpec(1080, 1527, anchors)
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


                    val omrTemplate =
                        context.loadJsonFromAssets<Template>("template_omr_10_ques.json")
                    if (omrTemplate == null) {
                        Toast.makeText(context, "Error in decoding template", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        analysisUseCase.setAnalyzer(cameraExecutor, { imageProxy ->
                            val (omrImageProcessResult, centers) = overlay.detectAnchorsInsideOverlay(
                                imageProxy,
                                omrTemplate,
                                debug = true
                            )
                            if (omrImageProcessResult.success && omrImageProcessResult.debugBitmaps.isNotEmpty()) {
                                omrImageProcessResultGlobal = omrImageProcessResult
                                anchorPointsOnCapturedImage = centers
//                                imageCapture.takePicture(
//                                    ContextCompat.getMainExecutor(context),
//                                    object : ImageCapture.OnImageCapturedCallback() {
//                                        override fun onCaptureSuccess(captured: ImageProxy) {
//                                            try {
//                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
//                                                omrImageProcessResultGlobal = omrImageProcessResult
//                                                anchorPointsOnCapturedImage = centers
//
//                                            } finally {
//                                                captured.close()
//                                            }
//                                        }
//                                        override fun onError(exc: ImageCaptureException) {
//
//                                        }
//                                    }
//                                )
                            }
                            imageProxy.close()
                        })
                    }

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
                        ).also {
                            cameraReadyCallback()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    container
                })
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .background(color = Grey200, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .customBorder(
                            width = 2.dp,
                            color = Blue500,
                            style = BorderStyle.Dashed,
                            cornerRadius = 12.dp
                        )
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "We need access to your camera\nto scan OMR sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Grey500,
                            textAlign = TextAlign.Center
                        )

                        GenericButton(
                            text = "Enable Camera",
                            icon = Icons.Default.PhotoCamera,
                            modifier = Modifier
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
}
