package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.preCleanGray
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

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

        for ((index, screenRect) in overlayRects.withIndex()) {
            // map screen overlay square -> Mat rect (same logic)
            val matRect = ImageConversionUtils.screenRectToMatRect(screenRect, previewRect, gray)

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
        val templateAnchors = listOf(
            omrTemplate.anchor_top_left,
            omrTemplate.anchor_top_right,
            omrTemplate.anchor_bottom_right,
            omrTemplate.anchor_bottom_left
        )
        val warped = warpWithAnchors(
            src = gray,
            detectedAnchors = centersInBuffer,
            templateAnchors = templateAnchors,
            templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
        )

        val warpedBitmap: Bitmap? =
            if (debug) warped.toBitmapSafe() else null

        return Pair(warped, warpedBitmap)
    }

}