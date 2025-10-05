package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.preCleanGray
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrDetectionResult
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject
import kotlin.math.roundToInt

class OmrProcessor @Inject constructor() {

    companion object {
        const val TAG = "OmrProcessor"
        val templateProcessor = TemplateProcessor()
    }

    @WorkerThread
    fun processOmrSheet(
        template: Template,
        inputBitmap: Bitmap,
        debug: Boolean = false
    ): OmrImageProcessResult {
        val debugMap = hashMapOf<String, Bitmap>()

        val srcMat = Mat()
        Utils.bitmapToMat(inputBitmap, srcMat)
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY)
        debugMap["gray"] = gray.toBitmapSafe()

        val grayClean = gray.preCleanGray(
            useClahe = true,        // turn off if paper already well-lit
            useBilateral = false    // set true if bubble edges look jagged/noisy
        )

        debugMap["gray-clean"] = grayClean.toBitmapSafe()

        // 2. Detect 4 anchor squares in scanned sheet
        val detectedAnchors = templateProcessor.detectAnchorPoints(
            srcMat, grayClean, true,
            debugMapAdditionCallback = { title, bitmap ->
                debugMap[title] = bitmap
            },
            failedCallback = { reason ->
                return OmrImageProcessResult(
                    success = false,
                    reason = reason,
                    debugBitmaps = debugMap
                )
            },
        ) ?: listOf()

        // 3. Warp scanned sheet to template’s canonical size
        val templateAnchors = listOf(
            template.anchor_top_left,
            template.anchor_top_right,
            template.anchor_bottom_right,
            template.anchor_bottom_left
        ) // anchors from your template JSON
        val warped = warpWithAnchors(
            src = gray,
            detectedAnchors = detectedAnchors,
            templateAnchors = templateAnchors,
            templateSize = Size(template.sheet_width, template.sheet_height)
        )
        debugMap["warped"] = warped.toBitmapSafe()


        val debugPoints = mutableListOf<AnchorPoint>()
        for (q in template.questions) {
            for (o in q.options) {
                debugPoints.add(
                    AnchorPoint(
                        o.x + template.anchor_top_left.x,
                        o.y + template.anchor_top_left.y
                    )
                )
            }
        }
        debugMap["first-highlight"] = OpenCvUtils.drawPoints(
            warped,
            debugPoints,
            fillColor = Scalar(255.0, 255.0, 255.0),
            textColor = Scalar(0.0, 255.0, 255.0),
        ).toBitmapSafe()


        return OmrImageProcessResult(
            success = true,
            reason = null,
            finalBitmap = warped.toBitmapSafe(),
            confidence = 1.0,
            debugBitmaps = debugMap,
        )
    }

    fun warpWithAnchors(
        src: Mat,
        detectedAnchors: List<AnchorPoint>,
        templateAnchors: List<AnchorPoint>,
        templateSize: Size
    ): Mat {
        val srcPoints = MatOfPoint2f(*detectedAnchors.map { Point(it.x, it.y) }
            .toTypedArray())
        val dstPoints = MatOfPoint2f(*templateAnchors.map { Point(it.x, it.y) }
            .toTypedArray())
        val H = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        val warped = Mat()
        Imgproc.warpPerspective(src, warped, H, templateSize)
        return warped
    }

    /**
     * Detect centers inside the provided overlay rects and fill centersOut.
     * Returns true if all anchors found, false otherwise.
     *
     * NOTE: This helper preserves the exact loop logic and debug bitmap recording.
     */
    fun findCentersInBuffer(
        overlayRects: List<RectF>,
        previewRect: RectF,
        gray: Mat,
        debug: Boolean,
        debugBitmaps: MutableMap<String, Bitmap>,
        centersOut: MutableList<AnchorPoint>
    ): Boolean {
        var allFound = true

        for ((index, overlayRect) in overlayRects.withIndex()) {
            // map screen overlay square -> Mat rect (same logic)
            val matRect = ImageConversionUtils.screenRectToMatRect(
                overlayRect,
                previewRect,
                gray
            )

            // Crop ROI directly with that rect
            val roi = Mat(gray, matRect)
            if (debug) debugBitmaps["roi - $index"] = roi.toBitmapSafe()

            // Detect anchor inside ROI (same implementation)
            val anchor = OpenCvUtils.detectAnchorInRoi(roi)
            roi.release()

            if (anchor == null) {
                allFound = false
            } else {
                centersOut += AnchorPoint(
                    (matRect.x + anchor.x.toFloat()).toDouble(),
                    (matRect.y + anchor.y.toFloat()).toDouble()
                )
            }
        }

        return allFound
    }

    /**
     * Performs the warp step using omrProcessor.warpWithAnchors and returns the warped Mat
     * plus an optional debug bitmap for warped (if debug=true)
     *
     * NOTE: Behavior unchanged: uses omrProcessor and templateProcessor as originally used.
     */
    fun warpWithTemplateAndGetWarped(
        gray: Mat,
        omrTemplate: Template,
        centersInBuffer: List<AnchorPoint>,
        debug: Boolean,
        debugBitmaps: MutableMap<String, Bitmap>
    ): Pair<Mat, Bitmap?> {
        val warped = warpWithAnchors(
            src = gray,
            detectedAnchors = centersInBuffer,
            templateAnchors = omrTemplate.getAnchorListClockwise(),
            templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
        )

        val warpedBitmap: Bitmap? =
            if (debug) warped.toBitmapSafe() else null

        return Pair(warped, warpedBitmap)
    }

    fun detectFilledBubbles(
        omrTemplate: Template,
        warped: Mat
    ): OmrDetectionResult {

        val correctOptionIndex = 1 // TODO: monika - placeholder change it
        val bubbleResults = mutableListOf<BubbleResult>()
        val marks = mutableListOf<Boolean>()

        omrTemplate.questions.forEachIndexed { _, question ->
            var isQuestionCorrect = false

            question.options.forEachIndexed { optionIndex, option ->
                val bubblePoint = AnchorPoint(
                    omrTemplate.anchor_top_left.x + option.x,
                    omrTemplate.anchor_top_left.y + option.y
                )

                val cx = bubblePoint.x.roundToInt().coerceIn(0, warped.cols() - 1)
                val cy = bubblePoint.y.roundToInt().coerceIn(0, warped.rows() - 1)

                val (isFilled, confidence) = isBubbleFilled(
                    srcMat = warped,
                    radius = omrTemplate.getAverageRadius(),
                    cx = cx.toLong(),
                    cy = cy.toLong()
                )

                val isMarked = isFilled && confidence >= 0.45

                if (isMarked && optionIndex == correctOptionIndex) {
                    isQuestionCorrect = true
                    bubbleResults.add(BubbleResult(bubblePoint, true))
                } else if (isMarked) {
                    isQuestionCorrect = false
                    bubbleResults.add(BubbleResult(bubblePoint, false))
                }
            }

            marks.add(isQuestionCorrect)
        }

        return OmrDetectionResult(
            bubbles = bubbleResults,
            marks = marks
        )
    }

    /**
     * Returns Pair(isFilled:Boolean, confidence:Double) where confidence is the fraction of dark pixels inside the circular mask (0..1).
     * The caller decides a fill threshold (fillFractionThreshold) to convert confidence -> boolean filled.
     */
    fun isBubbleFilled(
        srcMat: Mat,
        cx: Long,
        cy: Long,
        radius: Int,
        binarizeThreshold: Double = 127.0,
        fillFractionThreshold: Double = 0.5 // not used for returned confidence; caller will threshold
    ): Pair<Boolean, Double> {
        if (srcMat.empty()) return false to 0.0

        val cols = srcMat.cols()
        val rows = srcMat.rows()
        val x = cx.coerceIn(0, (cols - 1).toLong())
        val y = cy.coerceIn(0, (rows - 1).toLong())

        // convert to gray if necessary
        val gray = if (srcMat.channels() == 1) {
            srcMat.clone()
        } else {
            Mat().also { Imgproc.cvtColor(srcMat, it, Imgproc.COLOR_BGR2GRAY) }
        }

        // mask
        val mask = Mat.zeros(rows, cols, gray.type())
        Imgproc.circle(mask, Point(x.toDouble(), y.toDouble()), radius, Scalar(255.0), -1)

        // binary inverse: dark->255
        val binary = Mat()
        Imgproc.threshold(gray, binary, binarizeThreshold, 255.0, Imgproc.THRESH_BINARY_INV)

        // mean over mask -> fraction dark
        val meanScalar = Core.mean(binary, mask)
        val darkFraction = (meanScalar.`val`[0] / 255.0).coerceIn(0.0, 1.0)

        // cleanup
        if (gray !== srcMat) gray.release()
        mask.release()
        binary.release()

        val isFilled = darkFraction >= fillFractionThreshold
        return isFilled to darkFraction
    }

}