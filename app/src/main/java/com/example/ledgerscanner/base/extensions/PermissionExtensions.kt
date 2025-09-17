package com.example.ledgerscanner.base.extensions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import com.example.ledgerscanner.base.enums.PermissionStatus

fun Int.toPermissionStatus(activity: Activity, permission: String): PermissionStatus {
    return when (this) {
        PackageManager.PERMISSION_GRANTED -> PermissionStatus.PermissionGranted
        PackageManager.PERMISSION_DENIED -> {
            if (!activity.shouldShowRequestPermissionRationale(permission)) {
                PermissionStatus.PermissionPermanentlyDenied
            }
            PermissionStatus.PermissionDenied
        }

        else -> PermissionStatus.PermissionDenied
    }
}