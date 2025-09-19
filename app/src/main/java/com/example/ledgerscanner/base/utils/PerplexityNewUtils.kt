package com.example.ledgerscanner.base.utils

import android.graphics.Bitmap
import android.util.Log
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.lang.Exception
import androidx.core.graphics.createBitmap

/**
 * Simple, readable Perplexity-style OMR helper.
 *
 * Pipeline:
 * 1) convert to gray + blur
 * 2) find largest 4-point contour (sheet)
 * 3) warp (deskew) to fixed rectangle
 * 4) crop expected bubbles region
 * 5) adaptive threshold -> binary
 * 6) find bubble contours and decide filled / not
 *
 * Also collects intermediate Bitmaps in a map for visual debugging.
 *
 * NOTE: Call System.loadLibrary("opencv_java4") before using this.
 */
object PerplexityNewUtils {

    private const val TAG = "PerplexityUtilsSimple"

    // ---------- Public entrypoint ----------
    fun processOMR(bitmap: Bitmap, sheetId: String): PreprocessResult {
        val debug = mutableMapOf<String, Bitmap>()
        try {
            // 1. gray + blur
            val gray = bitmapToGrayMat(bitmap)
            debug["gray"] = matToBitmapSafe(gray)

            // 2. find sheet contour (largest quad)
            val sheetQuad = findSheetContour(gray)
            // draw contour debug
            debug["contours"] = drawContourDebug(gray, sheetQuad)

            // 3. warp to rectangle (if found) else use gray
            val warped = if (sheetQuad != null) warpToA4(gray, sheetQuad) else gray.clone()
            debug["warped_full"] = matToBitmapSafe(warped)

            // 4. crop bubble region (heuristic fraction)
            val bubbleRegion = cropBubbleArea(warped)
            debug["bubble_region"] = matToBitmapSafe(bubbleRegion)

            // 5. binarize for bubble detection
            val binary = binarizeForBubbles(bubbleRegion)
            debug["binary"] = matToBitmapSafe(binary)

            // 6. find bubble contours
            val bubbleContours = findBubbleContours(binary)
            debug["bubbles_contours"] = drawContoursOn(binary, bubbleContours)

            // 7. extract bubble answers
            val results = extractBubbleAnswers(binary, bubbleContours)

            // cleanup mats we created (release native memory)
            gray.release()
            if (sheetQuad != null) sheetQuad.release()
            warped.release()
            bubbleRegion.release()
            binary.release()
            bubbleContours.forEach { it.release() }

            // return final result (warped preview + debug map)
            return PreprocessResult(
                ok = true,
                reason = null,
                warpedBitmap = debug["warped_full"],
                transformMatrix = null,
                confidence = 0.9,
                intermediate = debug
            )
        } catch (e: Exception) {
            Log.e(TAG, "processOMR error", e)
            return PreprocessResult(
                ok = false,
                reason = e.message ?: "Error",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = debug
            )
        }
    }

    // ---------------- Helper functions (simple, commented) ----------------

    // Convert Android Bitmap to single-channel gray Mat and blur a bit
    private fun bitmapToGrayMat(bmp: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bmp, mat) // usually gives RGBA
        val gray = Mat()
        if (mat.channels() == 4) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
        } else if (mat.channels() == 3) {
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)
        } else {
            mat.copyTo(gray)
        }
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        mat.release()
        return gray
    }

    // Find the largest contour and approximate it to a 4-point polygon.
    // Return MatOfPoint2f with 4 points (TL, TR, BR, BL) or null if not found.
    private fun findSheetContour(gray: Mat): MatOfPoint2f? {
        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        // Make edges thicker so contour is continuous
        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(edges, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        edges.release()

        if (contours.isEmpty()) {
            contours.forEach { it.release() }
            return null
        }

        // sort by area descending, try to find a polygon with 4 vertices
        contours.sortByDescending { Imgproc.contourArea(it) }
        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area < 1000) { // skip tiny
                c.release()
                continue
            }
            val c2f = MatOfPoint2f(*c.toArray())
            val peri = Imgproc.arcLength(c2f, true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)
            c2f.release()
            c.release()
            if (approx.total() == 4L) {
                // order and return
                val ordered = orderCorners(approx.toArray())
                approx.release()
                return MatOfPoint2f(*ordered)
            }
            approx.release()
        }

        // fallback: no quad found
        contours.forEach { if (!it.empty()) it.release() }
        return null
    }

    // Warp the found quad to a fixed rectangle (A4-like). Returns single-channel Mat
    private fun warpToA4(srcGray: Mat, quad: MatOfPoint2f): Mat {
        val targetW = 1654
        val targetH = 2339
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(targetW.toDouble(), 0.0),
            Point(targetW.toDouble(), targetH.toDouble()),
            Point(0.0, targetH.toDouble())
        )
        val M = Imgproc.getPerspectiveTransform(quad, dst)
        val warped = Mat()
        Imgproc.warpPerspective(srcGray, warped, M, Size(targetW.toDouble(), targetH.toDouble()))
        M.release()
        dst.release()
        return warped
    }

    // Crop the region where bubbles typically live (heuristic fraction of page).
    private fun cropBubbleArea(warped: Mat): Mat {
        val r0 = (warped.rows() * 0.10).toInt()
        val r1 = (warped.rows() * 0.90).toInt()
        val c0 = (warped.cols() * 0.05).toInt()
        val c1 = (warped.cols() * 0.85).toInt()
        return warped.submat(r0, r1, c0, c1).clone() // clone so parent can be released
    }

    // Adaptive threshold then a small open to remove speckles and small morphology tweaks
    private fun binarizeForBubbles(regionGray: Mat): Mat {
        val bin = Mat()
        Imgproc.adaptiveThreshold(
            regionGray,
            bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            2.0
        )
        val ker = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_OPEN, ker)
        return bin
    }

    // Find contours in binary image and filter by area/aspect/circularity to keep bubble-like shapes.
    private fun findBubbleContours(bin: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(bin, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        if (contours.isEmpty()) return emptyList()

        val imgArea = bin.rows() * bin.cols()
        val minA = imgArea * 0.00005 // tune
        val maxA = imgArea * 0.01

        val keep = mutableListOf<MatOfPoint>()
        for (c in contours) {
            val area = Imgproc.contourArea(c)
            val rect = Imgproc.boundingRect(c)
            val aspect = if (rect.height != 0) rect.width.toDouble() / rect.height else 0.0
            val peri = Imgproc.arcLength(MatOfPoint2f(*c.toArray()), true)
            val circ = if (peri > 1e-6) 4 * Math.PI * area / (peri * peri) else 0.0
            if (area in minA..maxA && aspect in 0.6..1.4 && circ > 0.35) {
                keep.add(c)
            } else {
                c.release()
            }
        }
        return keep
    }

    // Very simple extraction: group contours by y (rows), sort each row by x, then compute fill percent.
    private fun extractBubbleAnswers(binImg: Mat, contours: List<MatOfPoint>): List<Triple<Int, Char, Double>> {
        // result entries: (questionNumber, optionChar, fillConfidence)
        if (contours.isEmpty()) return emptyList()

        // Sort contours by Y (top->bottom)
        val sorted = contours.sortedBy { Imgproc.boundingRect(it).y }
        // Group by approximate row (use rect.y / approx rowHeight)
        val rows = sorted.groupBy { Math.round(Imgproc.boundingRect(it).y / 40.0).toInt() }

        val out = mutableListOf<Triple<Int, Char, Double>>()
        var qIndex = 1
        val options = listOf('A', 'B', 'C', 'D')
        for ((_, group) in rows) {
            val rowSorted = group.sortedBy { Imgproc.boundingRect(it).x }
            rowSorted.forEachIndexed { idx, c ->
                // mask the bubble and count white pixels inside (binary is inverted -> filled=white)
                val mask = Mat.zeros(binImg.size(), CvType.CV_8U)
                Imgproc.drawContours(mask, listOf(c), -1, Scalar(255.0), -1)
                val tmp = Mat()
                Core.bitwise_and(binImg, binImg, tmp, mask)
                val nonZero = Core.countNonZero(tmp).toDouble()
                val rect = Imgproc.boundingRect(c)
                val area = (rect.width * rect.height).toDouble().coerceAtLeast(1.0)
                val fill = nonZero / area
                out.add(Triple(qIndex, options.getOrElse(idx) { '?' }, fill))
                mask.release(); tmp.release()
            }
            qIndex++
        }
        return out
    }

    // ---------- Small utilities for debug/bitmap conversions ----------

    // Return a Bitmap for a Mat (convert if needed). Always returns ARGB_8888 Bitmap.
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

    // Draw a single contour (if it exists) on top of gray for debug
    private fun drawContourDebug(gray: Mat, quad: MatOfPoint2f?): Bitmap {
        val rgba = Mat()
        Imgproc.cvtColor(gray, rgba, Imgproc.COLOR_GRAY2RGBA)
        quad?.let {
            val poly = MatOfPoint(*it.toArray())
            Imgproc.drawContours(rgba, listOf(poly), -1, Scalar(0.0, 255.0, 0.0, 255.0), 4)
            poly.release()
        }
        val b = matToBitmapSafe(rgba)
        rgba.release()
        return b
    }

    private fun drawContoursOn(src: Mat, contours: List<MatOfPoint>): Bitmap {
        val rgba = Mat()
        if (src.channels() == 1) Imgproc.cvtColor(src, rgba, Imgproc.COLOR_GRAY2RGBA) else src.copyTo(rgba)
        Imgproc.drawContours(rgba, contours, -1, Scalar(255.0, 0.0, 0.0, 255.0), 2)
        val b = matToBitmapSafe(rgba)
        rgba.release()
        return b
    }

    // Order 4 corners in TL, TR, BR, BL order (simple and robust)
    private fun orderCorners(pts: Array<Point>): Array<Point> {
        val tl = pts.minByOrNull { it.x + it.y }!!
        val br = pts.maxByOrNull { it.x + it.y }!!
        val rest = pts.filter { it != tl && it != br }
        val (p1, p2) = if (rest.size >= 2) Pair(rest[0], rest[1]) else Pair(pts[0], pts[1])
        val (tr, bl) = if (p1.x > p2.x) Pair(p1, p2) else Pair(p2, p1)
        return arrayOf(tl, tr, br, bl)
    }
}