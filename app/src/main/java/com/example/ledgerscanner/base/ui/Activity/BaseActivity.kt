package com.example.ledgerscanner.base.ui.Activity

import android.Manifest
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import com.example.ledgerscanner.base.enums.PermissionStatus

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

    @Composable
    fun createActivityLauncherComposeSpecific(callback: () -> Unit): ManagedActivityResultLauncher<Intent, ActivityResult> {
        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            callback()
        }
    }

    @Composable
    fun <I, O> createActivityLauncherComposeSpecific(
        contract: ActivityResultContract<I, O>,
        onResult: (O) -> Unit
    ): ManagedActivityResultLauncher<I, O> {
        return rememberLauncherForActivityResult(contract = contract, onResult = onResult)
    }

}