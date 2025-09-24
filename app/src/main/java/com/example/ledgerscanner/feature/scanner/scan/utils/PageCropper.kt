package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.annotation.WorkerThread
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class PageCropper {

    /**
     * Detects the largest 4-point document contour, warps it into a rectangle and returns:
     * - warpedBitmap (if found) or original if not found
     * - intermediate debug bitmaps map (if debug = true)
     *
     * Usage: call off the UI thread.
     */
    @WorkerThread
    fun detectAndCropPage(
        inputBitmap: Bitmap,
        targetWidth: Int? = null,       // if provided, final warped image will be resized to this width/height aspect
        targetHeight: Int? = null,
        debug: Boolean = true
    ): PreprocessResult {
        val debugMap = mutableMapOf<String, Bitmap>()
        var srcMat: Mat? = null
        var gray = Mat()
        try {
            // 1) Bitmap -> Mat (BGR)
            srcMat = Mat()
            Utils.bitmapToMat(inputBitmap, srcMat)
            Imgproc.cvtColor(srcMat, gray, Imgproc.COLOR_RGBA2GRAY)

            if (debug) debugMap["gray"] = matToBitmapSafe(gray)

            // 2) Smooth + Canny edges
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val edges = Mat()
            Imgproc.Canny(blurred, edges, 50.0, 150.0)
            // Dilate a bit so broken edges join
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.dilate(edges, edges, kernel)

            if (debug) debugMap["edges"] = matToBitmapSafe(edges)

            // 3) Find contours and choose largest polygon with 4 points
            val contours = mutableListOf<MatOfPoint>()
            Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

            // sort by contour area desc
            contours.sortByDescending { Imgproc.contourArea(it) }

            var pageQuad: MatOfPoint2f? = null
            for (c in contours) {
                val area = Imgproc.contourArea(c)
                if (area < 1000) { // skip tiny
                    c.release()
                    continue
                }
                val approx = MatOfPoint2f()
                val c2f = MatOfPoint2f(*c.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
                c2f.release()
                // require 4-point polygon
                if (approx.total() == 4L) {
                    // found
                    val ordered = orderCorners(approx.toArray())
                    pageQuad = MatOfPoint2f(*ordered)
                    approx.release()
                    break
                }
                approx.release()
                c.release()
            }

            // Debug: draw selected quad on original
            if (debug) {
                val overlay = Mat()
                Imgproc.cvtColor(gray, overlay, Imgproc.COLOR_GRAY2RGBA)
                pageQuad?.let { quad ->
                    val poly = MatOfPoint(*quad.toArray())
                    Imgproc.drawContours(overlay, listOf(poly), -1, Scalar(0.0, 255.0, 0.0, 255.0), 6)
                    poly.release()
                }
                debugMap["selected_quad"] = matToBitmapSafe(overlay)
                overlay.release()
            }

            // 4) If no quad, return original but include debug
            if (pageQuad == null) {
                // cleanup mats
                blurred.release(); edges.release(); kernel.release()
                contours.forEach { it.release() }
                gray.release()
                srcMat?.release()
                return PreprocessResult(
                    ok = false,
                    reason = "Page contour not found",
                    warpedBitmap = inputBitmap,
                    transformMatrix = null,
                    confidence = 0.0,
                    intermediate = debugMap
                )
            }

            // 5) Compute destination rectangle size from quad geometry
            val srcPts = pageQuad.toArray()
            val widthA = distance(srcPts[2], srcPts[3]) // bottom-right to bottom-left
            val widthB = distance(srcPts[1], srcPts[0]) // top-right to top-left
            val maxWidth = max(widthA, widthB).toInt()

            val heightA = distance(srcPts[1], srcPts[2]) // top-right to bottom-right
            val heightB = distance(srcPts[0], srcPts[3]) // top-left to bottom-left
            val maxHeight = max(heightA, heightB).toInt()

            // if target size provided, respect aspect ratio of target
            val dstW: Int
            val dstH: Int
            if (targetWidth != null && targetHeight != null) {
                dstW = targetWidth
                dstH = targetHeight
            } else {
                // clamp to reasonable numbers to avoid enormous mats; keep size similar to original
                val longSide = max(maxWidth, maxHeight)
                val scale = if (longSide > 3000) 3000.0 / longSide else 1.0
                dstW = max(100, (maxWidth * scale).toInt())
                dstH = max(100, (maxHeight * scale).toInt())
            }

            // Destination points: TL, TR, BR, BL
            val dst = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(dstW.toDouble() - 1.0, 0.0),
                Point(dstW.toDouble() - 1.0, dstH.toDouble() - 1.0),
                Point(0.0, dstH.toDouble() - 1.0)
            )

            // 6) Get perspective transform, warp
            val M = Imgproc.getPerspectiveTransform(pageQuad, dst)
            val warped = Mat()
            Imgproc.warpPerspective(srcMat, warped, M, Size(dstW.toDouble(), dstH.toDouble()), Imgproc.INTER_LINEAR)

            if (debug) {
                debugMap["warped_color"] = matToBitmapSafe(warped)
                // also store a grayscale warped
                val warpedGray = Mat()
                Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_RGBA2GRAY)
                debugMap["warped_gray"] = matToBitmapSafe(warpedGray)
                warpedGray.release()
            }

            // Convert warped Mat -> Bitmap
            val warpedBmp = createBitmap(warped.cols(), warped.rows())
            Utils.matToBitmap(warped, warpedBmp)

            // release native mats
            blurred.release(); edges.release(); kernel.release()
            contours.forEach { it.release() }
            pageQuad.release()
            M.release()
            warped.release()
            gray.release()
            srcMat.release()

            return PreprocessResult(
                ok = true,
                reason = null,
                warpedBitmap = warpedBmp,
                transformMatrix = null,
                confidence = 0.95,
                intermediate = debugMap
            )
        } catch (ex: Exception) {
            // ensure Mats released on error
            try { gray.release() } catch (_: Exception) {}
            try { srcMat?.release() } catch (_: Exception) {}
            return PreprocessResult(
                ok = false,
                reason = ex.message ?: "Exception during page detection",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = debugMap
            )
        }
    }

    // ---------- small helpers ----------

    private fun distance(a: Point, b: Point): Double {
        return hypot(a.x - b.x, a.y - b.y)
    }

    // Order points to TL, TR, BR, BL
    private fun orderCorners(pts: Array<Point>): Array<Point> {
        if (pts.size != 4) return pts
        val sortedBySum = pts.sortedBy { it.x + it.y } // tl has smallest x+y, br largest
        val tl = sortedBySum.first()
        val br = sortedBySum.last()
        val others = pts.filter { it != tl && it != br }
        val (p1, p2) = Pair(others[0], others[1])
        val tr = if (p1.x > p2.x) p1 else p2
        val bl = if (p1.x > p2.x) p2 else p1
        return arrayOf(tl, tr, br, bl)
    }

    // Convert Mat -> Bitmap safely for debug (handles 1/3/4 channels)
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