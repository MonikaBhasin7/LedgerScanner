package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.android.Utils.bitmapToMat
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

object Try {

    @WorkerThread
    fun processOMRWithTemplate(
        inputBitmap: Bitmap,
        template: Template,
        debug: Boolean = true
    ): PreprocessResult {
        val debugMap = mutableMapOf<String, Bitmap>()
        // 1. Convert to grayscale
        val tplMatBgr = Mat()
        Utils.bitmapToMat(inputBitmap, tplMatBgr)
        val gray = Mat()
        Imgproc.cvtColor(tplMatBgr, gray, Imgproc.COLOR_BGR2GRAY)
        if (debug) debugMap["gray"] = matToBitmapSafe(gray)

        // 2. Detect 4 fiducial dots
        val fiducials = detectAnchorSquares(gray)
        if (fiducials.size != 4) {
            return PreprocessResult(
                ok = false,
                reason = "Could not detect 4 fiducial dots",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = debugMap
            )
        }

        // 4. Draw the fiducial dots for debug
        val squareAnchorPoints = drawPoints(tplMatBgr, fiducials, Scalar(0.0, 0.0, 255.0))
        if (debug) debugMap["fiducials"] = matToBitmapSafe(squareAnchorPoints)

        detectBubbleCenters(tplMatBgr)

        return PreprocessResult(
            ok = true,
            reason = "Could not detect 4 fiducial dots",
            warpedBitmap = null,
            transformMatrix = null,
            confidence = 0.0,
            intermediate = debugMap
        )
    }

    fun detectBubbleCenters(gray: Mat): List<Point> {
        val blurred = Mat()
        Imgproc.GaussianBlur(gray, blurred, Size(9.0, 9.0), 2.0)

        val circles = Mat()
        Imgproc.HoughCircles(
            blurred,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,                 // dp: inverse ratio of resolution
            20.0,                // minDist between centers
            100.0,               // param1: higher Canny threshold
            30.0,                // param2: smaller = more circles, larger = fewer
            10,                  // minRadius
            30                   // maxRadius
        )

        val centers = mutableListOf<Point>()
        for (i in 0 until circles.cols()) {
            val data = circles.get(0, i) ?: continue
            val x = data[0]
            val y = data[1]
            val r = data[2]
            centers.add(Point(x, y))
            // you can draw circle here if you want
        }

        blurred.release()
        circles.release()
        return centers
    }

    fun detectAnchorSquares(gray: Mat, debug: Boolean = false): List<Point> {
        val anchors = mutableListOf<Point>()

        // 1. Threshold (since anchors are black)
        val bin = Mat()
        Imgproc.threshold(gray, bin, 50.0, 255.0, Imgproc.THRESH_BINARY_INV)

        // 2. Find contours
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(bin, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // 3. Filter contours that look like squares
        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.04 * peri, true)

            // Keep only quadrilaterals
            if (approx.total() == 4L) {
                val rect = Imgproc.boundingRect(MatOfPoint(*approx.toArray()))
                val aspect = rect.width.toDouble() / rect.height.toDouble()
                if (aspect > 0.8 && aspect < 1.2) { // close to square
                    val area = rect.width * rect.height
                    if (area > 200 && area < 5000) { // filter by size
                        val center = Point(
                            rect.x + rect.width / 2.0,
                            rect.y + rect.height / 2.0
                        )
                        anchors.add(center)
                    }
                }
            }
        }

        // 4. Optional debug image
        if (debug) {
            val debugMat = Mat()
            Imgproc.cvtColor(gray, debugMat, Imgproc.COLOR_GRAY2BGR)
            for (pt in anchors) {
                Imgproc.circle(debugMat, pt, 10, Scalar(0.0, 0.0, 255.0), 2)
            }
            // Save or show debug image
        }

        bin.release()
        hierarchy.release()

        return anchors
    }

    fun drawPoints(src: Mat, points: List<Point>, color: Scalar = Scalar(0.0, 0.0, 255.0)): Mat {
        val out = src.clone()

        for ((i, p) in points.withIndex()) {
            // Draw small circle
            Imgproc.circle(out, p, 10, color, -1)

            // Label point index (TL=0, TR=1, BR=2, BL=3)
            Imgproc.putText(
                out,
                "$i",
                Point(p.x + 15, p.y),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.8,
                color,
                2
            )
        }
        return out
    }

    private fun matToBitmapSafe(mat: Mat): Bitmap {
        val tmp = Mat()
        when (mat.channels()) {
            1 -> Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_GRAY2RGBA)
            3 -> Imgproc.cvtColor(mat, tmp, Imgproc.COLOR_BGR2RGBA)
            4 -> mat.copyTo(tmp)
            else -> mat.copyTo(tmp)
        }
        val bmp = createBitmap(tmp.cols(), tmp.rows())
        Utils.matToBitmap(tmp, bmp)
        tmp.release()
        return bmp
    }
}