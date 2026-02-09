package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.tasks.await
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class BarcodeScanner @Inject constructor() {

    companion object {
        private const val TAG = "BarcodeScanner"
    }

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODABAR,
            Barcode.FORMAT_ITF
        )
        .build()

    /**
     * Scans for a barcode in the warped (perspective-corrected) OMR sheet image.
     *
     * Crops the right portion of the sheet where the barcode is typically located,
     * then uses ML Kit to decode it.
     *
     * @param warpedMat The perspective-corrected grayscale Mat of the OMR sheet
     * @return The decoded barcode value, or null if no barcode found
     */
    suspend fun scanBarcode(warpedMat: Mat): String? {
        var croppedRoi: Mat? = null
        var colorMat: Mat? = null

        try {
            // Crop the right ~30% of the sheet where barcode is located
            val roiX = (warpedMat.cols() * 0.65).toInt()
            val roiY = 0
            val roiWidth = warpedMat.cols() - roiX
            val roiHeight = (warpedMat.rows() * 0.35).toInt() // Top 35% of sheet

            if (roiWidth <= 0 || roiHeight <= 0) {
                Log.w(TAG, "Invalid ROI dimensions for barcode scan")
                return null
            }

            val roi = Rect(roiX, roiY, roiWidth, roiHeight)
            croppedRoi = Mat(warpedMat, roi)

            // Convert grayscale to RGB for ML Kit
            colorMat = Mat()
            Imgproc.cvtColor(croppedRoi, colorMat, Imgproc.COLOR_GRAY2RGB)

            val bitmap = Bitmap.createBitmap(
                colorMat.cols(), colorMat.rows(), Bitmap.Config.ARGB_8888
            )
            org.opencv.android.Utils.matToBitmap(colorMat, bitmap)

            // Try scanning the cropped region first
            val result = scanBitmapForBarcode(bitmap)
            if (result != null) {
                Log.d(TAG, "Barcode found in cropped region: $result")
                return result
            }

            // Fallback: scan the full warped image
            Log.d(TAG, "No barcode in cropped region, trying full image")
            return scanFullImage(warpedMat)

        } catch (e: Exception) {
            Log.e(TAG, "Error scanning barcode", e)
            return null
        } finally {
            croppedRoi?.release()
            colorMat?.release()
        }
    }

    private suspend fun scanFullImage(grayMat: Mat): String? {
        var colorMat: Mat? = null
        try {
            colorMat = Mat()
            Imgproc.cvtColor(grayMat, colorMat, Imgproc.COLOR_GRAY2RGB)

            val bitmap = Bitmap.createBitmap(
                colorMat.cols(), colorMat.rows(), Bitmap.Config.ARGB_8888
            )
            org.opencv.android.Utils.matToBitmap(colorMat, bitmap)

            return scanBitmapForBarcode(bitmap)
        } finally {
            colorMat?.release()
        }
    }

    private suspend fun scanBitmapForBarcode(bitmap: Bitmap): String? {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val scanner = BarcodeScanning.getClient(options)

        return try {
            val barcodes = scanner.process(inputImage).await()

            if (barcodes.isNotEmpty()) {
                val barcode = barcodes.first()
                Log.d(TAG, "Barcode detected - format: ${barcode.format}, value: ${barcode.rawValue}")
                barcode.rawValue
            } else {
                null
            }
        } finally {
            scanner.close()
        }
    }
}
