package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.base.errors.ErrorMessages
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.feature.scanner.results.model.AnchorDetectionResult
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.results.model.OmrProcessingContext
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessQualityReport
import com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel
import com.example.ledgerscanner.feature.scanner.scan.utils.AnswerEvaluator
import com.example.ledgerscanner.feature.scanner.scan.utils.BubbleAnalyzer
import com.example.ledgerscanner.feature.scanner.scan.utils.ImageQualityChecker
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

// OmrScannerViewModel.kt
@HiltViewModel
class OmrScannerViewModel @Inject constructor(
    private val omrProcessor: OmrProcessor,
    private val templateProcessor: TemplateProcessor,
    private val answerEvaluator: AnswerEvaluator,
    private val imageQualityChecker: ImageQualityChecker,
    private val bubbleAnalyzer: BubbleAnalyzer
) : ViewModel() {

    // Keep this for the result screen
    private val _omrImageProcessResult = MutableStateFlow<OmrImageProcessResult?>(null)
    val omrImageProcessResult = _omrImageProcessResult.asStateFlow()

    private val _brightnessQuality = MutableStateFlow<BrightnessQualityReport?>(null)
    val brightnessQuality = _brightnessQuality.asStateFlow()

    fun setCapturedResult(result: OmrImageProcessResult) {
        _omrImageProcessResult.value = result
    }

    // Return the result directly instead of updating StateFlow
    suspend fun processOmrFrame(
        image: ImageProxy,
        examEntity: ExamEntity,
        anchorSquaresOnPreviewScreen: List<RectF>,
        previewRect: RectF,
        debug: Boolean = false
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> = withContext(Dispatchers.Default) {

        val processingContext = OmrProcessingContext()

        try {
            // Step 1: Validate inputs
            if (!(anchorSquaresOnPreviewScreen.isNotEmpty() && previewRect.width() > 0f)) {
                return@withContext OmrImageProcessResult(
                    success = false,
                    reason = ErrorMessages.ANCHORS_NOT_DETECTED
                ) to emptyList()
            }

            // Step 2: Convert image to grayscale Mat
            processingContext.grayMat =
                ImageConversionUtils.imageProxyToGrayMatUpright(image).also {
                    if (debug) processingContext.debugBitmaps["image_proxy_gray_mat"] = it.toBitmapSafe()
                }

            // Step 2.5: CHECK BRIGHTNESS QUALITY (NEW)
            val brightnessReport = imageQualityChecker.checkBrightness(
                processingContext.grayMat!!,
                analyzeHistogram = true
            )
            _brightnessQuality.value = brightnessReport

            // Log brightness quality
            Log.d(TAG, "Brightness Quality: ${brightnessReport.brightnessCheck.level}, " +
                    "Value: ${brightnessReport.brightnessCheck.value.toInt()}")

            // Optionally warn if quality is poor/failed (but continue processing)
            if (brightnessReport.brightnessCheck.level <= QualityLevel.POOR) {
                Log.w(TAG, "Poor brightness detected: ${brightnessReport.brightnessCheck.suggestion}")
            }

            // Step 3: Detect anchor centers
            val anchorDetectionResult = detectAnchorCenters(
                overlayRects = anchorSquaresOnPreviewScreen,
                previewRect = previewRect,
                grayMat = processingContext.grayMat!!,
                debugBitmaps = processingContext.debugBitmaps,
                debug = debug
            )

            if (!anchorDetectionResult.success) {
                return@withContext OmrImageProcessResult(
                    success = false,
                    reason = ErrorMessages.ANCHORS_EXPECTED_COUNT_MISMATCH,
                    debugBitmaps = processingContext.debugBitmaps
                ) to anchorDetectionResult.centers
            }

            // Step 4: Warp image
            val (warpedMat, warpedDebugBitmap) = omrProcessor.warpWithTemplateAndGetWarped(
                gray = processingContext.grayMat!!,
                omrTemplate = examEntity.template,
                centersInBuffer = anchorDetectionResult.centers
            )
            processingContext.warpedMat = warpedMat
            if (debug && warpedDebugBitmap != null) {
                processingContext.debugBitmaps["warped"] = warpedDebugBitmap
            }

            // Step 5: Process bubbles and evaluate
            return@withContext processBubblesAndEvaluate(
                processingContext = processingContext,
                examEntity = examEntity,
                centers = anchorDetectionResult.centers,
                debug = debug
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing OMR frame", e)
            return@withContext OmrImageProcessResult(
                success = false,
                reason = "Error: ${e.message}",
                debugBitmaps = processingContext.debugBitmaps
            ) to emptyList()
        } finally {
            processingContext.release()
        }
    }

    private fun detectAnchorCenters(
        overlayRects: List<RectF>,
        previewRect: RectF,
        grayMat: Mat,
        debugBitmaps: MutableMap<String, Bitmap>,
        debug: Boolean
    ): AnchorDetectionResult {
        val centersInBuffer = mutableListOf<AnchorPoint>()

        val allFound = omrProcessor.findCentersInBuffer(
            overlayRects = overlayRects,
            previewRect = previewRect,
            gray = grayMat,
            centersOut = centersInBuffer,
            onDebug = { key, image ->
                debugBitmaps[key] = image
            }
        )

        if (debug && centersInBuffer.isNotEmpty()) {
            val grayWithAnchors = grayMat.clone()
            OpenCvUtils.drawPoints(grayWithAnchors, points = centersInBuffer).apply {
                debugBitmaps["full_image_with_anchors"] = toBitmapSafe()
                release()
            }
        }

        return AnchorDetectionResult(allFound, centersInBuffer)
    }

    private fun processBubblesAndEvaluate(
        processingContext: OmrProcessingContext,
        examEntity: ExamEntity,
        centers: List<AnchorPoint>,
        debug: Boolean
    ): Pair<OmrImageProcessResult, List<AnchorPoint>> {
        if (centers.size != EXPECTED_ANCHOR_COUNT) {
            return OmrImageProcessResult(
                success = false,
                reason = "Expected $EXPECTED_ANCHOR_COUNT anchors, found ${centers.size}",
                debugBitmaps = processingContext.debugBitmaps
            ) to centers
        }

        val template = examEntity.template
        val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(template)

        if (bubblePoints.size != template.totalBubbles()) {
            return OmrImageProcessResult(
                success = false,
                reason = "Bubble count mismatch: expected ${template.totalBubbles()}, got ${bubblePoints.size}",
                debugBitmaps = processingContext.debugBitmaps
            ) to centers
        }

        val detectionResult = bubbleAnalyzer.detectFilledBubbles(
            template,
            processingContext.warpedMat!!
        )

        if (debug) {
            addBubbleDetectionDebugBitmap(
                processingContext.warpedMat!!,
                detectionResult.bubbles,
                processingContext.debugBitmaps
            )
        }

        val evaluationResult = evaluateAnswersIfPossible(examEntity, detectionResult.bubbles)
        val finalBitmap = createFinalResultBitmap(
            processingContext.warpedMat!!,
            detectionResult.bubbles,
            evaluationResult
        )

        return OmrImageProcessResult(
            success = true,
            debugBitmaps = processingContext.debugBitmaps,
            finalBitmap = finalBitmap,
            detectedBubbles = detectionResult.bubbles,
            evaluation = evaluationResult
        ) to centers
    }

    private fun addBubbleDetectionDebugBitmap(
        warpedMat: Mat,
        bubbles: List<BubbleResult>,
        debugBitmaps: MutableMap<String, Bitmap>
    ) {
        val bubbleDebugMat = warpedMat.clone()
        OpenCvUtils.drawPoints(
            bubbleDebugMat,
            points = bubbles.map { it.point },
            fillColor = Scalar(255.0, 255.0, 255.0),
            textColor = Scalar(0.0, 255.0, 255.0)
        ).apply {
            debugBitmaps["bubbles_detected"] = toBitmapSafe()
            release()
        }
    }

    private fun evaluateAnswersIfPossible(
        examEntity: ExamEntity,
        detectedBubbles: List<BubbleResult>
    ): EvaluationResult? {
        return examEntity.answerKey?.let {
            answerEvaluator.evaluateAnswers(detectedBubbles, examEntity)
        }
    }

    private fun createFinalResultBitmap(
        warpedMat: Mat,
        bubbles: List<BubbleResult>,
        evaluation: EvaluationResult?
    ): Bitmap {
        val coloredWarped = warpedMat.toColoredWarped()

        return try {
            if (evaluation != null) {
                OpenCvUtils.drawPointsWithEvaluation(
                    coloredWarped,
                    bubbles = bubbles,
                    evaluation = evaluation
                ).toBitmapSafe()
            } else {
                OpenCvUtils.drawPoints(
                    coloredWarped,
                    points = bubbles.map { it.point },
                    fillColor = Scalar(255.0, 255.0, 255.0),
                    textColor = Scalar(0.0, 255.0, 255.0)
                ).toBitmapSafe()
            }
        } finally {
            coloredWarped.release()
        }
    }

    companion object {
        private const val TAG = "OmrScannerViewModel"
        private const val EXPECTED_ANCHOR_COUNT = 4
    }
}