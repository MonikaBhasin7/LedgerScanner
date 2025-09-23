package com.example.ledgerscanner.feature.scanner.scan.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.feature.scanner.scan.model.BubbleResult
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
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
import kotlin.math.min

object TemplateOmrProcessor {
    fun processWithTemplate(
        context: Context,
        inputBitmap: Bitmap,
        template: Template,
        debug: Boolean = true
    ): PreprocessResult {
        val intermediates = mutableMapOf<String, Bitmap>()

        // convert input bitmap to BGR Mat (OpenCV standard)
        val srcBgr = Mat()
        try {
            Utils.bitmapToMat(inputBitmap, srcBgr)
        } catch (e: Exception) {
            srcBgr.release()
            return PreprocessResult(
                false,
                "Bitmap->Mat conversion failed: ${e.message}",
                intermediate = intermediates
            )
        }

        try {
            // 1) prepare gray copy for corner-finding
            val gray = Mat()
            if (srcBgr.channels() == 4) Imgproc.cvtColor(srcBgr, gray, Imgproc.COLOR_RGBA2GRAY)
            else if (srcBgr.channels() == 3) Imgproc.cvtColor(srcBgr, gray, Imgproc.COLOR_BGR2GRAY)
            else srcBgr.copyTo(gray)
            if (debug) intermediates["input_gray"] = matToBitmapSafe(gray)

            // 2) try to find the 4-corner sheet quad from the photo (largest 4-vertex contour)
            val sheetQuad = findLargestQuad(gray)
            if (sheetQuad == null) {
                gray.release()
                srcBgr.release()
                return PreprocessResult(
                    false,
                    "Sheet corners not found",
                    intermediate = intermediates
                )
            }

            // 3) create destination corners from template canonical size
            val dstCorners = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(template.sheet_width.toDouble(), 0.0),
                Point(template.sheet_width.toDouble(), template.sheet_height.toDouble()),
                Point(0.0, template.sheet_height.toDouble())
            )

            // srcCorners from detection (order them to TL,TR,BR,BL)
            val orderedSrc = orderCornersForHomography(sheetQuad.toArray())
            val srcCorners = MatOfPoint2f(*orderedSrc)

            // 4) compute perspective transform and warp the original *color* image to template space
            val H = Imgproc.getPerspectiveTransform(srcCorners, dstCorners)
            val warped = Mat()
            Imgproc.warpPerspective(
                srcBgr,
                warped,
                H,
                Size(template.sheet_width.toDouble(), template.sheet_height.toDouble())
            )
            if (debug) intermediates["warped_color"] = matToBitmapSafe(warped)

            val warpedGray = Mat()
            Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
            if (debug) intermediates["warped_gray"] = matToBitmapSafe(warpedGray)

            // 6) For each option rectangle in the template, compute a fill score
            val answers = mutableListOf<BubbleResult>()
            val optionChars = listOf(
                'A',
                'B',
                'C',
                'D'
            ) // fallback if template not labeled; use template.option if provided

            // If template stores per-question options, iterate over them
            template.questions.forEach { q ->
                q.options.forEach { opt ->
                    val x0 = opt.x.coerceAtLeast(0)
                    val y0 = opt.y.coerceAtLeast(0)
                    val w = opt.w.coerceAtLeast(1)
                    val h = opt.h.coerceAtLeast(1)
                    val x1 = min(warpedGray.cols(), x0 + w)
                    val y1 = min(warpedGray.rows(), y0 + h)

                    if (x0 >= x1 || y0 >= y1) {
                        answers.add(BubbleResult(q.q_no, opt.option, false, 0.0))
                        return@forEach
                    }
                    val roi = Rect(x0, y0, x1 - x0, y1 - y0)
                    val patch = Mat(warpedGray, roi)

                    // compute filledness WITHOUT doing global binarization:
                    // - compute mean intensity inside small central circle (bubble interior)
                    // - compute mean intensity of ring/outside area for contrast
                    // - use relative darkness as score
                    val (filled, confidence) = computeBubbleFillScore(patch)

                    answers.add(BubbleResult(q.q_no, opt.option, filled, confidence))

                    if (debug) {
                        // draw a small debug patch preview per first question only (avoid too many bitmaps)
                        val key = "patch_q${q.q_no}_opt${opt.option}"
                        if (!intermediates.containsKey(key)) {
                            intermediates[key] = matToBitmapSafe(patch)
                        }
                    }

                    patch.release()
                }
            }

            // cleanup mats
            gray.release()
            srcCorners.release()
            dstCorners.release()
            H.release()
            warpedGray.release()

            // if you want warped color to preview as warpedBitmap in PreprocessResult:
            val warpedBitmap = if (debug) {
                intermediates["warped_color"]
            } else {
                null
            }

            // return PreprocessResult with intermediate bitmaps
            return PreprocessResult(
                ok = true,
                reason = null,
                warpedBitmap = warpedBitmap,
                transformMatrix = null,
                confidence = 0.9,
                intermediate = intermediates
            )
        } catch (e: Exception) {
            // release in case of exception
            try {
                srcBgr.release()
            } catch (ignored: Exception) {
            }
            return PreprocessResult(
                false,
                "Processing failed: ${e.message}",
                intermediate = intermediates
            )
        } finally {
            // ensure srcBgr released if still valid
            if (!srcBgr.empty()) srcBgr.release()
        }
    }

    fun matToBitmapSafe(mat: Mat): Bitmap {
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

    /**
     * Find the largest quadrilateral contour in the image and return it as MatOfPoint2f.
     * Layman: finds the biggest 4-corner shape (paper) in the photo by edge detection and contour approximation.
     */
    private fun findLargestQuad(gray: Mat): MatOfPoint2f? {
        val edged = Mat()
        Imgproc.Canny(gray, edged, 50.0, 150.0)
        // dilate a little so the contour is continuous
        Imgproc.dilate(
            edged,
            edged,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        )

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edged,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        hierarchy.release()
        edged.release()

        if (contours.isEmpty()) {
            contours.forEach { it.release() }
            return null
        }

        // sort desc by area and attempt to approximate a quad
        contours.sortByDescending { Imgproc.contourArea(it) }
        for (c in contours) {
            val area = Imgproc.contourArea(c)
            if (area < 1000) {
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
                return approx // caller will order these points
            }
            approx.release()
        }
        // no quad found
        contours.forEach { if (!it.empty()) it.release() }
        return null
    }

    /**
     * Order 4 points to TL, TR, BR, BL.
     * Layman: takes 4 scattered corner points and returns them in a consistent order,
     * which is required when computing a perspective transform.
     */
    private fun orderCornersForHomography(pts: Array<Point>): Array<Point> {
        // tl has minimal x+y, br has maximal x+y
        val tl = pts.minByOrNull { it.x + it.y }!!
        val br = pts.maxByOrNull { it.x + it.y }!!
        val rest = pts.filter { it != tl && it != br }
        val (p1, p2) = Pair(rest[0], rest[1])
        // top-right is the one with smaller y
        val tr: Point
        val bl: Point
        if (p1.x > p2.x) {
            tr = p1; bl = p2
        } else {
            tr = p2; bl = p1
        }
        return arrayOf(tl, tr, br, bl)
    }

    /**
     * Compute fill score for a bubble patch WITHOUT doing a global binary:
     *
     * Strategy (simple & robust in many lighting conditions):
     *  - We expect each patch to contain one circular bubble (ring or filled).
     *  - Compute central circle mask (radius = 40% min(width,height)/2).
     *  - Compute mean intensity inside the circle (meanInside) and mean in outer ring (meanOutside).
     *  - Filled bubble -> inside is darker (smaller mean) than outside -> score = (meanOutside - meanInside)/255
     *  - Also compute % of dark pixels inside using an adaptive local threshold (block size tuned to patch size).
     *  - Combine both signals to get final confidence 0..1 and boolean filled if confidence > threshold.
     *
     * Layman: look inside the circle. If the center is considerably darker than its surroundings and contains many dark pixels,
     * it's probably filled.
     */
    private fun computeBubbleFillScore(patchGray: Mat): Pair<Boolean, Double> {
        // defensive: small patch check
        val pw = patchGray.cols().coerceAtLeast(1)
        val ph = patchGray.rows().coerceAtLeast(1)
        val minDim = min(pw, ph)
        val radius = (minDim * 0.4).toInt().coerceAtLeast(2) // central circle radius
        val cx = pw / 2.0
        val cy = ph / 2.0

        // create circular mask for interior
        val maskInside = Mat.zeros(patchGray.size(), CvType.CV_8U)
        Imgproc.circle(maskInside, Point(cx, cy), radius, Scalar(255.0), -1)

        // compute means
        val meanInside = Core.mean(patchGray, maskInside).`val`[0]

        // outside ring mask: full patch minus inner circle
        val fullMask = Mat.ones(patchGray.size(), CvType.CV_8U)
        Core.multiply(fullMask, Scalar(255.0), fullMask)
        val maskOutside = Mat()
        Core.bitwise_not(maskInside, maskOutside)
        // mean outside (apply maskOutside)
        val meanOutside = Core.mean(patchGray, maskOutside).`val`[0]

        // local adaptive threshold inside patch to count dark pixels
        val local = Mat()
        // pick blockSize odd and proportional to patch
        var blockSize = (minDim / 3) // e.g., for 30px bubble -> blockSize = 10
        if (blockSize % 2 == 0) blockSize += 1
        if (blockSize < 3) blockSize = 3

        try {
            Imgproc.adaptiveThreshold(
                patchGray,
                local,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                blockSize,
                5.0
            )
        } catch (e: Exception) {
            // fallback to OTSU if adaptive fails
            Imgproc.threshold(
                patchGray,
                local,
                0.0,
                255.0,
                Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU
            )
        }

        // count dark pixels inside the circle
        val masked = Mat()
        Core.bitwise_and(local, maskInside, masked)
        val darkInside = Core.countNonZero(masked).toDouble()
        val insideArea = Core.countNonZero(maskInside).toDouble().coerceAtLeast(1.0)
        val darkFraction = (darkInside / insideArea).coerceIn(0.0, 1.0) // 0..1

        // signal from mean difference (normalize)
        val meanDiff =
            (meanOutside - meanInside) / 255.0 // higher positive means inside darker than outside

        // combine signals (weights tuned empirically)
        val w1 = 0.6 // weight for darkFraction
        val w2 = 0.4 // weight for meanDiff
        val score = (w1 * darkFraction + w2 * (meanDiff.coerceIn(-1.0, 1.0))).coerceIn(0.0, 1.0)

        // decide filled (tunable threshold)
        val filled = score >= 0.35 // start with 0.35, tune per your data

        // release mats
        maskInside.release(); fullMask.release(); maskOutside.release(); local.release(); masked.release()

        // return boolean and confidence score 0..1
        return Pair(filled, score)
    }

}