package com.example.ledgerscanner.feature.scanner.scan.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import com.example.ledgerscanner.base.enums.PermissionStatus
import com.example.ledgerscanner.base.ui.components.GenericToolbar
import com.example.ledgerscanner.base.ui.theme.LedgerScannerTheme
import com.example.ledgerscanner.base.ui.theme.White

class ScanOmrWithCamera : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LedgerScannerTheme {
                Scaffold(
                    containerColor = White,
                    topBar = {
                        GenericToolbar(title = "Scan OMR with Camera") {
                            //todo monika add
                        }
                    },
                    content = { innerPadding ->
                        Column(modifier = Modifier.padding(innerPadding)) {
                            CameraWidget()
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun CameraWidget() {
        var cameraPermissionStatus by remember { mutableStateOf(PermissionStatus.PermissionDenied) }

        val launcher = createPermissionLauncherComposeSpecific {
            cameraPermissionStatus = it
            println("Manifest.permission.CAMERA - $cameraPermissionStatus")
        }

        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }


}

open class BaseActivity : ComponentActivity() {

    fun createPermissionLauncher(permissionStatusCallback: (PermissionStatus) -> Unit): ActivityResultLauncher<String> {
        return registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                permissionStatusCallback(PermissionStatus.PermissionGranted)
            } else {
                val permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
                if (permanentlyDenied) permissionStatusCallback(PermissionStatus.PermissionPermanentlyDenied)
                else permissionStatusCallback(PermissionStatus.PermissionDenied)
            }
        }
    }

    @Composable
    fun createPermissionLauncherComposeSpecific(
        permissionStatusCallback: (PermissionStatus) -> Unit
    ): ManagedActivityResultLauncher<String, Boolean> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                permissionStatusCallback(PermissionStatus.PermissionGranted)
            } else {
                val permanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
                if (permanentlyDenied) permissionStatusCallback(PermissionStatus.PermissionPermanentlyDenied)
                else permissionStatusCallback(PermissionStatus.PermissionDenied)
            }
        }
    }

}