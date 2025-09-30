package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.OmrUtils
import com.example.ledgerscanner.base.utils.preCleanGray
import com.example.ledgerscanner.base.utils.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.OmrImageProcessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class OmrProcessor {

    companion object {
        const val TAG = "OmrProcessor"
        val templateProcessor = TemplateProcessor()
    }

    @WorkerThread
    fun processOmrSheet(template: Template, inputBitmap: Bitmap, debug : Boolean = false): OmrImageProcessResult {
        val debugMap = mutableMapOf<String, Bitmap>()

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


        val debugPoints = mutableListOf<Point>()
        for (q in template.questions) {
            for (o in q.options) {
                debugPoints.add(
                    Point(
                        o.x + template.anchor_top_left.x,
                        o.y + template.anchor_top_left.y
                    )
                )
            }
        }
        debugMap["first-highlight"] = OmrUtils.drawPoints(
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
        detectedAnchors: List<Point>,
        templateAnchors: List<Point>,
        templateSize: Size
    ): Mat {
        val srcPoints = MatOfPoint2f(*detectedAnchors.toTypedArray())
        val dstPoints = MatOfPoint2f(*templateAnchors.toTypedArray())
        val H = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        val warped = Mat()
        Imgproc.warpPerspective(src, warped, H, templateSize)
        return warped
    }

}