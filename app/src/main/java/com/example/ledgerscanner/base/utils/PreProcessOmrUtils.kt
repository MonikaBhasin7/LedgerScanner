package com.example.ledgerscanner.base.utils

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import com.example.ledgerscanner.base.utils.ImageUtils.binarizeForBubbles
import com.example.ledgerscanner.base.utils.ImageUtils.findDocumentContour
import com.example.ledgerscanner.base.utils.ImageUtils.warpToRectangle
import com.example.ledgerscanner.feature.scanner.scan.model.PreprocessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc

object PreProcessOmrUtils {

    /**
     * High-level preprocessing function you call after capture.
     *
     * Steps performed:
     *  1. Convert input Bitmap -> grayscale Mat
     *  2. Denoise / blur to reduce small speckle noise
     *  3. Adaptive threshold to get binary sheet for contour detection
     *  4. Find document contour (largest quadrilateral)
     *  5. Perspective transform (warp) to get top-down sheet
     *  6. Enhance contrast (CLAHE) and return a cleaned Bitmap
     *
     * Returns a Bitmap of the processed, top-down OMR sheet.
     *
     * IMPORTANT: this should run on a background thread. This function is a suspend function
     * and internally switches to Dispatchers.Default for CPU work.
     */
    suspend fun preprocessFile(originalBitmap: Bitmap): PreprocessResult =
        withContext(Dispatchers.Default) {
            // defensive: ensure OpenCV native library loaded (you should do this once on app startup)
            try {
                // ----- 1) Pre-check: rotation / orientation -----
                // Ensure originalBitmap is correctly rotated already. If not, fix EXIF rotation BEFORE calling this function.

                // ----- 2) Downscale for quick checks -----
                val down = originalBitmap.downscaleForDetection() // returns Bitmap
                val isBlurry = down.isImageBlurry()
                if (isBlurry) {
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Image is blurred",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.0,
                        intermediate = mapOf()
                    )
                }

                // ----- 3) Convert downscaled bitmap to Mat (grayscale) -----
                // I expect bitmapToGrayMat() returns a Mat (single-channel CV_8UC1).
                val grayDown: Mat = down.bitmapToGrayMat() // ensure this returns CV_8UC1

                // ----- 4) Denoise + equalize -----
                val den = grayDown.denoise()           // Mat CV_8UC1
                val eq = den.equalizeContrast()        // Mat CV_8UC1

                // ----- 5) Quick quality checks -----
                val brightness = eq.computeBrightness() // Double or Int (0..255)
                val sharpness = eq.computeSharpness()   // Double
                if (brightness < 45) {
                    // release mats
                    grayDown.release(); den.release(); eq.release()
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Too dark",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.0,
                        intermediate = mapOf()
                    )
                }

                // ----- 6) Find document contour in downscaled image -----
                val corners =
                    findDocumentContour(eq) // should return MatOfPoint2f or null (points in downscale coords)
                if (corners == null) {
                    grayDown.release(); den.release(); eq.release()
                    return@withContext PreprocessResult(
                        ok = false,
                        reason = "Document corners not found",
                        warpedBitmap = null,
                        transformMatrix = null,
                        confidence = 0.2,
                        intermediate = mapOf("preview" to down) // optionally return downscaled preview
                    )
                }

                // ----- 7) Convert original Bitmap -> Mat (use full resolution for warp) -----
                val origMat = Mat()
                // Utils.bitmapToMat converts ARGB_8888 Bitmap into CV_8UC4 (RGBA) by default
                Utils.bitmapToMat(originalBitmap, origMat)

                // Convert RGBA -> GRAY for any checks (if needed)
                val srcGrayOrig = Mat()
                Imgproc.cvtColor(origMat, srcGrayOrig, Imgproc.COLOR_RGBA2GRAY)

                // ----- 8) Map corners from downscaled coords back to original scale -----
                val scaleX = originalBitmap.width.toDouble() / down.width.toDouble()
                val scaleY = originalBitmap.height.toDouble() / down.height.toDouble()

                // corners: MatOfPoint2f in downscale coordinates. Convert to array of Point in original coords.
                val downPoints = corners.toArray() // Array<Point> (points in downscale)
                // ensure corners length == 4
                if (downPoints.size < 4) {
                    // cleanup
                    grayDown.release(); den.release(); eq.release()
                    origMat.release(); srcGrayOrig.release()
                    corners.release()
                    return@withContext PreprocessResult(false, "Insufficient corner points")
                }
                // map to original
                val origPoints =
                    downPoints.map { p -> Point(p.x * scaleX, p.y * scaleY) }.toTypedArray()
                val cornersOrig = MatOfPoint2f(*origPoints)

                // ----- 9) Warp to rectangle (target DPI/size) -----
                // Ensure warpToRectangle expects source MatOfPoint2f in order [tl, tr, br, bl].
                val targetW = 1654 // e.g., A4 at 150 dpi ~ 1654x2339
                val targetH = 2339
                val warped = warpToRectangle(
                    origMat,
                    cornersOrig,
                    targetWidth = targetW,
                    targetHeight = targetH
                )
                // warpToRectangle should return a Mat in BGR or RGBA depending on implementation.
                // I assume returned Mat is BGR (common in OpenCV), convert to RGBA for bitmap.

                val warpedRgba = Mat()
                Imgproc.cvtColor(warped, warpedRgba, Imgproc.COLOR_BGR2RGBA)

                // create bitmap correctly
                val warpedBmp = createBitmap(warpedRgba.cols(), warpedRgba.rows())
                Utils.matToBitmap(warpedRgba, warpedBmp)

                // ----- 10) Prepare binary image for bubble detection -----
                val warpedGray = Mat()
                Imgproc.cvtColor(warped, warpedGray, Imgproc.COLOR_BGR2GRAY)
                val binary =
                    binarizeForBubbles(warpedGray) // returns single-channel Mat CV_8UC1 (0/255)
                val binaryRgba = Mat()
                Imgproc.cvtColor(binary, binaryRgba, Imgproc.COLOR_GRAY2RGBA)
                val binaryBmp = createBitmap(binaryRgba.cols(), binaryRgba.rows())
                Utils.matToBitmap(binaryRgba, binaryBmp)

                // optional: compute a numeric confidence using corner quality / sharpness etc
//                val confidence = computeConfidenceFrom(corners, sharpness, brightness)

                // ----- 11) release mats to free native memory -----
                grayDown.release()
                den.release()
                eq.release()
                corners.release()
                origMat.release()
                srcGrayOrig.release()
                cornersOrig.release()
                warped.release()
                warpedRgba.release()
                warpedGray.release()
                binary.release()
                binaryRgba.release()

                // ----- 12) return result -----
                PreprocessResult(
                    ok = true,
                    reason = null,
                    warpedBitmap = warpedBmp,
                    transformMatrix = null, // optional: return perspective matrix if you need it
                    confidence = 0.9,
                    intermediate = mapOf("binary" to binaryBmp)
                )
            } catch (e: Exception) {
                return@withContext PreprocessResult(false, reason = e.message ?: "Unknown error")
            }
        }
}