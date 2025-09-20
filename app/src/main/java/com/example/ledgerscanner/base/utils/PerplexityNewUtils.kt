package com.example.ledgerscanner.base.utils

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
//            val bubbleRegion = cropBubbleArea(warped)
//            debug["bubble_region"] = matToBitmapSafe(bubbleRegion)

            // 5. binarize for bubble detection
            val binary = binarizeForBubbles(warped)
            debug["binary"] = matToBitmapSafe(binary)

            // 6. find bubble contours
            val bubbleContours = findBubbleContours(binary)
            debug["bubbles_contours"] = drawContoursOn(binary, bubbleContours)

            // 7. extract bubble answers
            val rows = groupContoursToRowsAndColumns(bubbleContours, expectedOptionsPerRow = 4)

            // extract answers
            val answers = extractAnswersFromRows(rows, binary, expectedOptionsPerRow = 4)

            // cleanup mats we created (release native memory)
            gray.release()
            if (sheetQuad != null) sheetQuad.release()
            warped.release()
//            bubbleRegion.release()
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
        Imgproc.dilate(
            edges,
            edges,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        )

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
        val pts = quad.toArray()
        val widthTop = Math.hypot(pts[1].x - pts[0].x, pts[1].y - pts[0].y)
        val widthBottom = Math.hypot(pts[2].x - pts[3].x, pts[2].y - pts[3].y)
        val maxWidth = max(widthTop, widthBottom).toInt()

        val heightLeft = Math.hypot(pts[3].x - pts[0].x, pts[3].y - pts[0].y)
        val heightRight = Math.hypot(pts[2].x - pts[1].x, pts[2].y - pts[1].y)
        val maxHeight = max(heightLeft, heightRight).toInt()

        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )

        val M = Imgproc.getPerspectiveTransform(quad, dst)
        val warped = Mat()
        Imgproc.warpPerspective(srcGray, warped, M, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        return warped
    }

    // Replace the old cropBubbleArea() with this function
    private fun cropBubbleArea(warped: Mat): Mat {
        // warped: single-channel grayscale Mat (CV_8U)
        // 1) Make a clean binary image that highlights bubbles
        val bin = Mat()
        // adaptive threshold with inverted output (bubbles -> white)
        Imgproc.adaptiveThreshold(
            warped,
            bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            15,
            8.0
        )

        // small close to join broken bubble strokes (tune kernel size if needed)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 3.0))
        Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_CLOSE, kernel)

        val rows = bin.rows()
        val cols = bin.cols()

        // 2) Row-wise projection (count of white pixels per row)
        val rowSums = DoubleArray(rows)
        for (r in 0 until rows) {
            // countNonZero works on a row view
            rowSums[r] = Core.countNonZero(bin.row(r)).toDouble()
        }

        // 3) Column-wise projection (count of white pixels per column)
        val colSums = DoubleArray(cols)
        for (c in 0 until cols) {
            colSums[c] = Core.countNonZero(bin.col(c)).toDouble()
        }

        // 4) Thresholds: relative to peak projection so it's adaptive
        val maxRow = rowSums.maxOrNull() ?: 0.0
        val maxCol = colSums.maxOrNull() ?: 0.0

        // small fractions — tune if you miss rows/columns
        val rowThresh = maxRow * 0.05      // keep rows with >= 5% of max row activity
        val colThresh = maxCol * 0.02      // keep columns with >= 2% of max column activity

        // find first/last row above threshold
        val top = rowSums.indexOfFirst { it > rowThresh }.let { if (it == -1) 0 else it }
        val bottom = rowSums.indexOfLast { it > rowThresh }.let { if (it == -1) rows - 1 else it }

        // find first/last column above threshold
        val left = colSums.indexOfFirst { it > colThresh }.let { if (it == -1) 0 else it }
        val right = colSums.indexOfLast { it > colThresh }.let { if (it == -1) cols - 1 else it }

        // 5) If detection failed (no region), fallback to whole warped image
        if (top >= bottom || left >= right) {
            kernel.release()
            val result = warped.clone()
            bin.release()
            return result
        }

        // 6) Add small padding (2% of region size) and clamp to image bounds
        val padY = ((bottom - top) * 0.02).toInt().coerceAtLeast(6)
        val padX = ((right - left) * 0.02).toInt().coerceAtLeast(6)

        val r0 = (top - padY).coerceAtLeast(0)
        val r1 = (bottom + padY + 1).coerceAtMost(rows) // +1 because submat end is exclusive
        val c0 = (left - padX).coerceAtLeast(0)
        val c1 = (right + padX + 1).coerceAtMost(cols)

        // 7) Return a clone of submat so caller can safely release original mats
        val cropped = warped.submat(r0, r1, c0, c1).clone()

        // cleanup
        kernel.release()
        bin.release()

        return cropped
    }

    // Adaptive threshold then a small open to remove speckles and small morphology tweaks
    fun binarizeForBubbles(gray: Mat): Mat {
        val bin = Mat()
        Imgproc.adaptiveThreshold(
            gray,
            bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_MEAN_C,
            Imgproc.THRESH_BINARY_INV,
            21,
            2.0
        )
        return bin
    }

    fun findBubbleContours(bin: Mat): List<MatOfPoint> {
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(bin, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        // Filter contours: circularity, area, aspect ratio
        return contours.filter {
            val area = Imgproc.contourArea(it)
            val rect = Imgproc.boundingRect(it)
            val aspect = rect.width.toDouble() / rect.height
            val circularity = 4 * Math.PI * area / (Imgproc.arcLength(MatOfPoint2f(*it.toArray()), true)).pow(2)
            area > 100 && area < 1500 && aspect > 0.8 && aspect < 1.2 && circularity > 0.5
        }
    }

    fun groupContoursToRowsAndColumns(
        contours: List<MatOfPoint>,
        expectedOptionsPerRow: Int = 4
    ): List<List<MatOfPoint>> {
        if (contours.isEmpty()) return emptyList()

        // get centers and sizes
        data class CInfo(
            val contour: MatOfPoint,
            val cx: Double,
            val cy: Double,
            val w: Int,
            val h: Int
        )

        val infos = contours.map {
            val r = Imgproc.boundingRect(it)
            CInfo(it, r.x + r.width / 2.0, r.y + r.height / 2.0, r.width, r.height)
        }

        // median height/width
        val heights = infos.map { it.h }.sorted()
        val widths = infos.map { it.w }.sorted()
        val medianH = heights[heights.size / 2].toDouble().coerceAtLeast(1.0)
        val medianW = widths[widths.size / 2].toDouble().coerceAtLeast(1.0)

        val rowTolerance = max((medianH * 0.6), 8.0) // min tolerance 8px
        val remaining = infos.toMutableList()
        val rows = mutableListOf<List<MatOfPoint>>()

        // cluster by Y into rows
        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val rowGroup = mutableListOf<CInfo>()
            rowGroup.add(seed)

            // find others close in Y
            val iterator = remaining.iterator()
            while (iterator.hasNext()) {
                val r = iterator.next()
                if (abs(r.cy - seed.cy) <= rowTolerance) {
                    rowGroup.add(r)
                    iterator.remove()
                }
            }
            // sort rowGroup by X (left -> right)
            rowGroup.sortBy { it.cx }
            rows.add(rowGroup.map { it.contour })
        }

        // Now we have rows, but each row might contain > expectedOptionsPerRow * columns.
        // If some rows have many more contours (e.g., noise), we can try to collapse close-by contours horizontally into bins
        val normalizedRows = rows.map { rowContours ->
            // if length roughly equals expected -> good
            if (rowContours.size == expectedOptionsPerRow) return@map rowContours

            // else attempt to cluster by X into `expectedOptionsPerRow` groups
            val xs =
                rowContours.map { Imgproc.boundingRect(it).x + Imgproc.boundingRect(it).width / 2.0 }
            // perform simple greedy binning by nearest neighbor to evenly distribute into expectedOptionsPerRow bins
            val zipped = rowContours.map {
                Pair(
                    it,
                    Imgproc.boundingRect(it).x + Imgproc.boundingRect(it).width / 2.0
                )
            }.toMutableList()
            zipped.sortBy { it.second }
            // if too many items, try to merge adjacent ones whose centers are very close (within medianW * 0.6)
            val merged = mutableListOf<MatOfPoint>()
            var i = 0
            while (i < zipped.size) {
                var j = i + 1
                var accum = Imgproc.boundingRect(zipped[i].first)
                var mergedContour = zipped[i].first
                while (j < zipped.size) {
                    val next = Imgproc.boundingRect(zipped[j].first)
                    if (abs(next.x + next.width / 2.0 - accum.x - accum.width / 2.0) < medianW * 0.6) {
                        // merge by taking larger rect as representative (keep both contours — but for grouping we choose representative)
                        // We'll just pick the one whose area is larger to represent the merged bubble area
                        val areaCurrent = Imgproc.contourArea(mergedContour)
                        val areaNext = Imgproc.contourArea(zipped[j].first)
                        if (areaNext > areaCurrent) mergedContour = zipped[j].first
                        accum = Rect(
                            min(accum.x, next.x),
                            min(accum.y, next.y),
                            max(accum.x + accum.width, next.x + next.width) - min(accum.x, next.x),
                            max(accum.y + accum.height, next.y + next.height) - min(accum.y, next.y)
                        )
                        j++
                    } else break
                }
                merged.add(mergedContour)
                i = j
            }
            // if merged size still too large, just take nearest `expectedOptionsPerRow` by splitting into equal bins and picking median in each bin
            if (merged.size > expectedOptionsPerRow) {
                val bins = expectedOptionsPerRow
                val perBin = merged.size / bins
                val selected = mutableListOf<MatOfPoint>()
                var idx = 0
                for (b in 0 until bins) {
                    val start = b * perBin
                    val end = if (b == bins - 1) merged.size - 1 else (start + perBin - 1)
                    val sub = merged.subList(start, end + 1)
                    // pick the median element
                    selected.add(sub[sub.size / 2])
                    idx++
                }
                return@map selected
            }

            // otherwise good
            merged
        }

        return normalizedRows
    }

    fun extractAnswersFromRows(
        rows: List<List<MatOfPoint>>,
        bin: Mat,
        expectedOptionsPerRow: Int = 4
    ): List<BubbleResult> {
        val results = mutableListOf<BubbleResult>()
        var questionIndex = 1
        val optionChars = listOf('A', 'B', 'C', 'D')

        for (row in rows) {
            // if row is empty skip
            if (row.isEmpty()) continue

            // sort row by X
            val sortedRow = row.sortedBy { Imgproc.boundingRect(it).x }

            // If row count not exactly equal expectedOptionsPerRow, try to pick nearest expectedOptionsPerRow
            val chosen = if (sortedRow.size == expectedOptionsPerRow) sortedRow else {
                // if larger, pick equally spaced entries
                if (sortedRow.size > expectedOptionsPerRow) {
                    val step = sortedRow.size.toDouble() / expectedOptionsPerRow
                    (0 until expectedOptionsPerRow).map {
                        sortedRow[(it * step).toInt().coerceAtMost(sortedRow.size - 1)]
                    }
                } else {
                    // if fewer bubbles found, keep as-is and pad with nulls (we mark as BLANK later)
                    sortedRow
                }
            }

            // evaluate each bubble in chosen list
            for (i in 0 until expectedOptionsPerRow) {
                val contour = if (i < chosen.size) chosen[i] else null
                if (contour == null) {
                    results.add(
                        BubbleResult(
                            questionIndex,
                            optionChars.getOrElse(i) { '?' },
                            false,
                            0.0
                        )
                    )
                    continue
                }
                val rect = Imgproc.boundingRect(contour)
                // create mask
                val mask = Mat.zeros(bin.size(), CvType.CV_8U)
                Imgproc.drawContours(mask, listOf(contour), -1, Scalar(255.0), -1)

                val bubbleMasked = Mat()
                Core.bitwise_and(bin, bin, bubbleMasked, mask)

                val nonZero = Core.countNonZero(bubbleMasked).toDouble()
                val area = max(1.0, (rect.width * rect.height).toDouble())
                val fillPercent = nonZero / area

                // threshold for deciding filled - tune 0.25..0.45
                val filled = fillPercent > 0.30
                val confidence = fillPercent.coerceIn(0.0, 1.0)

                results.add(
                    BubbleResult(
                        questionIndex,
                        optionChars.getOrElse(i) { '?' },
                        filled,
                        confidence
                    )
                )

                mask.release()
                bubbleMasked.release()
            }

            questionIndex++
        }

        return results
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
        if (src.channels() == 1) Imgproc.cvtColor(
            src,
            rgba,
            Imgproc.COLOR_GRAY2RGBA
        ) else src.copyTo(rgba)
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

data class BubbleResult(
    val question: Int,
    val option: Char,
    val filled: Boolean,
    val confidence: Double
)