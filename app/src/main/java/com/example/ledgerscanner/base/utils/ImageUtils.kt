package com.example.ledgerscanner.base.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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

object ImageUtils {
    fun loadCorrectlyOrientedBitmap(
        path: String,
        reqWidth: Int = 1080,
        reqHeight: Int = 1920
    ): Bitmap? {
        // 1) read EXIF orientation
        val orientation = try {
            val exif = ExifInterface(path)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        } catch (e: IOException) {
            ExifInterface.ORIENTATION_UNDEFINED
        }

        // 2) decode with sampling to avoid OOM
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        val bmp = BitmapFactory.decodeFile(path, options) ?: return null

        // 3) rotate if needed
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bmp

        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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