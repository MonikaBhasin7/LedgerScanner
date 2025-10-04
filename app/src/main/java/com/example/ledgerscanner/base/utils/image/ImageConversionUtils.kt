package com.example.ledgerscanner.base.utils.image

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect

object ImageConversionUtils {

     fun imageProxyToGrayMatUpright(image: ImageProxy): Mat {
        val yPlane = image.planes[0]
        val w = image.width
        val h = image.height
        val rowStride = yPlane.rowStride
        val buffer = yPlane.buffer

        val mat = Mat(h, w, CvType.CV_8UC1)
        if (rowStride == w) {
            val arr = ByteArray(buffer.remaining())
            buffer.get(arr)
            mat.put(0, 0, arr)
        } else {
            val row = ByteArray(rowStride)
            for (r in 0 until h) {
                buffer.position(r * rowStride)
                buffer.get(row, 0, rowStride)
                mat.put(r, 0, row, 0, w)
            }
        }

        // Rotate based on imageInfo.rotationDegrees
        val rotated = Mat()
        when (image.imageInfo.rotationDegrees) {
            90 -> Core.rotate(mat, rotated, Core.ROTATE_90_CLOCKWISE)
            180 -> Core.rotate(mat, rotated, Core.ROTATE_180)
            270 -> Core.rotate(mat, rotated, Core.ROTATE_90_COUNTERCLOCKWISE)
            else -> mat.copyTo(rotated)
        }
        mat.release()
        return rotated
    }

    fun screenRectToMatRect(
        screenRect: RectF,
        previewRect: RectF,
        mat: Mat
    ): Rect {
        // 1) Normalize the overlay rect into the previewRect (0..1 in both axes)
        val u0 = ((screenRect.left - previewRect.left) / previewRect.width()).coerceIn(0f, 1f)
        val v0 = ((screenRect.top - previewRect.top) / previewRect.height()).coerceIn(0f, 1f)
        val u1 = ((screenRect.right - previewRect.left) / previewRect.width()).coerceIn(0f, 1f)
        val v1 = ((screenRect.bottom - previewRect.top) / previewRect.height()).coerceIn(0f, 1f)

        // 2) Map normalized coords to Mat pixels (Mat is UPRIGHT)
        val w = mat.cols()
        val h = mat.rows()

        val x0 = (u0 * w).toInt()
        val y0 = (v0 * h).toInt()
        val x1 = (u1 * w).toInt()
        val y1 = (v1 * h).toInt()

        // 3) Clamp & build OpenCV Rect (note: OpenCV Rect width/height are inclusive-exclusive)
        val left = minOf(x0, x1).coerceIn(0, w - 1)
        val top = minOf(y0, y1).coerceIn(0, h - 1)
        val right = maxOf(x0, x1).coerceIn(left + 1, w)
        val bottom = maxOf(y0, y1).coerceIn(top + 1, h)

        return Rect(left, top, right - left, bottom - top)
    }
}