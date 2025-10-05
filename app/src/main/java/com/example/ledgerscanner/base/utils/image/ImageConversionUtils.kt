package com.example.ledgerscanner.base.utils.image

import android.graphics.RectF
import androidx.camera.core.ImageProxy
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import kotlin.math.roundToInt

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
        overlayRect: RectF,
        previewRect: RectF,
        mat: Mat,
        swapXYIfNeeded: Boolean = false
    ): Rect {
        // defensive: avoid division by zero
        val dispW = previewRect.width().coerceAtLeast(1f)
        val dispH = previewRect.height().coerceAtLeast(1f)

        // 1) normalized coords relative to the displayed image area (allow values outside 0..1)
        val u0 = (overlayRect.left - previewRect.left) / dispW
        val v0 = (overlayRect.top - previewRect.top) / dispH
        val u1 = (overlayRect.right - previewRect.left) / dispW
        val v1 = (overlayRect.bottom - previewRect.top) / dispH

        // 2) map normalized coords to mat pixels (use floats until final rounding)
        val matW = mat.cols().toFloat()
        val matH = mat.rows().toFloat()

        // optionally swap axes if mat orientation differs from preview coordinate system
        val (x0f, y0f, x1f, y1f) = if (!swapXYIfNeeded) {
            arrayOf(u0 * matW, v0 * matH, u1 * matW, v1 * matH)
        } else {
            // Swap: map u->y and v->x (useful if mat was not rotated to upright)
            arrayOf(v0 * matH, u0 * matW, v1 * matH, u1 * matW)
        }

        // 3) round to nearest pixel
        val x0 = x0f.roundToInt()
        val y0 = y0f.roundToInt()
        val x1 = x1f.roundToInt()
        val y1 = y1f.roundToInt()

        // 4) clamp to mat bounds AFTER mapping (ensure width/height >= 1)
        val left = minOf(x0, x1).coerceIn(0, maxOf(0, mat.cols() - 1))
        val top = minOf(y0, y1).coerceIn(0, maxOf(0, mat.rows() - 1))
        val right = maxOf(x0, x1).coerceIn(left + 1, mat.cols())
        val bottom = maxOf(y0, y1).coerceIn(top + 1, mat.rows())

        return Rect(left, top, right - left, bottom - top)
    }
}