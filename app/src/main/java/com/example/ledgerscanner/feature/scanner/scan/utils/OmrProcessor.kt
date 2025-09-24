package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.OmrUtils
import com.example.ledgerscanner.base.utils.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class OmrProcessor {

    companion object {
        val templateProcessor = TemplateProcessor()
        val cropper = PageCropper()
    }

    @WorkerThread
    fun processOmrSheet(template: Template, inputBitmap: Bitmap): PreprocessResult {
        val debugMap = mutableMapOf<String, Bitmap>()

        val srcMat = Mat()
        Utils.bitmapToMat(inputBitmap, srcMat)
        val gray = Mat()
        Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_BGR2GRAY)
        debugMap["gray"] = gray.toBitmapSafe()


        // 2. Detect 4 anchor squares in scanned sheet
        val detectedAnchors = templateProcessor.detectAnchorSquares(gray)
        if (detectedAnchors.size != 4) {
            return PreprocessResult(
                ok = false,
                reason = "Could not detect 4 anchors",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = debugMap
            )
        }
        debugMap["anchors"] = OmrUtils.drawPoints(srcMat, detectedAnchors).toBitmapSafe()

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
            size = Size(template.sheet_width, template.sheet_height)
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
            debugPoints
        ).toBitmapSafe()


        return PreprocessResult(
            ok = true,
            reason = null,
            warpedBitmap = warped.toBitmapSafe(),
            transformMatrix = null,
            confidence = 1.0,
            intermediate = debugMap,
        )
    }

    fun warpWithAnchors(
        src: Mat,
        detectedAnchors: List<Point>,
        templateAnchors: List<Point>,
        size: Size
    ): Mat {
        val srcPoints = MatOfPoint2f(*detectedAnchors.toTypedArray())
        val dstPoints = MatOfPoint2f(*templateAnchors.toTypedArray())
        val H = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

        val warped = Mat()
        Imgproc.warpPerspective(src, warped, H, size)
        return warped
    }
}