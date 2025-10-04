package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

@HiltViewModel
class OmrScannerViewModel @Inject constructor(
    private val omrProcessor: OmrProcessor,
    private val templateProcessor: TemplateProcessor
) : ViewModel() {
    private val _omrImageProcessResult = MutableStateFlow<OmrImageProcessResult?>(null)
    val omrImageProcessResult = _omrImageProcessResult.asStateFlow()

    fun setOmrImageProcessResult(result: OmrImageProcessResult) {
        _omrImageProcessResult.value = result
    }

    suspend fun processOmrFrame(
        image: ImageProxy,
        omrTemplate: Template,
        anchorSquaresOnPreviewScreen: List<RectF>,
        previewRect: RectF,
        debug: Boolean = false
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> = withContext(Dispatchers.Default) {

        val debugBitmaps = hashMapOf<String, Bitmap>()
        var finalBitmap: Bitmap? = null

        try {
            // 1) validate inputs (same behavior as before)
            val overlayRects = anchorSquaresOnPreviewScreen
            if (overlayRects.isEmpty() || previewRect.width() <= 0f) {
                return@withContext OmrImageProcessResult(false) to emptyList()
            }

            // 2) convert to gray mat (unchanged implementation)
            val gray = ImageConversionUtils.imageProxyToGrayMatUpright(image)
            if (debug) debugBitmaps["image proxy gray mat"] = gray.toBitmapSafe()

            // 3) detect centers in buffer (loop extracted)
            val centersInBuffer = mutableListOf<AnchorPoint>()
            val allFound = omrProcessor.findCentersInBuffer(
                overlayRects = overlayRects,
                previewRect = previewRect,
                gray = gray,
                debug = debug,
                debugBitmaps = debugBitmaps,
                centersOut = centersInBuffer
            )

            if (debug) {
                OpenCvUtils.drawPoints(
                    gray,
                    points = centersInBuffer,
                ).apply {
                    debugBitmaps["full image with found anchor"] = toBitmapSafe()
                }
            }

            if (!allFound) {
                gray.release()
                return@withContext OmrImageProcessResult(
                    success = false,
                    debugBitmaps = debugBitmaps
                ) to centersInBuffer
            }

            // 4) warp & bubble processing (extracted)
            val (warped, warpedDebugBitmap) = omrProcessor.warpWithTemplateAndGetWarped(
                gray = gray,
                omrTemplate = omrTemplate,
                centersInBuffer = centersInBuffer,
                debug = debug,
                debugBitmaps = debugBitmaps
            )

            if (debug && warpedDebugBitmap != null) {
                debugBitmaps["warped"] = warpedDebugBitmap
            }

            if (centersInBuffer.size == 4) {
                val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(omrTemplate)
                val detectedBubbles = omrProcessor.detectFilledBubbles(omrTemplate, warped)
                if (debug) {
                    debugBitmaps["bubble"] = OpenCvUtils.drawPoints(
                        warped,
                        bubblePoints,
                        fillColor = Scalar(255.0, 255.0, 255.0), // white dots
                        textColor = Scalar(0.0, 255.0, 255.0),   // cyan labels
                    ).toBitmapSafe()
                }
                finalBitmap = OpenCvUtils.drawPoints(
                    warped.toColoredWarped(),
                    bubblesWithColor = detectedBubbles,
                    fillColor = Scalar(255.0, 255.0, 255.0), // white dots
                    textColor = Scalar(0.0, 255.0, 255.0),   // cyan labels
                ).toBitmapSafe()
                if (bubblePoints.size != omrTemplate.totalBubbles()) {
                    return@withContext OmrImageProcessResult(
                        success = false,
                        debugBitmaps = debugBitmaps
                    ) to centersInBuffer
                }
                gray.release()
            } else {
                return@withContext OmrImageProcessResult(
                    success = false,
                    debugBitmaps = debugBitmaps,
                ) to centersInBuffer
            }


            return@withContext OmrImageProcessResult(
                success = true,
                debugBitmaps = debugBitmaps,
                finalBitmap = finalBitmap
            ) to centersInBuffer

        } catch (e: Exception) {
            return@withContext OmrImageProcessResult(
                success = false,
                reason = e.toString(),
                debugBitmaps = debugBitmaps
            ) to listOf()
        }
    }
}