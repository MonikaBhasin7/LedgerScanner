package com.example.ledgerscanner.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import java.io.IOException
import androidx.core.graphics.scale
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.max
import androidx.core.graphics.createBitmap

object ImageUtils {
//    fun loadCorrectlyOrientedBitmap(
//        context: Context,
//        uri: Uri,
//        reqWidth: Int = 1080,
//        reqHeight: Int = 1920
//    ): Bitmap? {
//        // 1) Decode bitmap from URI
//        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            val source = ImageDecoder.createSource(context.contentResolver, uri)
//            ImageDecoder.decodeBitmap(source)
//        } else {
//            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
//        } ?: return null
//
//        // 2) Read EXIF orientation (only works if URI has a file path)
//        val orientation = try {
//            val path = uri.path
//            if (path != null) {
//                val exif = ExifInterface(path)
//                exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_UNDEFINED
//                )
//            } else ExifInterface.ORIENTATION_UNDEFINED
//        } catch (e: IOException) {
//            ExifInterface.ORIENTATION_UNDEFINED
//        }
//
//        // 3) Rotate if needed
//        val rotation = when (orientation) {
//            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
//            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
//            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
//            else -> 0f
//        }
//        if (rotation != 0f) {
//            val matrix = Matrix().apply { postRotate(rotation) }
//            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//        }
//
//        return bitmap
//    }
//
//    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
//        val (height: Int, width: Int) = options.outHeight to options.outWidth
//        var inSampleSize = 1
//        if (height > reqHeight || width > reqWidth) {
//            val halfHeight = height / 2
//            val halfWidth = width / 2
//            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
//                inSampleSize *= 2
//            }
//        }
//        return inSampleSize
//    }


    @WorkerThread
    fun loadBitmapCorrectOrientation(
        context: Context,
        uri: Uri,
        reqWidth: Int = 1080,
        reqHeight: Int = 1920
    ): Bitmap? {
        try {
            // 1) Read EXIF orientation from the URI via InputStream (works with content://)
            val exifOrientation = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // On Android 7.0+ we can read EXIF directly from InputStream
                        ExifInterface(stream).getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED
                        )
                    } else {
                        // On older Android we need a file path, not stream
                        val path = uri.path
                        if (path != null) {
                            ExifInterface(path).getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED
                            )
                        } else {
                            ExifInterface.ORIENTATION_UNDEFINED
                        }
                    }
                } ?: ExifInterface.ORIENTATION_UNDEFINED
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_UNDEFINED
            }

            val rotationDegrees = when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // 2) Decode bitmap (sampled down to reqWidth/reqHeight to avoid OOM)
            val decoded: Bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // ImageDecoder automatically honors EXIF orientation (applies rotation)
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val origW = info.size.width
                        val origH = info.size.height
                        val scale =
                            maxOf(1f, origW.toFloat() / reqWidth, origH.toFloat() / reqHeight)
                        val targetW = (origW / scale).toInt().coerceAtLeast(1)
                        val targetH = (origH / scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    // Decode bounds first then sample
                    val input1 = context.contentResolver.openInputStream(uri) ?: return null
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(input1, null, opts)
                    input1.close()

                    val sample = calculateInSampleSize(opts, reqWidth, reqHeight)
                    val input2 = context.contentResolver.openInputStream(uri) ?: return null
                    val opts2 = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inJustDecodeBounds = false
                    }
                    val bmp = BitmapFactory.decodeStream(input2, null, opts2)
                    input2.close()
                    bmp
                }
            } catch (e: Exception) {
                try {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } catch (ex: Exception) {
                    null
                }
            } ?: return null


            /*
             Strategy summary:
             - On API>=28 ImageDecoder SHOULD already apply EXIF rotation.
               So we generally MUST NOT rotate after decode.
             - On older APIs we MUST rotate according to EXIF.
             - But in practice some camera/provider combos may NOT include EXIF or decoders might behave differently.
             - We'll apply a small heuristic: if API>=28 and EXIF says rotation != 0 but the decoded bitmap
               already looks rotated (dimensions swapped), we assume ImageDecoder handled it and skip manual rotate.
             */

            val needsManualRotate = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {
                    // ImageDecoder usually auto-applies rotation. If rotationDegrees != 0, check if decoded looks rotated
                    // If rotationDegrees is 90/270, then after rotation width/height swap. If decoder already rotated,
                    // decoded.width < decoded.height for portrait expectation. We'll rotate only if it still matches wrong orientation.
                    when (rotationDegrees) {
                        90, 270 -> {
                            // If decoded width > height and rotationDegrees says 90/270, decoder probably DID NOT rotate -> rotate manually.
                            decoded.width > decoded.height
                        }

                        180 -> {
                            // For 180, dimension stays same — rely on EXIF value here (rotate if rotationDegrees==180)
                            true
                        }

                        else -> false
                    }
                }

                else -> {
                    // older devices: ImageDecoder not available -> we must rotate according to EXIF
                    rotationDegrees != 0
                }
            }


            if (!needsManualRotate) {
                // decoded is fine as-is
                return decoded
            }

            // perform rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotated =
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            // recycle original if distinct to free memory
            if (rotated !== decoded) decoded.recycle()
            return rotated

        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun findDocumentContour(matGray: Mat): MatOfPoint2f? {
        val blurred = Mat()
        Imgproc.GaussianBlur(matGray, blurred, Size(5.0, 5.0), 0.0)

        val edged = Mat()
        Imgproc.Canny(blurred, edged, 50.0, 150.0) // tune thresholds

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            edged,
            contours,
            hierarchy,
            Imgproc.RETR_LIST,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        hierarchy.release(); edged.release(); blurred.release()

        // sort by area desc
        contours.sortByDescending { Imgproc.contourArea(it) }

        for (c in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*c.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*c.toArray()), approx, 0.02 * peri, true)
            if (approx.total() == 4L) {
                // found rectangle-like contour
                val res = MatOfPoint2f()
                approx.copyTo(res)
                // ensure points are ordered (tl,tr,br,bl)
                return sortPointsClockwise(res)
            }
        }
        return null
    }

    private fun sortPointsClockwise(src: MatOfPoint2f): MatOfPoint2f {
        val pts = src.toArray().toList()
        // Convert to list and compute centroid, sort by angle
        val cx = pts.map { it.x }.average()
        val cy = pts.map { it.y }.average()
        val sorted = pts.sortedBy { Math.atan2(it.y - cy, it.x - cx) }
        // ensure ordering starts top-left etc. rotate accordingly if needed
        return MatOfPoint2f(*sorted.toTypedArray())
    }

    fun warpToRectangle(
        srcMat: Mat,
        corners: MatOfPoint2f,
        targetWidth: Int = 1654,
        targetHeight: Int = 2339
    ): Mat {
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(targetWidth - 1.0, 0.0),
            Point(targetWidth - 1.0, targetHeight - 1.0),
            Point(0.0, targetHeight - 1.0)
        )
        val M = Imgproc.getPerspectiveTransform(corners, dst)
        val warped = Mat()
        Imgproc.warpPerspective(
            srcMat,
            warped,
            M,
            Size(targetWidth.toDouble(), targetHeight.toDouble())
        )
        return warped // caller should release M if stored
    }

    fun binarizeForBubbles(warpedGray: Mat): Mat {
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            warpedGray,
            thresh,
            255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            25,
            10.0
        )
        // morphological close to fill small holes in bubbles
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(3.0, 3.0))
        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, kernel)
        return thresh
    }
}

fun Bitmap.downscaleForDetection(maxLongSide: Int = 1200): Bitmap {
    val w = this.width
    val h = this.height
    val long = maxOf(w, h)
    if (long <= maxLongSide) return this
    val scale = maxLongSide.toFloat() / long.toFloat()
    val newW = (w * scale).toInt()
    val newH = (h * scale).toInt()
    return this.scale(newW, newH)
}

fun Bitmap.bitmapToGrayMat(): Mat {
    val mat = Mat()
    Utils.bitmapToMat(this, mat) // RGBA by default
    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY)
    return mat
}

fun Mat.denoise(): Mat {
    val out = Mat()
    // Good default: small gaussian then bilateral for preserving edges
    Imgproc.GaussianBlur(this, out, Size(3.0, 3.0), 0.0)
    // Optional: bilateral for better edge preservation (slower)
    // Imgproc.bilateralFilter(out, out, 9, 75.0, 75.0)
    return out
}

fun Mat.equalizeContrast(): Mat {
    val out = Mat()
    // CLAHE (good for uneven lighting)
    val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0)) // clipLimit 2.0, grid 8x8
    clahe.apply(this, out)
    return out
}

// brightness: mean intensity
fun Mat.computeBrightness(): Double {
    val meanScalar = Core.mean(this)
    return meanScalar.`val`[0] // 0..255
}

// sharpness: variance of Laplacian
fun Mat.computeSharpness(): Double {
    val lap = Mat()
    Imgproc.Laplacian(this, lap, CvType.CV_64F)
    val mean = MatOfDouble()
    val std = MatOfDouble()
    Core.meanStdDev(lap, mean, std)
    val variance = std.get(0, 0)[0] * std.get(0, 0)[0]
    lap.release(); mean.release(); std.release()
    return variance // higher = sharper
}

private const val TARGET_WIDTH = 800

/**
 * Convert to a scaled gray 2D array of luminance values (0..255).
 * We downscale so result is fast and more comparable across cameras.
 */
private fun bitmapToGrayArrayScaled(src: Bitmap): Pair<Array<IntArray>, IntArray> {
    val width = src.width
    val height = src.height

    // compute scale factor to target width (but keep aspect ratio)
    val scale = if (width > TARGET_WIDTH) TARGET_WIDTH.toFloat() / width.toFloat() else 1f
    val newW = max(3, (width * scale).toInt())
    val newH = max(3, (height * scale).toInt())

    val scaledRaw = src.scale(newW, newH)
    // ensure we have a software-backed ARGB_8888 bitmap to allow getPixels()
    val scaled = ensureSoftwareBitmap(scaledRaw)
    val pixels = IntArray(newW * newH)
    scaled.getPixels(pixels, 0, newW, 0, 0, newW, newH)

    val gray = Array(newH) { IntArray(newW) }
    var idx = 0
    for (y in 0 until newH) {
        for (x in 0 until newW) {
            val p = pixels[idx++]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // luminance — use standard Rec. 601 weights
            val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            gray[y][x] = lum
        }
    }
    // return gray 2D and dims in int array [w, h]
    return Pair(gray, intArrayOf(newW, newH))
}

/**
 * Ensure the bitmap is software-backed and uses ARGB_8888 so we can call getPixels / copy / Utils.bitmapToMat etc.
 * If the provided bitmap is hardware-backed (API >= O) or has an incompatible config, we create a new ARGB_8888 bitmap
 * and draw the source into it.
 */
private fun ensureSoftwareBitmap(bmp: Bitmap): Bitmap {
    // On API >= 26 there is Bitmap.Config.HARDWARE which doesn't allow pixel access.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val config = bmp.config
        // if not hardware and config exists
        if (config != Bitmap.Config.HARDWARE) {
            // If already ARGB_8888, return as-is
            if (config == Bitmap.Config.ARGB_8888) return bmp
            // Otherwise convert to ARGB_8888
            val out = createBitmap(bmp.width, bmp.height)
            val c = Canvas(out)
            c.drawBitmap(bmp, 0f, 0f, null)
            return out
        }
        // config == HARDWARE -> must convert
        val out = createBitmap(bmp.width, bmp.height)
        val c = Canvas(out)
        c.drawBitmap(bmp, 0f, 0f, null)
        return out
    } else {
        // API < 26: HARDWARE config does not exist.
        // But bmp.config may still be null or something not ARGB_8888 -> convert if needed.
        val config = bmp.config
        if (config == Bitmap.Config.ARGB_8888) {
            return bmp
        }
        // convert to ARGB_8888
        val out = createBitmap(bmp.width, bmp.height)
        val c = Canvas(out)
        c.drawBitmap(bmp, 0f, 0f, null)
        return out
    }
}

/**
 * Compute variance of Laplacian.
 *
 * Kernel:   [-1 -1 -1
 *            -1  8 -1
 *            -1 -1 -1]
 *
 * This is a simple 3x3 Laplacian (sensitive to edges). We compute the raw convolution output
 * (which can be negative), collect them, compute mean and variance.
 *
 * Returns variance as Double.
 */
fun Bitmap.laplacianVariance(): Double {
    val (gray, dims) = bitmapToGrayArrayScaled(this)
    val w = dims[0]
    val h = dims[1]

    // Laplacian kernel offsets
    val kernel = arrayOf(
        intArrayOf(-1, -1, -1),
        intArrayOf(-1, 8, -1),
        intArrayOf(-1, -1, -1)
    )

    val responses = DoubleArray((w - 2) * (h - 2))
    var rIdx = 0
    var sum = 0.0
    var sumSq = 0.0

    // compute responses for interior pixels (skip borders)
    for (y in 1 until h - 1) {
        for (x in 1 until w - 1) {
            var acc = 0
            // apply kernel
            for (ky in -1..1) {
                for (kx in -1..1) {
                    val v = gray[y + ky][x + kx]
                    acc += v * kernel[ky + 1][kx + 1]
                }
            }
            val valDouble = acc.toDouble()
            responses[rIdx++] = valDouble
            sum += valDouble
            sumSq += valDouble * valDouble
        }
    }

    val n = responses.size
    if (n == 0) return 0.0
    val mean = sum / n
    val variance = (sumSq / n) - (mean * mean)
    // variance could be tiny negative due to fp rounding; clamp
    return if (variance < 0.0) 0.0 else variance
}

/**
 * Quick boolean helper: returns true if image considered blurry.
 *
 * @param bitmap captured image
 * @param threshold recommended starting threshold: 100.0
 *        - If variance < threshold => blurry
 *        - Raise threshold if too many false-positives (blurry passing)
 *
 * Recommended: run on sample images and set threshold accordingly.
 * On mobile photos: typical thresholds often range ~80..200 (device-dependent).
 */
fun Bitmap.isImageBlurry(threshold: Double = 120.0): Boolean {
    val varLap = this.laplacianVariance()
    // you might want to log for calibration:
    // Log.d("BlurUtils", "lapVar=$varLap threshold=$threshold")
    return varLap < threshold
}

fun Mat.toBitmapSafe(): Bitmap {
    val tmp = Mat()
    when (this.channels()) {
        1 -> Imgproc.cvtColor(this, tmp, Imgproc.COLOR_GRAY2RGBA)
        3 -> Imgproc.cvtColor(this, tmp, Imgproc.COLOR_BGR2RGBA)
        4 -> this.copyTo(tmp)
        else -> this.copyTo(tmp)
    }
    val bmp = createBitmap(tmp.cols(), tmp.rows())
    Utils.matToBitmap(tmp, bmp)
    tmp.release()
    return bmp
}

/**
 * Pre-clean a grayscale Mat:
 * 1) optional CLAHE to flatten lighting
 * 2) light denoise (Gaussian or Bilateral)
 */
fun Mat.preCleanGray(
    useClahe: Boolean = true,
    useBilateral: Boolean = false
): Mat {
    require(this.type() == CvType.CV_8UC1) {
        "preCleanGray expects single-channel 8-bit grayscale."
    }

    val work = this.clone()

    // 1) Contrast-Limited Adaptive Histogram Equalization (good for shadows/uneven light)
    if (useClahe) {
        val clahe = org.opencv.imgproc.Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
        clahe.apply(work, work)
    }

    // 2) Denoise
    if (useBilateral) {
        // Bilateral keeps edges sharper (slower). Good when circles are thin.
        // d: neighborhood diameter, sigmaColor/sigmaSpace tune smoothness.
        Imgproc.bilateralFilter(
            work,
            work, /*d*/
            9, /*sigmaColor*/
            50.0, /*sigmaSpace*/
            50.0
        )
    } else {
        // Gaussian is fast + stable
        org.opencv.imgproc.Imgproc.GaussianBlur(work, work, Size(5.0, 5.0), 0.0)
    }

    return work
}