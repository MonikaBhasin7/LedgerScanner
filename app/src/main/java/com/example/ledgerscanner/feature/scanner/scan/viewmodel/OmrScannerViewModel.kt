package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.utils.AnswerEvaluator
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.TemplateProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.opencv.core.Mat
import org.opencv.core.Scalar
import javax.inject.Inject

@HiltViewModel
class OmrScannerViewModel @Inject constructor(
    private val omrProcessor: OmrProcessor,
    private val templateProcessor: TemplateProcessor,
    private val answerEvaluator: AnswerEvaluator
) : ViewModel() {
    private val _omrImageProcessResult = MutableStateFlow<OmrImageProcessResult?>(null)
    val omrImageProcessResult = _omrImageProcessResult.asStateFlow()

    fun setOmrImageProcessResult(result: OmrImageProcessResult) {
        _omrImageProcessResult.value = result
    }

    suspend fun processOmrFrame(
        image: ImageProxy,
        examEntity: ExamEntity,
        anchorSquaresOnPreviewScreen: List<RectF>,
        previewRect: RectF,
        debug: Boolean = false
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> = withContext(Dispatchers.Default) {

        val debugBitmaps = hashMapOf<String, Bitmap>()
        var finalBitmap: Bitmap? = null
        var gray: Mat? = null
        var warped: Mat? = null
        val omrTemplate = examEntity.template

        try {
            // 1) Validate inputs
            val overlayRects = anchorSquaresOnPreviewScreen
            if (overlayRects.isEmpty() || previewRect.width() <= 0f) {
                return@withContext OmrImageProcessResult(false) to emptyList()
            }

            // 2) Convert to gray mat
            gray = ImageConversionUtils.imageProxyToGrayMatUpright(image)
            if (debug) debugBitmaps["image_proxy_gray_mat"] = gray.toBitmapSafe()

            // 3) Detect centers in buffer
            val centersInBuffer = mutableListOf<AnchorPoint>()
            val allFound = omrProcessor.findCentersInBuffer(
                overlayRects = overlayRects,
                previewRect = previewRect,
                gray = gray,
                debug = debug,
                debugBitmaps = debugBitmaps,
                centersOut = centersInBuffer
            )

            if (debug && centersInBuffer.isNotEmpty()) {
                val grayWithAnchors = gray.clone()
                OpenCvUtils.drawPoints(
                    grayWithAnchors,
                    points = centersInBuffer,
                ).apply {
                    debugBitmaps["full_image_with_anchors"] = toBitmapSafe()
                    release()
                }
            }

            if (!allFound) {
                return@withContext OmrImageProcessResult(
                    success = false,
                    debugBitmaps = debugBitmaps
                ) to centersInBuffer
            }

            // 4) Warp image
            val (warpedMat, warpedDebugBitmap) = omrProcessor.warpWithTemplateAndGetWarped(
                gray = gray,
                omrTemplate = omrTemplate,
                centersInBuffer = centersInBuffer,
                debug = debug,
                debugBitmaps = debugBitmaps
            )
            warped = warpedMat

            if (debug && warpedDebugBitmap != null) {
                debugBitmaps["warped"] = warpedDebugBitmap
            }

            if (centersInBuffer.size == 4) {
                val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(omrTemplate)

                if (bubblePoints.size != omrTemplate.totalBubbles()) {
                    return@withContext OmrImageProcessResult(
                        success = false,
                        reason = "Bubble count mismatch: expected ${omrTemplate.totalBubbles()}, got ${bubblePoints.size}",
                        debugBitmaps = debugBitmaps
                    ) to centersInBuffer
                }

                //Detect filled bubbles (ONLY DETECTION)
                val detectionResult = omrProcessor.detectFilledBubbles(omrTemplate, warped)

                if (debug) {
                    val bubbleDebugMat = warped.clone()
                    debugBitmaps["bubbles_detected"] = OpenCvUtils.drawPoints(
                        bubbleDebugMat,
                        points = detectionResult.bubbles.map { it.point },
                        fillColor = Scalar(255.0, 255.0, 255.0),
                        textColor = Scalar(0.0, 255.0, 255.0)
                    ).toBitmapSafe()
                    bubbleDebugMat.release()
                }

                //OPTIONAL: Evaluate answers if answer key is provided
                val evaluationResult = if (examEntity.answerKey != null) {
                    answerEvaluator.evaluateAnswers(
                        detectedBubbles = detectionResult.bubbles,
                        examEntity,
                    )
                } else {
                    null
                }

                // Create final result bitmap
                val coloredWarped = warped.toColoredWarped()
                finalBitmap = if (evaluationResult != null) {
                    // Draw with colors based on correctness
                    OpenCvUtils.drawPointsWithEvaluation(
                        coloredWarped,
                        bubbles = detectionResult.bubbles,
                        evaluation = evaluationResult
                    ).toBitmapSafe()
                } else {
                    // Draw without evaluation colors
                    OpenCvUtils.drawPoints(
                        coloredWarped,
                        points = detectionResult.bubbles.map { it.point },
                        fillColor = Scalar(255.0, 255.0, 255.0),
                        textColor = Scalar(0.0, 255.0, 255.0)
                    ).toBitmapSafe()
                }
                coloredWarped.release()


                return@withContext OmrImageProcessResult(
                    success = true,
                    debugBitmaps = debugBitmaps,
                    finalBitmap = finalBitmap,
                    detectedBubbles = detectionResult.bubbles,
                    evaluation = evaluationResult
                ) to centersInBuffer
            } else {
                return@withContext OmrImageProcessResult(
                    success = false,
                    reason = "Expected 4 anchors, found ${centersInBuffer.size}",
                    debugBitmaps = debugBitmaps
                ) to centersInBuffer
            }
        } catch (e: Exception) {
            android.util.Log.e("OmrScannerViewModel", "Error processing OMR frame", e)
            return@withContext OmrImageProcessResult(
                success = false,
                reason = "Error: ${e.message}",
                debugBitmaps = debugBitmaps
            ) to listOf()
        } finally {
            // IMPORTANT: Release Mat objects to prevent memory leaks
            gray?.release()
            warped?.release()
        }
    }
}