package com.example.ledgerscanner.feature.scanner.scan.model

import android.graphics.Bitmap
import org.opencv.core.Mat

data class PreprocessResult(
    val ok: Boolean,
    val reason: String? = null,
    val warpedBitmap: Bitmap? = null,         // top-down, cropped OMR sheet
    val transformMatrix: Mat? = null,         // 3x3 perspective matrix (optional)
    val confidence: Double = 1.0,             // heuristic confidence (0..1)
    val intermediate: Map<String, Bitmap> = emptyMap() // for debug (gray, edged, thresh)
)