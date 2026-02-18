package com.example.ledgerscanner.feature.scanner.scan.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.example.ledgerscanner.base.errors.ErrorMessages
import com.example.ledgerscanner.base.network.UiState
import com.example.ledgerscanner.base.utils.StorageUtils
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.base.utils.image.toColoredWarped
import com.example.ledgerscanner.database.entity.ExamEntity
import com.example.ledgerscanner.database.entity.ScanResultEntity
import com.example.ledgerscanner.feature.scanner.results.model.AnchorDetectionResult
import com.example.ledgerscanner.feature.scanner.results.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.results.model.EvaluationResult
import com.example.ledgerscanner.feature.scanner.results.model.OmrProcessingContext
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.BrightnessQualityReport
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.QualityLevel
import com.example.ledgerscanner.feature.scanner.scan.utils.AnchorGeometryValidator
import com.example.ledgerscanner.feature.scanner.scan.utils.AnswerEvaluator
import com.example.ledgerscanner.feature.scanner.scan.utils.BarcodeScanner
import com.example.ledgerscanner.feature.scanner.scan.utils.BubbleAnalyzer
import com.example.ledgerscanner.feature.scanner.scan.utils.EnrollmentReader
import com.example.ledgerscanner.feature.scanner.scan.utils.FrameStabilityTracker
import com.example.ledgerscanner.feature.scanner.scan.utils.GeometryValidationResult
import com.example.ledgerscanner.feature.scanner.scan.utils.ImageQualityChecker
import com.example.ledgerscanner.feature.scanner.scan.utils.OmrProcessor
import com.example.ledgerscanner.feature.scanner.scan.utils.StabilityResult
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
    private val bubbleAnalyzer: BubbleAnalyzer,
    private val barcodeScanner: BarcodeScanner,
    private val enrollmentReader: EnrollmentReader,
    private val frameStabilityTracker: FrameStabilityTracker,
    private val anchorGeometryValidator: AnchorGeometryValidator
) : ViewModel() {

    // Keep this for the result screen
    private val _omrImageProcessResult = MutableStateFlow<ScanResultEntity?>(null)
    val omrImageProcessResult = _omrImageProcessResult.asStateFlow()

    private val _brightnessQuality = MutableStateFlow<BrightnessQualityReport?>(null)
    val brightnessQuality = _brightnessQuality.asStateFlow()

    private val _isScanningEnabled = MutableStateFlow(true)
    val isScanningEnabled = _isScanningEnabled.asStateFlow()

    fun setCapturedResult(result: ScanResultEntity?) {
        _omrImageProcessResult.value = result
        _isScanningEnabled.value = false
    }
    fun enableScanning() {
        _isScanningEnabled.value = true
        _omrImageProcessResult.value = null
        _brightnessQuality.value = null
        frameStabilityTracker.reset()
    }

    fun disableScanning() {
        _isScanningEnabled.value = false
    }

    suspend fun processOmrFrame(
        context: Context,
        image: ImageProxy,
        examEntity: ExamEntity,
        anchorRegions: List<RectF>,
        previewBounds: RectF,
        debug: Boolean = false,
        onAnchorsDetected: (List<AnchorPoint>) -> Unit,
        onStabilityUpdate: (StabilityResult) -> Unit = {},
        onGeometryUpdate: (GeometryValidationResult) -> Unit = {},
        onBrightnessUpdate: (QualityLevel) -> Unit = {}
    ): UiState<ScanResultEntity> = withContext(Dispatchers.Default) {
        val processingContext = OmrProcessingContext()

        try {
            // Step 1: Validate inputs
            if (!(anchorRegions.isNotEmpty() && previewBounds.width() > 0f)) {
                onAnchorsDetected(emptyList())
                return@withContext UiState.Error(ErrorMessages.ANCHORS_NOT_DETECTED)
            }

            // Step 2: Convert image to grayscale Mat
            processingContext.grayMat =
                ImageConversionUtils.imageProxyToGrayMatUpright(image).also {
                    if (debug) processingContext.debugBitmaps["image_proxy_gray_mat"] =
                        it.toBitmapSafe()
                }

            processingContext.rawBitmap = processingContext.grayMat?.toBitmapSafe()

            // Step 2.5: CHECK BRIGHTNESS QUALITY (NEW)
            val brightnessReport = imageQualityChecker.checkBrightness(
                processingContext.grayMat!!,
                analyzeHistogram = false
            )
            _brightnessQuality.value = brightnessReport
            onBrightnessUpdate(brightnessReport.brightnessCheck.level)

            // Log brightness quality
            Log.d(
                TAG, "Brightness Quality: ${brightnessReport.brightnessCheck.level}, " +
                        "Value: ${brightnessReport.brightnessCheck.value.toInt()}"
            )

            // BUG FIX: Block processing when brightness is FAILED
            // (prevents garbage results from extremely dark/bright images)
            if (brightnessReport.brightnessCheck.level == QualityLevel.FAILED) {
                onAnchorsDetected(emptyList())
                return@withContext UiState.Error(
                    brightnessReport.brightnessCheck.suggestion
                        ?: ErrorMessages.IMAGE_PROCESSING_FAILED
                )
            }

            if (brightnessReport.brightnessCheck.level <= QualityLevel.POOR) {
                Log.w(
                    TAG,
                    "Poor brightness detected: ${brightnessReport.brightnessCheck.suggestion}"
                )
            }

            // Step 3: Detect anchor centers
            val anchorDetectionResult = detectAnchorCenters(
                overlayRects = anchorRegions,
                previewRect = previewBounds,
                grayMat = processingContext.grayMat!!,
                debugBitmaps = processingContext.debugBitmaps,
                debug = debug
            )

            onAnchorsDetected(anchorDetectionResult.centers)

            // BUG FIX: Require exactly 4 anchors before attempting warp
            // (previously would pass partial anchors to getPerspectiveTransform → crash/garbage)
            if (anchorDetectionResult.centers.size != EXPECTED_ANCHOR_COUNT) {
                frameStabilityTracker.reset()
                onStabilityUpdate(StabilityResult(false, 0, FrameStabilityTracker.REQUIRED_STABLE_FRAMES, 0.0))
                return@withContext UiState.Error(ErrorMessages.ANCHORS_NOT_DETECTED)
            }

            // Step 3.5: Geometry validation — reject curved/uneven sheets
            val geometry = anchorGeometryValidator.validate(anchorDetectionResult.centers)
            onGeometryUpdate(geometry)
            if (!geometry.isValid) {
                frameStabilityTracker.reset() // force re-stabilize after user adjusts sheet
                return@withContext UiState.Error(
                    geometry.rejectionReason ?: "Sheet is not flat. Please flatten it"
                )
            }

            // Step 3.7: Lightweight stability gate with fast path for high-confidence frames
            val stability = frameStabilityTracker.addFrame(anchorDetectionResult.centers)
            onStabilityUpdate(stability)
            val highConfidenceFastPath =
                anchorDetectionResult.success && stability.maxMovement <= FAST_PATH_MAX_MOVEMENT_PX
            if (!stability.isStable && !highConfidenceFastPath) {
                return@withContext UiState.Idle()
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

            // Step 5: Scan barcode from warped image
            val barcodeValue = try {
                barcodeScanner.scanBarcode(processingContext.warpedMat!!)
            } catch (e: Exception) {
                Log.w(TAG, "Barcode scan failed, continuing without it", e)
                null
            }
            if (barcodeValue != null) {
                Log.d(TAG, "Barcode detected: $barcodeValue")
            }

            // Step 6: Process bubbles and evaluate
            val omrImageProcessResult = processBubblesAndEvaluate(
                processingContext = processingContext,
                examEntity = examEntity,
                centers = anchorDetectionResult.centers,
                debug = debug
            ).copy(barcodeId = barcodeValue)

            return@withContext getScanEntityObject(context, examEntity, omrImageProcessResult)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing OMR frame", e)
            return@withContext UiState.Error(e.message ?: ErrorMessages.SCAN_RESULTS_LOAD_FAILED)
        } finally {
            processingContext.release()
        }
    }

    suspend fun getScanEntityObject(
        context: Context,
        examEntity: ExamEntity,
        omrImageProcessResult: OmrImageProcessResult
    ): UiState<ScanResultEntity> {
        try {
            require(omrImageProcessResult.success) {
                ErrorMessages.IMAGE_PROCESSING_FAILED
            }

            val evaluation = omrImageProcessResult.evaluation
                ?: return UiState.Error(ErrorMessages.EVALUATION_FAILED)

            val detectedBubbles = omrImageProcessResult.detectedBubbles
                ?: return UiState.Error(ErrorMessages.BUBBLE_DETECTION_FAILED)

            // Save images
            val scannedImagePath = omrImageProcessResult.finalBitmap?.let { bitmap ->
                StorageUtils.saveImageToStorage(
                    context,
                    bitmap,
                    "scan_${System.currentTimeMillis()}.jpg"
                )
            } ?: return UiState.Error(ErrorMessages.IMAGE_SAVE_FAILED)

            val thumbnailPath = omrImageProcessResult.finalBitmap.let { bitmap ->
                val thumbnail = StorageUtils.createThumbnail(bitmap, 300, 400)
                StorageUtils.saveImageToStorage(
                    context,
                    thumbnail,
                    "thumb_${System.currentTimeMillis()}.jpg"
                )
            }

            val debugImagePaths = mutableMapOf<String, String>()
            omrImageProcessResult.debugBitmaps.forEach { (label, bitmap) ->
                try {
                    val debugPath = StorageUtils.saveImageToStorage(
                        context,
                        bitmap,
                        "debug_${label}_${System.currentTimeMillis()}.jpg"
                    )
                    debugImagePaths[label] = debugPath
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save debug image: $label", e)
                    // Continue saving other images even if one fails
                }
            }

            val rawImagePath = omrImageProcessResult.rawBitmap?.let { bitmap ->
                StorageUtils.saveImageToStorage(
                    context,
                    bitmap,
                    "raw_${System.currentTimeMillis()}.jpg"
                )
            } ?: return UiState.Error(ErrorMessages.IMAGE_SAVE_FAILED)

            // Read enrollment number from process result
            val enrollmentNumber = omrImageProcessResult.enrollmentNumber

            // Convert detected bubbles to answers
            val studentAnswers = mutableMapOf<Int, List<Int>>()

            evaluation.answerMap.forEach { (key, item) ->
                studentAnswers[key] = item.userSelected.toList()
            }

            val questionConfidences = mutableMapOf<Int, Double?>()
            detectedBubbles.groupBy { it.questionIndex }.forEach { (qIndex, bubbles) ->
                val maxConfidence = bubbles.maxOfOrNull { it.confidence }
                questionConfidences[qIndex] = maxConfidence
            }

            val allConfidences = detectedBubbles.map { it.confidence }
            val avgConfidence = if (allConfidences.isNotEmpty()) {
                allConfidences.average()
            } else null

            val minConfidence = allConfidences.minOrNull()


            val lowConfidenceQuestions = questionConfidences
                .filter { (it.value ?: 0.0) < BubbleAnalyzer.MIN_CONFIDENCE }


            return UiState.Success(
                ScanResultEntity(
                    examId = examEntity.id,
                    barCode = omrImageProcessResult.barcodeId,
                    enrollmentNumber = enrollmentNumber,
                    scannedImagePath = scannedImagePath,
                    thumbnailPath = thumbnailPath,
                    clickedRawImagePath = rawImagePath,
                    debugImagesPath = debugImagePaths,
                    scannedAt = System.currentTimeMillis(),
                    studentAnswers = studentAnswers,
                    multipleMarksDetected = evaluation.multipleMarksQuestions,
                    score = evaluation.marksObtained,
                    totalQuestions = evaluation.totalQuestions,
                    correctCount = evaluation.correctCount,
                    wrongCount = evaluation.incorrectCount,
                    blankCount = evaluation.unansweredCount,
                    scorePercent = evaluation.percentage,
                    questionConfidences = questionConfidences,
                    avgConfidence = avgConfidence,
                    minConfidence = minConfidence,
                    lowConfidenceQuestions = lowConfidenceQuestions,
                )
            )
        } catch (e: IllegalArgumentException) {
            return UiState.Error(ErrorMessages.IMAGE_PROCESSING_FAILED)
        } catch (e: IllegalStateException) {
            return UiState.Error(e.message ?: ErrorMessages.SCAN_RESULT_SAVE_FAILED)
        } catch (e: Exception) {
            Log.e("ScanResultRepository", "Error creating scan entity", e)
            return UiState.Error(ErrorMessages.SCAN_RESULT_SAVE_FAILED)
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
            enableDebug = debug,
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
    ): OmrImageProcessResult {
        if (centers.size != EXPECTED_ANCHOR_COUNT) {
            return OmrImageProcessResult(
                success = false,
                reason = "Expected $EXPECTED_ANCHOR_COUNT anchors, found ${centers.size}",
                debugBitmaps = processingContext.debugBitmaps
            )
        }

        val template = examEntity.template
        val bubblePoints = templateProcessor.mapTemplateBubblesToImagePoints(template)

        if (bubblePoints.size != template.totalBubbles()) {
            return OmrImageProcessResult(
                success = false,
                reason = "Bubble count mismatch: expected ${template.totalBubbles()}, got ${bubblePoints.size}",
                debugBitmaps = processingContext.debugBitmaps
            )
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

        // Read enrollment number (if template has enrollment grid)
        val enrollmentNumber = try {
            enrollmentReader.readEnrollmentNumber(
                template = template,
                warped = processingContext.warpedMat!!
            )
        } catch (e: Exception) {
            Log.w(TAG, "Enrollment reading failed, continuing without it", e)
            null
        }
        if (enrollmentNumber != null) {
            Log.d(TAG, "Enrollment number detected: $enrollmentNumber")
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
            evaluation = evaluationResult,
            rawBitmap = processingContext.rawBitmap,
            enrollmentNumber = enrollmentNumber
        )
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
        private const val FAST_PATH_MAX_MOVEMENT_PX = 2.0
    }
}
