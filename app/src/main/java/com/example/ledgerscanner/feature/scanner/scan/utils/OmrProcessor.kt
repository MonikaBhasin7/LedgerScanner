package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.annotation.WorkerThread
import com.example.ledgerscanner.base.utils.image.ImageConversionUtils
import com.example.ledgerscanner.base.utils.image.OpenCvUtils
import com.example.ledgerscanner.base.utils.image.toBitmapSafe
import com.example.ledgerscanner.feature.scanner.scan.model.AnchorPoint
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

class OmrProcessor @Inject constructor() {

    companion object {
        private const val TAG = "OmrProcessor"
    }

    /**
     * Warps source image to template dimensions using perspective transformation
     *
     * @param src Source grayscale Mat
     * @param detectedAnchors Detected anchor points in source image
     * @param templateAnchors Expected anchor points from template
     * @param templateSize Target dimensions for warped image
     * @return Warped Mat in template coordinate space
     */
    @WorkerThread
    fun warpWithAnchors(
        src: Mat,
        detectedAnchors: List<AnchorPoint>,
        templateAnchors: List<AnchorPoint>,
        templateSize: Size
    ): Mat {
        var srcPoints: MatOfPoint2f? = null
        var dstPoints: MatOfPoint2f? = null
        var homography: Mat? = null

        try {
            srcPoints = MatOfPoint2f(*detectedAnchors.map { Point(it.x, it.y) }.toTypedArray())
            dstPoints = MatOfPoint2f(*templateAnchors.map { Point(it.x, it.y) }.toTypedArray())
            homography = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)

            val warped = Mat()
            Imgproc.warpPerspective(src, warped, homography, templateSize)

            return warped
        } finally {
            srcPoints?.release()
            dstPoints?.release()
            homography?.release()
        }
    }

    /**
     * Detects anchor centers within overlay rectangles on screen
     *
     * @param overlayRects Screen rectangles where anchors are expected
     * @param previewRect Camera preview rectangle bounds
     * @param gray Grayscale image from camera
     * @param debug Enable debug bitmap generation
     * @param debugBitmaps Map to store debug bitmaps
     * @param centersOut Output list to populate with detected centers
     * @return true if all anchors found, false otherwise
     */
    @WorkerThread
    fun findCentersInBuffer(
        overlayRects: List<RectF>,
        previewRect: RectF,
        gray: Mat,
        centersOut: MutableList<AnchorPoint>,
        enableDebug: Boolean = false,
        onDebug: (String, Bitmap) -> Unit
    ): Boolean {
        var allAnchorsFound = true
        // Defensive snapshot to avoid ConcurrentModificationException when UI thread updates overlay.
        val overlaySnapshot = overlayRects.map { RectF(it) }

        overlaySnapshot.forEachIndexed { index, overlayRect ->
            var roi: Mat? = null

            try {
                // Map screen coordinates to Mat coordinates
                val matRect = ImageConversionUtils.screenRectToMatRect(
                    overlayRect,
                    previewRect,
                    gray
                )

                // Extract region of interest
                roi = Mat(gray, matRect)

                if (enableDebug) {
                    onDebug("roi_anchor_$index", roi.toBitmapSafe())
                }

                // Detect anchor in ROI
                val anchorInRoi = OpenCvUtils.detectAnchorInRoi(roi)

                if (anchorInRoi == null) {
                    allAnchorsFound = false
                    Log.w(TAG, "Anchor $index not detected in ROI")
                } else {
                    // Convert ROI coordinates to full image coordinates
                    val anchorInImage = AnchorPoint(
                        x = (matRect.x + anchorInRoi.x.toFloat()).toDouble(),
                        y = (matRect.y + anchorInRoi.y.toFloat()).toDouble()
                    )
                    centersOut.add(anchorInImage)
                }
            } finally {
                roi?.release()
            }
        }

        return allAnchorsFound
    }

    /**
     * Warps image to template space and optionally generates debug bitmap
     *
     * @param gray Grayscale image to warp
     * @param omrTemplate Template containing anchor positions and dimensions
     * @param centersInBuffer Detected anchor centers
     * @param debug Enable debug bitmap generation
     * @param debugBitmaps Map to store debug bitmaps
     * @return Pair of (warped Mat, optional debug bitmap)
     */
    @WorkerThread
    fun warpWithTemplateAndGetWarped(
        gray: Mat,
        omrTemplate: Template,
        centersInBuffer: List<AnchorPoint>,
    ): Pair<Mat, Bitmap?> {
        val warped = warpWithAnchors(
            src = gray,
            detectedAnchors = centersInBuffer,
            templateAnchors = omrTemplate.getAnchorListClockwise(),
            templateSize = Size(omrTemplate.sheet_width, omrTemplate.sheet_height)
        )

        return warped to warped.toBitmapSafe()
    }
}
