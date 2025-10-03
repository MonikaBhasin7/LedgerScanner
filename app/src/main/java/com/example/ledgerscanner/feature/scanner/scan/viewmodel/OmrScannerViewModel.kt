package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
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

    fun detectAnchorsInsideOverlay(
        image: ImageProxy,
        omrTemplate: Template,
        anchorSquaresOnPreviewScreen: List<RectF>,
        previewRect: RectF,
        debug: Boolean = false
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> {
        val debugBitmaps = hashMapOf<String, Bitmap>()
        var finalBitmap: Bitmap? = null

        try {
            // 1) Get overlay squares in SCREEN pixels and the preview rectangle we drew against
            val overlayRects = anchorSquaresOnPreviewScreen // same sidePx as your overlay
            if (overlayRects.isEmpty() || previewRect.width() <= 0f) {
                return OmrImageProcessResult(false) to emptyList()
            }

            // 2) Convert camera buffer to a gray Mat (once per frame)
            val gray = ImageConversionUtils.imageProxyToGrayMatUpright(image)
            if (debug) debugBitmaps["image proxy gray mat"] = gray.toBitmapSafe()

            val centersInBuffer = ArrayList<AnchorPoint>(overlayRects.size)
            var allFound = true

            // 3) For each overlay square: map to buffer → crop → detect black square
            for ((index, screenRect) in overlayRects.withIndex()) {
                // 1) Map screen overlay square → Mat rect
                val matRect =
                    ImageConversionUtils.screenRectToMatRect(screenRect, previewRect, gray)

                // 2) Crop ROI directly with that rect
                val roi = Mat(gray, matRect)
                if (debug) debugBitmaps["roi - $index"] = roi.toBitmapSafe()

                // 3) Detect anchor inside ROI
//                val anchor = TemplateProcessor().detectAnchorPointsImpl(roi).getOrNull(0)
                val anchor = OpenCvUtils.detectAnchorInRoi(roi)
                roi.release()

                if (anchor == null) {
                    allFound = false
                } else {
                    // 4) Convert ROI-local center to full Mat coordinates
                    centersInBuffer += AnchorPoint(
                        (matRect.x + anchor.x.toFloat()).toDouble(),
                        (matRect.y + anchor.y.toFloat()).toDouble()
                    )
                }
            }

            if (debug)
                OpenCvUtils.drawPoints(
                    gray,
                    points = centersInBuffer,
                ).apply {
                    debugBitmaps["full image with found anchor"] = toBitmapSafe()
                }

            if (!allFound) {
                return OmrImageProcessResult(
                    success = false,
                    debugBitmaps = debugBitmaps
                ) to centersInBuffer
            }

            val templateAnchors = listOf(
                omrTemplate.anchor_top_left,
                omrTemplate.anchor_top_right,
                omrTemplate.anchor_bottom_right,
                omrTemplate.anchor_bottom_left
            ) // anchors from your template JSON
            val warped = omrProcessor.warpWithAnchors(
                src = gray,
                detectedAnchors = centersInBuffer,
                templateAnchors = templateAnchors,
                templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
            )
            if (debug) debugBitmaps["warped"] = warped.toBitmapSafe()

            if (centersInBuffer.size == 4) {
                val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(
                    omrTemplate
                )
                if (debug) {
                    finalBitmap = OpenCvUtils.drawPoints(
                        warped,
                        bubblePoints,
                        fillColor = Scalar(255.0, 255.0, 255.0), // white dots
                        textColor = Scalar(0.0, 255.0, 255.0),   // cyan labels
                    ).toBitmapSafe()
                    debugBitmaps["bubble"] = finalBitmap
                }
                if (bubblePoints.size != omrTemplate.totalBubbles()) {
                    return OmrImageProcessResult(
                        success = false,
                        debugBitmaps = debugBitmaps
                    ) to centersInBuffer
                }
            }

            gray.release()

            return OmrImageProcessResult(
                success = true,
                debugBitmaps = debugBitmaps,
                finalBitmap = finalBitmap
            ) to centersInBuffer
        } catch (e: Exception) {
            return OmrImageProcessResult(
                success = false,
                reason = e.toString(),
                debugBitmaps = debugBitmaps
            ) to listOf()
        }
    }
}