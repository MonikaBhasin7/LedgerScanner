package com.example.ledgerscanner.base.utils

import android.content.Context
import com.example.ledgerscanner.R
import java.io.File

object FileUtils {

    fun getOutputDirectory(context: Context): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, context.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }
}