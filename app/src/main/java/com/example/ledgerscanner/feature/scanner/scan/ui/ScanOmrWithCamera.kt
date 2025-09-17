package com.example.ledgerscanner.feature.scanner.scan.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ledgerscanner.base.enums.PermissionStatus
import com.example.ledgerscanner.base.extensions.BorderStyle
import com.example.ledgerscanner.base.extensions.customBorder
import com.example.ledgerscanner.base.ui.Activity.BaseActivity
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.*

class ScanOmrWithCamera : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    topBar = {
                        GenericToolbar(title = "Scan OMR with Camera") {
                            //todo monika add
                        }
                    },
                    bottomBar = {
                        CaptureButton()
                    },
                    content = { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .padding(horizontal = 16.dp)
                        ) {
                            CameraWidget()
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun CaptureButton() {
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
                Button(
                    onClick = {

                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings, // Material icon
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable Camera",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    @Composable
    fun CameraWidget() {
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

        Column {
            CameraViewOrPermissionCard(
                context,
                lifecycleOwner,
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
                })
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
                    icon = Icons.Outlined.Info,
                    text = "Fill the frame, include all four corners."
                )
                TipRow(
                    icon = Icons.Outlined.Info,
                    text = "Avoid glare and shadows on bubbles."
                )
                TipRow(
                    icon = Icons.Outlined.Info,
                    text = "Perspective will be corrected automatically."
                )
            }
        }
    }

    @Composable
    fun CameraViewOrPermissionCard(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        cameraPermissionStatus: PermissionStatus,
        takePermissionCallback: () -> Unit
    ) {
        val fraction = 0.65f

        if (cameraPermissionStatus == PermissionStatus.PermissionGranted)
            return AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(12.dp)),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
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
                            preview
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                })
        else
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
                        Button(
                            onClick = {
                                takePermissionCallback()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings, // Material icon
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable Camera",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
    }
}
