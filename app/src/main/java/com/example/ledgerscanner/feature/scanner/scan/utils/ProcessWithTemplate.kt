package com.example.ledgerscanner.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import com.example.ledgerscanner.feature.scanner.scan.model.Template
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.ORB
import org.opencv.imgproc.Imgproc
import org.opencv.calib3d.Calib3d
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object TemplateProcessor {
    private const val TAG = "TemplateProcessor"

    /**
     * Process an input photo using a template (no printed fiducials).
     *
     * Steps (high level):
     * 1) Convert inputBitmap and templateBitmap to Mats (gray).
     * 2) Use ORB feature-match to compute homography H (template -> photo).
     * 3) If H found, warp photo into template canonical size (template.sheet_width x sheet_height).
     * 4) For every option box in template.questions, take the ROI patch from warped image,
     *    compute a local Otsu threshold, create a circular mask, compute fill fraction
     *    and decide filled/not filled using threshold.
     * 5) Create overlay debug image with circles and returned intermediate Bitmaps.
     *
     * @param context not used here but left for future IO needs
     * @param inputBitmap the camera/photo bitmap
     * @param template the JSON-deserialized Template (sheet_width/height and question option boxes)
     * @param templateBitmap a Bitmap of the blank template (same canonical coordinates)
     * @param debug include debug bitmaps in PreprocessResult.intermediate
     */
    fun processWithTemplate(
        context: Context,
        inputBitmap: Bitmap,
        template: Template,
        templateBitmap: Bitmap,
        debug: Boolean = true
    ): PreprocessResult {
        val intermediate = mutableMapOf<String, Bitmap>()
        try {
            // 1) Convert template bitmap and input bitmap to Mats (BGR and Gray)
            val tplMatBgr = Mat()
            Utils.bitmapToMat(templateBitmap, tplMatBgr) // template expected BGR/RGBA from Utils
            val tplGray = Mat()
            Imgproc.cvtColor(tplMatBgr, tplGray, Imgproc.COLOR_RGBA2GRAY)

            val srcMatBgr = Mat()
            Utils.bitmapToMat(inputBitmap, srcMatBgr)
            val srcGray = Mat()
            Imgproc.cvtColor(srcMatBgr, srcGray, Imgproc.COLOR_RGBA2GRAY)

            // debug: template and source small preview (optional)
            if (debug) {
                intermediate["template_preview"] = matToBitmapSafe(tplGray)
                intermediate["input_preview"] = matToBitmapSafe(srcGray)
            }

            // 2) Compute homography H: template -> photo using ORB
            val H = computeHomographyFromTemplate(tplGray, srcGray)
            if (H == null) {
                // fallback: if homography failed, return helpful debug images
                tplMatBgr.release(); tplGray.release(); srcMatBgr.release(); srcGray.release()
                return PreprocessResult(
                    ok = false,
                    reason = "Homography (feature-match) failed",
                    warpedBitmap = null,
                    transformMatrix = null,
                    confidence = 0.0,
                    intermediate = intermediate
                )
            }

            // 3) Warp the photo to template canonical coordinates (size from template)
            val targetW = template.sheet_width
            val targetH = template.sheet_height
            val warped = Mat()
            Imgproc.warpPerspective(
                srcMatBgr,
                warped,
                H,
                Size(targetW.toDouble(), targetH.toDouble()),
                Imgproc.INTER_LINEAR
            )
            // Convert warped to gray for scoring
            val warpedGray = Mat()
            Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)

            if (debug) {
                intermediate["warped"] = matToBitmapSafe(warpedGray)
            }

            // 4) Prepare overlay (color) for debug drawing
            val overlay = Mat()
            Imgproc.cvtColor(warpedGray, overlay, Imgproc.COLOR_GRAY2RGBA)

            // 5) Iterate questions/options and score each option
            val results = mutableListOf<BubbleResult>()
            // radius estimate: if template bubble_diameter provided use it, else compute from box w/h (avg)
            for (q in template.questions) {
                val qNo = q.q_no
                val sortedOptions = q.options.sortedBy { it.x } // left -> right
                for (opt in sortedOptions) {
                    // ensure ROI inside warped bounds
                    val x0 = opt.x.coerceAtLeast(0)
                    val y0 = opt.y.coerceAtLeast(0)
                    val w = (opt.w).coerceAtLeast(4)
                    val h = (opt.h).coerceAtLeast(4)
                    val xClamped = min(x0, warpedGray.cols() - 1)
                    val yClamped = min(y0, warpedGray.rows() - 1)
                    val wClamped = if (xClamped + w > warpedGray.cols()) warpedGray.cols() - xClamped else w
                    val hClamped = if (yClamped + h > warpedGray.rows()) warpedGray.rows() - yClamped else h
                    if (wClamped <= 0 || hClamped <= 0) {
                        results.add(BubbleResult(qNo, opt.option[0], false, 0.0))
                        continue
                    }
                    val roiRect = Rect(xClamped, yClamped, wClamped, hClamped)
                    val patch = Mat(warpedGray, roiRect)

                    // compute radius approx (use template bubble diameter if given)
                    val radiusF = if (template.bubble_diameter != null && template.bubble_diameter > 0) {
                        (template.bubble_diameter / 2.0) * (wClamped.toDouble() / template.bubble_diameter.toDouble())
                    } else {
                        (min(wClamped, hClamped) / 2.0)
                    }
                    val radius = max(1, radiusF.roundToInt())

                    // compute fill score without global binarization:
                    // - apply local gaussian blur -> local Otsu threshold -> binary patch
                    // - create circular mask centered in patch with radius
                    // - compute filled fraction = white_count_inside_mask / circle_area
                    val score = computeBubbleFillScoreFromPatch(patch, radius)

                    // decide filled: threshold tuned (0.25..0.40). Use 0.30 as default
                    val filled = score >= 0.30

                    results.add(BubbleResult(qNo, opt.option[0], filled, score))

                    // draw overlay debug: circle and small filled dot proportional to score
                    val centerX = xClamped + (wClamped / 2.0)
                    val centerY = yClamped + (hClamped / 2.0)
                    val centerPoint = Point(centerX, centerY)
                    val color = if (filled) Scalar(0.0, 255.0, 0.0, 200.0) else Scalar(255.0, 0.0, 0.0, 160.0)
                    Imgproc.circle(overlay, centerPoint, radius, color, 2)
                    val innerR = (radius * score).roundToInt().coerceAtLeast(1)
                    Imgproc.circle(overlay, centerPoint, innerR, color, -1)

                    // release patch
                    patch.release()
                }
            }

            // 6) Convert overlay to bitmap for debugging and return
            if (debug) {
                intermediate["overlay"] = matToBitmapSafe(overlay)
            }

            // cleanup
            tplMatBgr.release(); tplGray.release()
            srcMatBgr.release(); srcGray.release()
            warped.release(); warpedGray.release()
            overlay.release()
            H.release()

            // return PreprocessResult: warped preview (use overlay as visualization)
            return PreprocessResult(
                ok = true,
                reason = null,
                warpedBitmap = intermediate["warped"], // raw warped gray preview
                transformMatrix = null,
                confidence = 0.9,
                intermediate = intermediate
            )
        } catch (ex: Exception) {
            Log.e(TAG, "processWithTemplate failed", ex)
            return PreprocessResult(
                ok = false,
                reason = ex.message ?: "error",
                warpedBitmap = null,
                transformMatrix = null,
                confidence = 0.0,
                intermediate = intermediate
            )
        }
    }

    // ------------------------------------------------------
    // Helper: compute homography using ORB + BF matcher + RANSAC
    // returns Mat 3x3 homography template->photo (so warpPerspective(photo, H_inv?) we'll warp photo by H)
    // ------------------------------------------------------
    private fun computeHomographyFromTemplate(templateGray: Mat, photoGray: Mat): Mat? {
        val orb = ORB.create(2500) // increase if template has many details
        val kps1 = MatOfKeyPoint()
        val desc1 = Mat()
        val kps2 = MatOfKeyPoint()
        val desc2 = Mat()
        orb.detectAndCompute(templateGray, Mat(), kps1, desc1)
        orb.detectAndCompute(photoGray, Mat(), kps2, desc2)

        if (desc1.empty() || desc2.empty()) {
            kps1.release(); kps2.release(); desc1.release(); desc2.release()
            return null
        }

        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
        // KNN matches
        val knnMatches = mutableListOf<MatOfDMatch>()
        matcher.knnMatch(desc1, desc2, knnMatches, 2)

        // ratio test
        val goodMatches = ArrayList<DMatch>()
        for (m in knnMatches) {
            val arr = m.toArray()
            if (arr.size >= 2) {
                val m1 = arr[0]
                val m2 = arr[1]
                if (m1.distance < 0.75 * m2.distance) {
                    goodMatches.add(m1)
                }
            } else if (arr.size == 1) {
                goodMatches.add(arr[0])
            }
            m.release()
        }

        if (goodMatches.size < 8) {
            // not enough matches
            kps1.release(); kps2.release(); desc1.release(); desc2.release()
            return null
        }

        // build point lists
        val ptsTpl = ArrayList<Point>()
        val ptsPhoto = ArrayList<Point>()
        val tplKps = kps1.toArray()
        val photoKps = kps2.toArray()
        for (m in goodMatches) {
            ptsTpl.add(tplKps[m.queryIdx].pt)
            ptsPhoto.add(photoKps[m.trainIdx].pt)
        }

        val srcPts = MatOfPoint2f(*ptsTpl.toTypedArray())
        val dstPts = MatOfPoint2f(*ptsPhoto.toTypedArray())
        // find homography with RANSAC
        val mask = Mat()
        val H = Calib3d.findHomography(srcPts, dstPts, Calib3d.RANSAC, 3.0, mask)

        // clean
        kps1.release(); kps2.release(); desc1.release(); desc2.release()
        srcPts.release(); dstPts.release()
        mask.release()

        return if (H.empty()) {
            H.release()
            null
        } else H
    }

    // compute fill score for a patch (single-channel Mat). Returns 0.0..1.0 fraction.
    // Approach:
    //  - apply small gaussian blur
    //  - compute Otsu threshold (local) to binarize (we want bubble as white → invert if needed)
    //  - create circular mask centered in patch with given radius
    //  - compute white_count_inside_mask / circle_area
    private fun computeBubbleFillScoreFromPatch(patchGray: Mat, radius: Int): Double {
        if (patchGray.empty()) return 0.0
        val blur = Mat()
        Imgproc.GaussianBlur(patchGray, blur, Size(3.0, 3.0), 0.0)

        val bin = Mat()
        // Otsu: returns threshold, binary with INV because typical filled area is darker -> we want filled=white
        val otsu = Imgproc.threshold(blur, bin, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)

        // create circular mask centered in patch
        val mask = Mat.zeros(bin.size(), CvType.CV_8U)
        val cx = (bin.cols() / 2.0).roundToInt()
        val cy = (bin.rows() / 2.0).roundToInt()
        Imgproc.circle(mask, Point(cx.toDouble(), cy.toDouble()), radius.coerceAtMost(max(cx, cy)), Scalar(255.0), -1)

        // count non-zero in masked region
        val masked = Mat()
        Core.bitwise_and(bin, bin, masked, mask)
        val whiteInside = Core.countNonZero(masked).toDouble()

        // circle area (in pixels). guard against 0
        val circleArea = Math.PI * radius * radius
        val normalized = if (circleArea > 0) (whiteInside / circleArea) else 0.0

        // release mats
        blur.release(); bin.release(); mask.release(); masked.release()
        // clamp 0..1
        return normalized.coerceIn(0.0, 1.0)
    }

    // convert Mat to Bitmap (ARGB_8888) safely
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

    data class BubbleResult(
        val question: Int,
        val option: Char,
        val filled: Boolean,
        val confidence: Double
    )
}