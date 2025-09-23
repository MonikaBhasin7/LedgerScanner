package com.example.ledgerscanner.feature.scanner.scan.utils

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

data class FiducialResult(val corners: List<Point>?, val debugBitmap: Bitmap?)

object FiducialDetector {

    /**
     * Detect 4 fiducial markers (black circular dots) in the given bitmap.
     * Returns corners in TL, TR, BR, BL order, or null if detection failed.
     * Also returns a debug bitmap overlay (optional).
     */
    fun detectFiducials(inputBmp: Bitmap, debug: Boolean = true): FiducialResult {
        val src = Mat()
        Utils.bitmapToMat(inputBmp, src) // RGBA or BGR depending on bitmap
        val gray = Mat()
        if (src.channels() == 4) Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        else if (src.channels() == 3) Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        else src.copyTo(gray)

        // 1) smooth small noise but keep marker shapes
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        // 2) threshold: black dots => white on bin (we invert later if needed)
        val bin = Mat()
        Imgproc.adaptiveThreshold(
            gray, bin,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            31,
            7.0
        )

        // 3) optional morphology to remove tiny specks
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_OPEN, kernel)

        // 4) find contours
        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(bin.clone(), contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // 5) filter by area + circularity, collect centers
        val imgArea = (bin.rows() * bin.cols()).toDouble()
        val minArea = (imgArea * 0.0002).coerceAtLeast(100.0) // tuneable
        val maxArea = (imgArea * 0.02).coerceAtLeast(500.0)   // tuneable

        val candidates = mutableListOf<Point>() // centers
        val candidateAreas = mutableListOf<Double>()
        val candidateRects = mutableListOf<Rect>()

        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area < minArea || area > maxArea) { c.release(); continue }
            val peri = Imgproc.arcLength(MatOfPoint2f(*c.toArray()), true)
            val circularity = if (peri > 1e-6) 4.0 * Math.PI * area / (peri * peri) else 0.0
            if (circularity < 0.4) { c.release(); continue } // require some roundness

            val r = Imgproc.boundingRect(c)
            val cx = r.x + r.width / 2.0
            val cy = r.y + r.height / 2.0
            candidates.add(Point(cx, cy))
            candidateAreas.add(area)
            candidateRects.add(r)
            c.release()
        }

        // 6) If too many candidates, merge close centers to reduce duplicates
        val merged = mergeClosePoints(candidates, mergeDist = (min(imgArea, 1.0) /*unused*/) )
        // mergeClosePoints will compute a proper dist threshold below

        // 7) If we still have more than 4, pick the 4 with largest area (approx)
        val finalCenters = if (merged.size > 4) {
            // compute approximate area by finding nearest original candidate area
            val withArea = merged.map { m ->
                var bestIdx = -1; var bestDist = Double.MAX_VALUE
                for ((i, orig) in candidates.withIndex()) {
                    val d = hypot(m.x - orig.x, m.y - orig.y)
                    if (d < bestDist) { bestDist = d; bestIdx = i }
                }
                Pair(m, if (bestIdx >= 0) candidateAreas[bestIdx] else 0.0)
            }
            withArea.sortedByDescending { it.second }.take(4).map { it.first }
        } else merged

        // 8) We need exactly 4 and they should be reasonably spread out
        if (finalCenters.size != 4) {
            // cleanup mats
            src.release(); gray.release(); bin.release(); kernel.release()
            return FiducialResult(null, if (debug) createDebugBitmap(src, bin, candidates) else null)
        }

        // 9) Order corners reliably (using centroid + angle)
        val ordered = orderPointsByAngle(finalCenters)

        // 10) Optional: sanity check - distinctness and near corners (close to edges)
        // (you can verify distance to edges to ensure they are near corners; warn if not)

        // build debug overlay
        val debugBmp = if (debug) {
            createOverlayBitmap(src, bin, ordered)
        } else null

        // release
        src.release(); gray.release(); bin.release(); kernel.release()
        return FiducialResult(ordered, debugBmp)
    }

    // Merge points closer than ~bubbleDiameter/2 or relative threshold.
    private fun mergeClosePoints(points: List<Point>, mergeDist: Double = -1.0): List<Point> {
        if (points.isEmpty()) return emptyList()
        // choose threshold based on image size distribution: median pairwise dist or fallback 40 pixels
        val dists = mutableListOf<Double>()
        for (i in 0 until points.size) for (j in i+1 until points.size) {
            dists.add(hypot(points[i].x - points[j].x, points[i].y - points[j].y))
        }
        val medianDist = if (dists.isEmpty()) 40.0 else dists.sorted()[dists.size / 2]
        val thresh = if (mergeDist > 0) mergeDist else (medianDist * 0.5).coerceAtLeast(16.0)

        val used = BooleanArray(points.size)
        val out = mutableListOf<Point>()
        for (i in points.indices) {
            if (used[i]) continue
            var sx = points[i].x
            var sy = points[i].y
            var count = 1
            used[i] = true
            for (j in i+1 until points.size) {
                if (used[j]) continue
                val d = hypot(points[i].x - points[j].x, points[i].y - points[j].y)
                if (d <= thresh) {
                    sx += points[j].x; sy += points[j].y; count++; used[j] = true
                }
            }
            out.add(Point(sx / count, sy / count))
        }
        return out
    }

    // Order four points TL, TR, BR, BL by centroid + angle
    private fun orderPointsByAngle(pts: List<Point>): List<Point> {
        // compute centroid
        val cx = pts.map { it.x }.average()
        val cy = pts.map { it.y }.average()
        // compute angle from centroid (-PI..PI), sort ascending
        val withAngle = pts.map { p ->
            val a = atan2(p.y - cy, p.x - cx) // angle where right=0, top negative
            Pair(p, a)
        }.sortedBy { it.second }

        // atan2 order is: -pi..pi, with -pi..-pi/2 left-bottom, -pi/2..0 top-left->top-right etc.
        // we need mapping to TL, TR, BR, BL. We'll convert angles to quadrant:
        // We'll sort by y then x more robustly:
        val byY = pts.sortedWith(compareBy({ it.y }, { it.x }))
        val top2 = byY.take(2).sortedBy { it.x }
        val bottom2 = byY.takeLast(2).sortedBy { it.x }
        return listOf(top2[0], top2[1], bottom2[1], bottom2[0]) // TL, TR, BR, BL
    }

    private fun createOverlayBitmap(src: Mat, bin: Mat, points: List<Point>): Bitmap {
        val overlay = Mat()
        if (src.channels() == 1) Imgproc.cvtColor(src, overlay, Imgproc.COLOR_GRAY2RGBA) else Imgproc.cvtColor(src, overlay, Imgproc.COLOR_BGR2RGBA)
        for ((i, p) in points.withIndex()) {
            Imgproc.circle(overlay, p, 10, Scalar(0.0, 255.0, 0.0, 255.0), 3)
            Imgproc.putText(overlay, "${i+1}", Point(p.x+12, p.y+6), Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255.0,0.0,0.0,255.0),2)
        }
        val bmp = createBitmap(overlay.cols(), overlay.rows())
        Utils.matToBitmap(overlay, bmp)
        overlay.release()
        return bmp
    }

    private fun createDebugBitmap(src: Mat, bin: Mat, centers: List<Point>): Bitmap {
        val rgba = Mat()
        if (src.channels() == 1) Imgproc.cvtColor(src, rgba, Imgproc.COLOR_GRAY2RGBA) else Imgproc.cvtColor(src, rgba, Imgproc.COLOR_BGR2RGBA)
        for (p in centers) Imgproc.circle(rgba, p, 6, Scalar(0.0, 0.0, 255.0), 2)
        val bmp = createBitmap(rgba.cols(), rgba.rows())
        Utils.matToBitmap(rgba, bmp)
        rgba.release()
        return bmp
    }
}