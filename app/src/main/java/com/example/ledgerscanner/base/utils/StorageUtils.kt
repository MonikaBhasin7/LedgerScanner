package com.example.ledgerscanner.base.utils

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object StorageUtils {

    suspend fun saveImageToStorage(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): String {
        return withContext(Dispatchers.IO) {
            val directory = File(context.filesDir, "scanned_sheets")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val file = File(directory, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            file.absolutePath
        }
    }

    fun createThumbnail(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    // ============ Cleanup ============

    suspend fun deleteImageFile(path: String) {
        withContext(Dispatchers.IO) {
            try {
                File(path).delete()
            } catch (e: Exception) {
                android.util.Log.e("ScanResultRepository", "Error deleting file: $path", e)
            }
        }
    }
}