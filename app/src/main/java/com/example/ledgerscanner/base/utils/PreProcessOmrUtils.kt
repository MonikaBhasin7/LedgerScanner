package com.example.ledgerscanner.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.util.Log
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max


suspend fun debugPreprocessFile(
    originalBitmap: Bitmap,
    context: Context,
    debug: Boolean = false
): PreprocessResult = withContext(Dispatchers.Default) {
    try {
        // 1) downscale for detection (quick checks)
        val down = downscaleForDetection(originalBitmap)

        // 2) quick blur check (simple Laplacian variance or similar)
        if (isImageBlurryLaplacian(down)) {
            return@withContext PreprocessResult(ok = false, reason = "Image is blurred")
        }

        // 3) convert downscaled bitmap to gray Mat
        val grayDown = Mat()
        Utils.bitmapToMat(down, grayDown)
        Imgproc.cvtColor(grayDown, grayDown, Imgproc.COLOR_RGBA2GRAY)

        // debug: save grayDown if requested
        val intermediateMap = mutableMapOf<String, Bitmap>()
        if (debug) intermediateMap["down_gray"] = matToBitmapSafe(grayDown)

        // 4) denoise and equalize (helps contour detection)
        val den = Mat()
        Imgproc.GaussianBlur(grayDown, den, Size(5.0, 5.0), 0.0)
        val eq = Mat()
        Imgproc.equalizeHist(den, eq)

        if (debug) intermediateMap["den_eq"] = matToBitmapSafe(eq)

        // 5) find a large quadrilateral — prefers the largest 4-point polygon
        val cornersDown = findDocumentContour(eq)

        if (cornersDown == null) {
            // clean mats
            grayDown.release(); den.release(); eq.release()
            return@withContext PreprocessResult(
                ok = false,
                reason = "Document corners not found",
                confidence = 0.2,
                intermediate = intermediateMap
            )
        }

        // 6) order corners and compute area ratio on downscaled image
        val ptsDown = cornersDown.toArray()
        val orderedDown = orderCornersClockwise(ptsDown)

        if (debug) {
            val overlay = drawPolygonOnBitmap(down, orderedDown)
            intermediateMap["down_polygon"] = overlay
        }

        val areaDown = polygonArea(orderedDown)
        val downArea = down.width.toDouble() * down.height.toDouble()
        val areaRatio = areaDown / downArea
        // reject if polygon is too small (likely a bubble)
        if (areaRatio < 0.08) { // tune threshold as needed (8% here)
            grayDown.release(); den.release(); eq.release(); cornersDown.release()
            return@withContext PreprocessResult(
                ok = false,
                reason = "Detected polygon too small (likely not document)",
                confidence = 0.15,
                intermediate = intermediateMap
            )
        }

        // 7) convert original to Mat (use full resolution for warp)
        val origMat = Mat()
        Utils.bitmapToMat(originalBitmap, origMat) // RGBA format typical
        val origW = originalBitmap.width
        val origH = originalBitmap.height

        // 8) map downscale corners back to original coordinates
        val scaleX = origW.toDouble() / down.width.toDouble()
        val scaleY = origH.toDouble() / down.height.toDouble()
        val mappedOrigPoints =
            orderedDown.map { p -> Point(p.x * scaleX, p.y * scaleY) }.toTypedArray()
        val cornersOrig = MatOfPoint2f(*mappedOrigPoints)

        // debug overlay on original
        if (debug) {
            val overlayOrig = drawPolygonOnBitmap(originalBitmap, mappedOrigPoints)
            intermediateMap["orig_polygon"] = overlayOrig
        }

        // 9) validate corners are reasonable on original
        if (!validateCorners(cornersOrig, origW, origH)) {
            // cleanup
            grayDown.release(); den.release(); eq.release(); cornersDown.release()
            origMat.release(); cornersOrig.release()
            return@withContext PreprocessResult(
                ok = false,
                reason = "Mapped polygon invalid on original",
                intermediate = intermediateMap
            )
        }

        // 10) warp to rectangle (target size)
        val targetW = 1654 // example: A4-ish at some DPI; tune for your use-case
        val targetH = 2339
        val warped = warpToRectangle(origMat, cornersOrig, targetW, targetH)

        // convert warped (BGR or RGBA) to RGBA bitmap properly
        val warpedRgba = Mat()
        // attempt to convert sensibly:
        if (warped.channels() == 1) {
            Imgproc.cvtColor(warped, warpedRgba, Imgproc.COLOR_GRAY2RGBA)
        } else if (warped.channels() == 3) {
            Imgproc.cvtColor(warped, warpedRgba, Imgproc.COLOR_BGR2RGBA)
        } else {
            // assume already RGBA
            warpedRgba.assignTo(warped)
        }

        val warpedBmp = matToBitmapSafe(warpedRgba)

        if (debug) intermediateMap["warped_preview"] = warpedBmp

        // 11) produce binary for bubbles (adaptive threshold / morphological)
        val warpedGray = Mat()
        Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            warpedGray,
            binary,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            25,
            12.0
        )
        if (debug) intermediateMap["warped_binary"] = matToBitmapSafe(binary)

        // 12) cleanup mats
        grayDown.release(); den.release(); eq.release(); cornersDown.release()
        origMat.release(); cornersOrig.release()
        warped.release(); warpedRgba.release(); warpedGray.release()
        // note: binary remains to bitmap then released below if needed
        binary.release()

        // 13) return success
        PreprocessResult(
            ok = true,
            reason = null,
            warpedBitmap = warpedBmp,
            confidence = 0.9,
            intermediate = intermediateMap
        )
    } catch (ex: Exception) {
        Log.e("Preprocess", "Exception in preprocess", ex)
        PreprocessResult(ok = false, reason = ex.message)
    }
}

// ------------------------------
// Helper functions
// ------------------------------

/** Downscale input to a smaller bitmap used for quick checks and contour detection.
 *  Keeps aspect ratio, target maximum dimension around 1000 px (tweakable).
 *  Purpose: faster contour detection and less noise from very large images.
 */
fun downscaleForDetection(src: Bitmap, maxDim: Int = 1000): Bitmap {
    val w = src.width
    val h = src.height
    val scale = if (max(w, h) > maxDim) maxDim.toFloat() / max(w, h) else 1f
    val newW = max(1, (w * scale).toInt())
    val newH = max(1, (h * scale).toInt())
    return Bitmap.createScaledBitmap(src, newW, newH, true)
}

/** Simple blur check using Laplacian variance: returns true when image is blurry.
 *  Lower variance => more blur. Threshold tuned empirically; change as needed.
 */
fun isImageBlurryLaplacian(bmp: Bitmap, thresh: Double = 80.0): Boolean {
    val mat = Mat()
    Utils.bitmapToMat(bmp, mat)
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
    val lap = Mat()
    Imgproc.Laplacian(mat, lap, CvType.CV_64F)
    val mean = MatOfDouble()
    val stddev = MatOfDouble()
    Core.meanStdDev(lap, mean, stddev)
    val varr = stddev.get(0, 0)[0] * stddev.get(0, 0)[0]
    lap.release()
    mat.release()
    return varr < thresh
}

fun findDocumentContour(srcGray: Mat): MatOfPoint2f? {
    // defensive: expect single-channel
    if (srcGray.empty() || srcGray.channels() != 1) return null

    // 1) Threshold to extract dark shapes (black markers)
    val bin = Mat()
    // THRESH_BINARY_INV brings black -> white so markers become white on black background
    Imgproc.threshold(srcGray, bin, 50.0, 255.0, Imgproc.THRESH_BINARY_INV)

    // 2) Morphology to close small holes and remove noise
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))
    Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_CLOSE, kernel)
    Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_OPEN, kernel)

    // 3) Find contours
    val contours = ArrayList<MatOfPoint>()
    val hierarchy = Mat()
    Imgproc.findContours(bin, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
    hierarchy.release()

    if (contours.isEmpty()) {
        bin.release()
        return null
    }

    // 4) Filter contours to keep only rectangular-ish filled blobs likely to be fiducials
    data class Candidate(val contour: MatOfPoint, val area: Double, val rect: Rect, val center: Point)

    val candidates = ArrayList<Candidate>()
    for (c in contours) {
        val area = Imgproc.contourArea(c)
        if (area < 100.0) { // skip tiny contours — tune threshold as needed
            c.release(); continue
        }

        val rect = Imgproc.boundingRect(c)
        // keep shapes that aren't too elongated (square-ish)
        val aspect = rect.width.toDouble() / max(1.0, rect.height.toDouble())
        if (aspect < 0.4 || aspect > 2.5) { // allow rectangles too (markers might be rectangular)
            c.release(); continue
        }

        // approximate polygon to reduce noise
        val peri = Imgproc.arcLength(MatOfPoint2f(*c.toArray()), true)
        val approx = MatOfPoint2f()
        Imgproc.approxPolyDP(MatOfPoint2f(*c.toArray()), approx, 0.02 * peri, true)
        // Accept shapes with at least 4 vertices (filled rectangle will have ~4 vertices)
        if (approx.total() < 4) {
            approx.release(); c.release(); continue
        }
        approx.release()

        val cx = rect.x + rect.width / 2.0
        val cy = rect.y + rect.height / 2.0
        candidates.add(Candidate(c, area, rect, Point(cx, cy)))
    }

    // release bin
    bin.release()

    if (candidates.size < 4) {
        // cleanup
        for (cand in candidates) cand.contour.release()
        return null
    }

    // 5) Sort candidates by area descending (prefer larger markers), then keep top N to choose best 4
    candidates.sortByDescending { it.area }
    val top = candidates.take(8) // take few best; sometimes noise present; tune as needed

    // 6) From the selected, pick 4 that best represent the four corners:
    // We'll cluster by position: look for extremes in x & y to find TL, TR, BR, BL
    // Compute convex hull of centers then reduce to 4 by extremities if needed.
    val centers = top.map { it.center }.toMutableList()

    // If we have more than 4 take the outermost ones (by combination of x+y, x-y)
    val bySum = centers.sortedBy { it.x + it.y }   // TL will be smallest sum
    val byDiff = centers.sortedBy { it.x - it.y }  // TR will be largest difference, etc.

    // Try a robust selection: choose TL (min x+y), BR (max x+y), TR (min y - x negative?) simpler:
    val tl = centers.minByOrNull { it.x + it.y }!!
    val br = centers.maxByOrNull { it.x + it.y }!!
    val tr = centers.minByOrNull { -it.x + it.y }!! // heuristic
    val bl = centers.minByOrNull { it.x - it.y }!!  // heuristic

    // If heuristics produced duplicates (possible), fallback to geometric selection:
    val chosen = mutableSetOf<Point>(tl, tr, br, bl)
    if (chosen.size < 4) {
        // fallback: take four extreme points: left-most, right-most, top-most, bottom-most
        val leftMost = centers.minByOrNull { it.x }!!
        val rightMost = centers.maxByOrNull { it.x }!!
        val topMost = centers.minByOrNull { it.y }!!
        val bottomMost = centers.maxByOrNull { it.y }!!
        chosen.clear()
        chosen.add(leftMost); chosen.add(rightMost); chosen.add(topMost); chosen.add(bottomMost)
        // If still not 4 unique, just pick first 4 centers
        if (chosen.size < 4) {
            chosen.clear()
            chosen.addAll(centers.take(4))
        }
    }

    // Now we have 4 points (unordered). Order them TL, TR, BR, BL
    val pts = chosen.toList()
    val ordered = orderPointsClockwise(pts)

    // cleanup contours memory
    for (cand in candidates) cand.contour.release()

    // Return as MatOfPoint2f
    val out = MatOfPoint2f()
    out.fromArray(ordered[0], ordered[1], ordered[2], ordered[3])
    return out
}

/**
 * Given a list of 4 (or more) points, find 4 key points and order them:
 * returns [tl, tr, br, bl]
 *
 * Strategy (common robust approach):
 *  - sum = x + y -> TL is min sum, BR is max sum
 *  - diff = x - y -> TR is min diff? (or depends on axes) -> we compute robustly below.
 */
private fun orderPointsClockwise(points: List<Point>): Array<Point> {
    // If not exactly 4, take the four with extreme properties
    val pts = if (points.size == 4) points else points.take(4)

    // compute sums and diffs
    val sums = pts.map { it.x + it.y }
    val diffs = pts.map { it.x - it.y }

    val tl = pts[sums.indexOf(sums.minOrNull()!!)]
    val br = pts[sums.indexOf(sums.maxOrNull()!!)]
    val tr = pts[diffs.indexOf(diffs.maxOrNull()!!)]  // right-top will maximize x - y
    val bl = pts[diffs.indexOf(diffs.minOrNull()!!)]

    // final safety: if any duplicates, fallback to sort by y then x
    val set = linkedSetOf(tl, tr, br, bl)
    if (set.size < 4) {
        val sorted = pts.sortedWith(compareBy({ it.y }, { it.x })) // top->bottom, left->right
        // pick top-left, top-right, bottom-right, bottom-left from sorted groups
        val topTwo = sorted.take(2).sortedBy { it.x }
        val bottomTwo = sorted.takeLast(2).sortedBy { it.x }
        return arrayOf(topTwo[0], topTwo[1], bottomTwo[1], bottomTwo[0])
    }
    return arrayOf(tl, tr, br, bl)
}
/**
 * Given MatOfPoint2f with 4 points in arbitrary order, returns MatOfPoint2f ordered as:
 * [top-left, top-right, bottom-right, bottom-left]
 */
fun orderPointsClockwise(src: MatOfPoint2f): MatOfPoint2f {
    val pts = src.toArray()
    if (pts.size != 4) return src

    // compute sum and diff as simple heuristics
    val sum = pts.map { it.x + it.y }
    val diff = pts.map { it.y - it.x }

    val tl = pts[sum.indexOf(sum.minOrNull()!!)]
    val br = pts[sum.indexOf(sum.maxOrNull()!!)]
    val tr = pts[diff.indexOf(diff.minOrNull()!!)]
    val bl = pts[diff.indexOf(diff.maxOrNull()!!)]

    return MatOfPoint2f(tl, tr, br, bl)
}
/**
 * Validate that the 4 corners are within image bounds and have a sizable area.
 * Returns true when corners look OK for warping.
 */
fun validateCorners(corners: MatOfPoint2f, imageWidth: Int, imageHeight: Int): Boolean {
    val pts = corners.toArray()
    // 1) all points inside image bounds (with small margin)
    if (pts.any { p -> p.x < -20 || p.y < -20 || p.x > imageWidth + 20 || p.y > imageHeight + 20 }) return false
    // 2) area check
    val area = polygonArea(pts)
    val imageArea = imageWidth.toDouble() * imageHeight.toDouble()
    val ratio = area / imageArea
    // require polygon to be at least 8% of full image area
    return ratio > 0.08
}

/**
 * Order an array of 4 points in clockwise order starting from top-left.
 * Uses the classic sum/diff method:
 *  - top-left has smallest (x+y)
 *  - bottom-right has largest (x+y)
 *  - top-right has smallest (x-y)
 *  - bottom-left has largest (x-y)
 */
fun orderCornersClockwise(points: Array<Point>): Array<Point> {
    if (points.size != 4) return points
    val sorted = points.sortedWith(compareBy({ it.y }, { it.x })).toTypedArray() // not enough alone
    // we'll use sum/diff:
    val sumSorted = points.sortedBy { it.x + it.y }
    val diffSorted = points.sortedBy { it.x - it.y }
    val tl = sumSorted.first()
    val br = sumSorted.last()
    val tr = diffSorted.first()
    val bl = diffSorted.last()
    return arrayOf(tl, tr, br, bl)
}

/** Compute polygon area (works for simple polygons). */
fun polygonArea(points: Array<Point>): Double {
    var sum = 0.0
    for (i in points.indices) {
        val j = (i + 1) % points.size
        sum += (points[i].x * points[j].y) - (points[j].x * points[i].y)
    }
    return abs(sum) / 2.0
}

/**
 * Warp the `src` Mat to a rectangle using the source corners (MatOfPoint2f)
 * The destination rectangle is width x height.
 *
 * srcCorners must be ordered TL, TR, BR, BL.
 */
fun warpToRectangle(src: Mat, srcCorners: MatOfPoint2f, targetWidth: Int, targetHeight: Int): Mat {
    // Destination points in order TL, TR, BR, BL
    val dst = MatOfPoint2f(
        Point(0.0, 0.0),
        Point(targetWidth.toDouble(), 0.0),
        Point(targetWidth.toDouble(), targetHeight.toDouble()),
        Point(0.0, targetHeight.toDouble())
    )
    // compute perspective transform
    val M = Imgproc.getPerspectiveTransform(srcCorners, dst)
    val out = Mat()
    Imgproc.warpPerspective(
        src,
        out,
        M,
        Size(targetWidth.toDouble(), targetHeight.toDouble()),
        Imgproc.INTER_LINEAR
    )
    M.release()
    dst.release()
    return out
}

/** Convert Mat -> Bitmap safely for debug UI. */
fun matToBitmapSafe(mat: Mat): Bitmap {
    // attempt to convert to RGBA Bitmap
    val rgba = Mat()
    when (mat.channels()) {
        1 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_GRAY2RGBA)
        3 -> Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_BGR2RGBA)
        4 -> mat.copyTo(rgba)
        else -> mat.copyTo(rgba)
    }
    val bmp = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(rgba, bmp)
    rgba.release()
    return bmp
}

/** Draw polygon (points) on a copy of the bitmap and return it.
 *  Useful to debug which polygon was detected.
 */
fun drawPolygonOnBitmap(src: Bitmap, points: Array<Point>): Bitmap {
    val copy = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(copy)
    val paint = Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    val path = Path()
    path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
    for (i in 1 until points.size) path.lineTo(points[i].x.toFloat(), points[i].y.toFloat())
    path.close()
    canvas.drawPath(path, paint)
    val dot = Paint().apply {
        color = android.graphics.Color.GREEN
        style = Paint.Style.FILL
    }
    for (p in points) canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 8f, dot)
    return copy
}

/** Save a bitmap to the app cache directory for debug. Returns file path or null. */
fun saveBitmapForDebug(context: Context, name: String, bmp: Bitmap): String? {
    return try {
        val dir = File(context.cacheDir, "preprocess_debug")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "$name-${System.currentTimeMillis()}.png")
        val fos = FileOutputStream(f)
        bmp.compress(Bitmap.CompressFormat.PNG, 90, fos)
        fos.flush()
        fos.close()
        f.absolutePath
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}